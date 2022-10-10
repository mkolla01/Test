---

- name: deploy cassandra configuration
  template:
    src: "{{ item.src }}"
    dest: "{{ item.dest }}"
    owner: root
    group: root
  loop:
    - { src: 'access.properties', dest: '/opt/cassandra/conf/access.properties'}
    - { src: 'cassandra-rackdc.properties', dest: '/opt/cassandra/conf/cassandra-rackdc.properties'}
    - { src: 'commitlog_archiving.properties', dest: '/opt/cassandra/conf/commitlog_archiving.properties'}
    - { src: 'logback.xml', dest: '/opt/cassandra/conf/logback.xml'}
  notify: restart cassandra
  tags: [ configure_cassandra ]

- name: deploy cassandra 3.x-specific config
  template:
    src: "{{ item.src }}"
    dest: "{{ item.dest }}"
    owner: root
    group: root
  loop:
   - { src: 'jvm.options', dest: '/opt/cassandra/conf/jvm.options'}
   - { src: 'cassandra.yaml', dest: '/opt/cassandra/conf/cassandra.yaml'}
   - { src: 'cassandra-env.sh', dest: '/opt/cassandra/conf/cassandra-env.sh'}
  when: "cass_version is version('4.0.0', '<')"
  notify: restart cassandra
  tags: [ configure_cassandra ]

- name: deploy cassandra 4.x-specific config
  template:
    src: "{{ item.src }}"
    dest: "{{ item.dest }}"
    owner: root
    group: root
  loop:
   - { src: 'jvm-server.options', dest: '/opt/cassandra/conf/jvm-server.options'}
   - { src: 'jvm8-server.options', dest: '/opt/cassandra/conf/jvm8-server.options'}
   - { src: 'cassandra4.yaml', dest: '/opt/cassandra/conf/cassandra.yaml'}
   - { src: 'cassandra4-env.sh', dest: '/opt/cassandra/conf/cassandra-env.sh'}
  when: "cass_version is version('4.0.0', '>=')"
  notify: restart cassandra
  tags: [ configure_cassandra ]

- name: Copy jmxremote password file
  template:
    src: jmxremote.password
    dest: /opt/cassandra/conf/jmxremote.password
    owner: cassandra
    group: cassandra
    mode: '0400'
  when: "'cassandra' in group_names or 'backblaze_dev' in group_names"
  tags: [ configure_cassandra ]

- name: Copy Cassandra backup scripts
  template:
    src: "{{ item }}"
    dest: "/usr/local/bin/{{ item }}"
    owner: root
    group: root
    mode: '0755'
  loop:
    - cassandra-dailysnapshot.sh
    - cassandra-incremental.sh
    - cassandra-commitlog.sh
    - cassandra-fakebackup.sh
  when: "'cassandra' in group_names or 'backblaze_dev' in group_names"
  tags: [ configure_cassandra, configure_cassandrabackup ]

- name: Create staging dirs for cassandra backups
  file:
    dest: "{{ item }}"
    state: directory
    owner: cassandra
    group: cassandra
  loop:
    - /opt/cassandrabackup/snapshots
    - /opt/cassandrabackup/incrementals
  tags: [ install_cassandra, configure_cassandra, configure_cassandrabackup ]

- name: Copy public key for encrypting backups
  copy:
    src: backuppub.pem
    dest: /opt/cassandrabackup/backuppub.pem
    owner: root
    group: root
  when: "'cassandra' in group_names or 'backblaze_dev' in group_names"
  tags: [ configure_cassandra, configure_cassandrabackup ]

- name: Create gpg dir for Cassandra backups
  file:
    dest: /root/.gnupg
    state: directory
    owner: root
    mode: '0700'
  tags: [ install_cassandra, configure_cassandra, configure_cassandrabackup ]

- name: Copy public gpg key for Cassandra backups
  copy:
    src: "{{ item }}"
    dest: "/root/.gnupg/{{ item }}"
    owner: root
    group: root
    mode: '0700'
  loop:
    - pubring.gpg
    - pubring.kbx
  tags: [ install_cassandra, configure_cassandra, configure_cassandrabackup ]

- name: Cassandra full snapshot cron job
  cron:
    name: "Cassandra daily snapshot"
    cron_file: cassandra_backup
    minute: "13"
    hour: "4"
    weekday: "{{ cass_full_backup_weekdays|default('*') }}"
    user: root
    job: "nice /usr/local/bin/cassandra-dailysnapshot.sh  > /tmp/latest_cass_backup.log 2>&1"
  when: "'cassandra' in group_names or 'backblaze_dev' in group_names"
  tags: [ configure_cassandra, configure_cassandrabackup ]

- name: Casssandra incremental backup cron job
  cron:
    name: "Cassandra incremental backup"
    cron_file: cassandra_backup
    minute: "{{ cass_inc_backup_minute|default(10) }}"
    hour: "*"
    weekday: "{{ cass_inc_backup_weekdays|default('*') }}"
    user: root
    job: "nice /usr/bin/flock -n /tmp/cassandra-incremental.lock /usr/local/bin/cassandra-incremental.sh > /tmp/latest_cass_inc_backup.log 2>&1"
  when: "'cassandra' in group_names"
  tags: [ configure_cassandra, configure_cassandrabackup ]


- name: Casssandra commitlog backup cron job
  cron:
    name: "Cassandra commitlog backup"
    cron_file: cassandra_backup
    minute: "*/5"
    hour: "*"
    weekday: "*"
    user: root
    job: "nice /usr/bin/flock -n /tmp/cassandra-commitlog.lock /usr/local/bin/cassandra-commitlog.sh > /tmp/latest_cass_commitlog_backup.log 2>&1"
  when: "'cassandra' in group_names"
  tags: [ configure_cassandra, configure_cassandrabackup ]

- name: Cassandra backup monitoring cron job
  cron:
    name: "Cassandra backup monitoring"
    cron_file: cassandra_backup
    minute: "2"
    hour: "4"
    user: root
    job: "/usr/local/bin/cassandra_backup_monitor.py"
  when: "'cassandra' in group_names or 'backblaze_dev' in group_names"
  tags: [ configure_cassandra, configure_cassandrabackup ]

- name: Copy monitoring script for s3 backups
  template:
    src: cassandra_backup_monitor.py
    dest: /usr/local/bin/cassandra_backup_monitor.py
    owner: root
    group: root
    mode: '0755'
  when: "'cassandra' in group_names or 'backblaze_dev' in group_names"
  tags: [ configure_cassandra, configure_cassandrabackup ]

- name: create directory for schema files
  file:
    dest: /opt/cassandra/schema/
    state: directory
    owner: cassandra
    group: cassandra
    mode: '0755'
  tags: [ configure_cassandra, configure_cassandra_schemafiles ]

- name: copy per-cluster schema to host
  template:
    src: "{{ item }}"
    dest: "/opt/cassandra/schema/{{ item}}"
    owner: root
    group: cassandra
    mode: '640'
  loop:
    - prod_cluster_keyspaces.cql
    - 01_b2_ddl.cql
    - 02_auth_ddl.cql
    - 03_cluster_users.cql
    - 07_usage_ddl.cql
    - 08_vault_stats_ddl.cql
    - 10_s3_object_lock_ddl.cql
    - 11_b2_sse_ddl.cql
    - 12_b2_data_deletion_ddl.cql
    - 14_b2_file_by_id_ddl.cql
    - 15_tiny_file_comparison_ddl.cql
    - 16_bucket_replication_schema_ddl.cql
    - 17_file_replication_schema_ddl.cql
    - 18_dmca_takedown_schema_ddl.cql
    - prod_system_auth_keyspaces.cql
  when: "'global' not in cass_cluster"
  tags: [ configure_cassandra, configure_cassandra_schemafiles ]

- name: copy global schema to host
  template:
    src: "{{ item }}"
    dest: "/opt/cassandra/schema/{{ item}}"
    owner: root
    group: cassandra
    mode: '640'
  loop:
    - prod_global_keyspaces.cql
    - 04_cross_groups_stats_ddl.cql
    - 05_groups_ddl.cql
    - 13_bzadmin_ddl.cql
    - prod_system_auth_keyspaces.cql
  when: "'global' in cass_cluster"
  tags: [ configure_cassandra, configure_cassandra_schemafiles ]

#
# Backblaze directories.
#

- name: make sure log directories exists
  file:
    dest: "/bzsite/commonlogs/{{ item }}"
    state: directory
    owner: cassandra
    group: cassandra
    mode: '0755'
  loop:
    - cassandra_health_check
    - cassandra_to_prometheus
  tags: [ install_cassandra, configure_cassandra ]

#
# 'nodetool repair' needs to run regularly
#

- name: figure which day to run the nodetool repair on
  set_nodetool_repair_hour:
    hostname: "{{ ansible_hostname }}"
  when: cassandraRepair != 'none'
  tags: [ configure_cassandra, skip_ansible_lint ]

- name: install script to run repair
  template:
    src: bz_cassandra_repair
    dest: /usr/local/bin/bz_cassandra_repair
    owner: root
    group: root
    mode: '0755'
  when: cassandraRepair != 'none'
  tags: [ configure_cassandra ]

- name: install program to push metrics to prometheus (Cassandra 3)
  template:
    src: cassandra3_to_prometheus.py
    dest: /usr/local/bin/cassandra_to_prometheus
    owner: root
    group: root
    mode: '0755'
  when: "cass_version is version('4.0.0', '<')"
  tags: [ configure_cassandra, configure_cass_to_prom ]

- name: install program to push metrics to prometheus (Cassandra 4)
  template:
    src: cassandra4_to_prometheus.py
    dest: /usr/local/bin/cassandra_to_prometheus
    owner: root
    group: root
    mode: '0755'
  when: "cass_version is version('4.0.0', '>=')"
  tags: [ configure_cassandra, configure_cass_to_prom ]

- name: 'cron job for nodetool repair: NO REPAIR'
  cron:
    name: nodetool-repair
    cron_file: nodetool-repair
    state: absent
    user: cassandra
  when: cassandraRepair == 'none'
  tags: [ configure_cassandra ]

- name: make sure cassandraRepair is set properly
  fail:
    msg: "cassandraRepair is {{ cassandraRepair }}, but should be: incremental, or off"
  when: cassandraRepair not in ['incremental', 'none']
  tags: [ configure_cassandra ]

- name: 'cron job for nodetool repair: Incremental Repair'
  cron:
    name: nodetool-repair
    cron_file: nodetool-repair
    state: present
    hour: "{{ nodetool_repair_hour }}"
    minute: "23"
    user: cassandra
    job: "/usr/local/bin/bz_cassandra_repair"
  when: cassandraRepair == 'incremental'
  tags: [ configure_cassandra ]

- name: cron job to mark b2.upload repaired then compact it
  cron:
    name: nodetool-repair-b2upload
    cron_file: nodetool-repair-b2upload
    state: present
    hour: "14"
    minute: "{{ b2upload_repair_minute }}"
    user: cassandra
    job: "/usr/local/bin/bznodetool repair b2 upload && /usr/local/bin/bznodetool compact b2 upload"
  when: b2upload_repair_minute is defined
  tags: [ configure_cassandra ]

- name: "cron job for cassandra_to_prometheus"
  cron:
    name: cassandra_to_prometheus
    cron_file: cassandra_to_prometheus
    state: present
    user: cassandra
    job: "/usr/bin/nice /usr/local/bin/cassandra_to_prometheus {{ prometheus_gateway_host_port }} > /dev/null 2>&1"
  tags: [ configure_cassandra ]

# For some reason, Cassandra seems to disable auto-compaction on
# its own.  We don't want it to do that.
- name: cron job to keep auto-compaction enabled
  cron:
    name: enable_auto_compaction
    cron_file: nodetool-auto-compaction
    state: present
    minute: "5,15,25,35,45,55"
    user: cassandra
    job: "/usr/local/bin/bznodetool enableautocompaction"
  tags: [ configure_cassandra ]

- import_tasks: ../../jdk/tasks/main.yml
  tags: [ install_jdk ]

- name: enable cassandra systemd service
  service:
    name: cassandra.service
    state: started
    enabled: true
  when: not ansible_local.bz_common.in_container
  tags: [ install_cassandra, configure_cassandra ]

- name: restart cassandra if Java upgraded
  command: 'true'
  when: jdk8_needed
  notify: restart cassandra
  tags: [ install_jdk ]

# Do any restart that needs to happen
- name: run handlers
  meta: flush_handlers
  tags: always

- name: wait_for_cassandra
  bz_wait_for_cassandra: {}
  when: not ansible_local.bz_common.in_container
  tags: [ install_cassandra, configure_cassandra, install_jdk, skip_ansible_lint ]
