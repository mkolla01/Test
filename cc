---
# Installs the desired version of cassandra.

- name: create cassandra group
  group:
    name: cassandra
    system: yes
    state: present
  tags: [ install_cassandra ]

- name: create cassandra user
  user:
    name: cassandra
    group: cassandra
    comment: 'Cassandra User'
    system: yes
    state: present
    createhome: no
  tags: [ install_cassandra ]

- name: decide whether Cassandra needs to be installed
  set_fact:
    cassandra_needed: "{{ ansible_local.bz_common.cassandra_version != cass_version }}"
  tags: [ install_cassandra ]

- name: stop cassandra for cassandra install
  service:
    name: cassandra
    state: stopped
  when: cassandra_needed and ansible_local.bz_common.was_cassandra_running
  tags: [ install_cassandra ]

- name: fetch and extract cassandra archive
  unarchive:
    src: "{{ repoURL }}/files/apache-cassandra-{{ cass_version }}-bin.tar.gz"
    dest: /opt/
    remote_src: yes
  when: cassandra_needed
  tags: [ install_cassandra ]

- name: symlink cassandra
  file:
    src: "/opt/apache-cassandra-{{ cass_version }}"
    dest: /opt/cassandra
    state: link
  when: cassandra_needed and not ansible_check_mode
  tags: [ install_cassandra ]

- name: make sure cassandra data directory exists
  file:
    dest: /opt/cassandra_data
    owner: cassandra
    group: cassandra
    state: directory
  tags: [ install_cassandra ]

- name: symlink cassandra data directory
  file:
    src: /opt/cassandra_data
    dest: /opt/cassandra/data
    state: link
  when: cassandra_needed and not ansible_check_mode
  tags: [ install_cassandra ]

- name: make sure commitlog archive directory exists
  file:
    dest: /opt/cassandra/commitlog_archive
    owner: cassandra
    group: cassandra
    state: directory
  tags: [ install_cassandra ]

- name: symlink cassandra bins
  file:
    src: "/opt/cassandra/bin/{{ item }}"
    dest: "/usr/local/bin/{{ item }}"
    state: link
  loop:
    - cqlsh
    - debug-cql
    - nodetool
    - sstableloader
    - sstablescrub
    - sstableupgrade
  when: cassandra_needed and not ansible_check_mode
  tags: [ install_cassandra ]

- name: symlink cassandra.in.sh
  file:
    src: /opt/cassandra/bin/cassandra.in.sh
    dest: /opt/cassandra/cassandra.in.sh
    state: link
  when: not ansible_check_mode
  tags: [ install_cassandra ]

- name: fix CASSANDRA_HOME for nodetool etc
  lineinfile:
    regexp: "^    CASSANDRA_HOME="
    line: "    CASSANDRA_HOME=/opt/cassandra/"
    path: /opt/cassandra/cassandra.in.sh
  tags: [ install_cassandra ]

- name: copy cassandra service pre-run check
  copy:
    src: cass-precheck.sh
    dest: /opt/cassandra/bin/cass-precheck.sh
    mode: '0755'
  tags: [ install_cassandra, configure_cassandra ]

- name: install cassandra systemd service
  copy:
    src: cassandra.service
    dest: /etc/systemd/system/cassandra.service
    owner: root
    group: root
  notify: systemd_daemon_reload
  tags: [ install_cassandra, configure_cassandra ]

- name: set ownership of cassandra directories
  file:
    dest: "{{ item }}"
    owner: cassandra
    group: cassandra
  loop:
    - "/opt/apache-cassandra-{{ cass_version }}"
    - /opt/cassandra_data
  tags: [ install_cassandra, configure_cassandra ]

- name: Install dependencies for Cassandra and backup scripts
  apt:
    name: [ libarchive-tools, pbzip2 ]
    state: present
    update_cache: yes
    cache_valid_time: "{{ apt_cache_valid_time }}"
  tags: [ install_cassandra, configure_cassandrabackup ]

- name: Install libjemalloc1 for Debian 9 and older.
  apt:
    name: [ libjemalloc1 ]
    state: present
    update_cache: yes
    cache_valid_time: "{{ apt_cache_valid_time }}"
  when: ansible_distribution == "Debian" and (ansible_distribution_major_version | int) < 10
  tags: [ install_cassandra, configure_cassandrabackup ]

- name: Install libjemalloc2 for Debian 10 and newer.
  apt:
    name: [ libjemalloc2 ]
    state: present
    update_cache: yes
    cache_valid_time: "{{ apt_cache_valid_time }}"
  tags: [ install_cassandra, configure_cassandrabackup ]
  when: ansible_distribution == "Debian" and (ansible_distribution_major_version | int) >= 10
