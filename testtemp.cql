CREATE TABLE sax.ranged_filtered_with_indexes (
  text_id text,
  uuid_id uuid,
  boolean_a boolean,
  boolean_b boolean,
  boolean_c boolean,
  time_stamp timestamp,
  PRIMARY KEY (text_id, uuid_id)
) WITH CLUSTERING ORDER BY (uuid_id ASC)
AND bloom_filter_fp_chance = 0.01
AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
AND comment = ''
AND compaction = {'min_threshold': '4', 'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32'}
AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
AND dclocal_read_repair_chance = 0.0
AND default_time_to_live = 0
AND gc_grace_seconds = 864000
AND max_index_interval = 2048
AND memtable_flush_period_in_ms = 0
AND min_index_interval = 128
AND read_repair_chance = 0.1
AND speculative_retry = '99.0PERCENTILE';



CREATE INDEX bolean_a_active ON sax.ranged_filtered_with_indexes (boolean_a);


CREATE INDEX bolean_b_active ON sax.ranged_filtered_with_indexes (boolean_b)
