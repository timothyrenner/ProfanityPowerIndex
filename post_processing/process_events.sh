#!/bin/bash

real_data=$1
jump_data=$2

# Read the start and stop times from the jumpstarted data.
start_time=$(cat $jump_data | head -n 1 | csvcut -tc 3)
stop_time=$(cat $jump_data | tail -n 1 | csvcut -tc 3)

sqlite3 ppi.db <<EOF
-- Import the raw data.
DROP TABLE IF EXISTS raw_data;
CREATE TABLE raw_data (tweet_id TEXT,
                       retweet_id TEXT,
                       created_at TEXT,
                       subject TEXT,
                       word TEXT);
.separator \t
.import ${real_data} raw_data

-- Reduce with counts.
DROP TABLE IF EXISTS raw_counts;
CREATE TABLE raw_counts AS
SELECT subject, word, created_at AS time, COUNT(*) AS COUNT
FROM raw_data
GROUP BY subject, word, time;

-- Insert the jumpstart data.
DROP TABLE IF EXISTS counts;
CREATE TABLE counts (subject TEXT,
                     word TEXT,
                     time TEXT,
                     count INTEGER);
.separator \t
.import ${jump_data} counts

-- Insert the collected data into the count table.
INSERT INTO counts
SELECT * FROM raw_counts
WHERE time >= '${start_time}' AND
      time <= '${stop_time}';

-- Finally, reapply the reduction and sort.
DROP TABLE IF EXISTS final_counts;
CREATE TABLE final_counts AS
SELECT subject, word, time, SUM(count) AS count
FROM counts
GROUP BY subject, word, time
ORDER BY time, subject, word;

-- Export.
.headers on

SELECT * FROM final_counts;
EOF