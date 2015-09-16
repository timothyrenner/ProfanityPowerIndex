-- Import the raw data.
DROP TABLE IF EXISTS raw_data;
CREATE TABLE raw_data (tweet_id TEXT,
                       retweet_id TEXT,
                       created_at TEXT,
                       subject TEXT,
                       word TEXT);

.separator \t
.import 20150916_cnn_debate.csv raw_data

-- Reduce with counts.
DROP TABLE IF EXISTS raw_counts;
CREATE TABLE raw_counts AS
SELECT subject, word, created_at as time, count(*) as count
FROM raw_data
GROUP BY subject, word, time;

-- Manually insert all of the word, candidate combinations for the collect
-- start time and collect end time.
DROP TABLE IF EXISTS counts;
CREATE TABLE counts (subject TEXT, 
                     word TEXT,
                     time TEXT,
                     count INTEGER);
   
.separator \t
.import 20150916_cnn_debate_extras.csv counts
   
-- Insert the (filtered) collected data into the table.
INSERT INTO counts 
SELECT * FROM raw_counts
WHERE time >= '2015-09-16T19:30-0400' AND
      time <= '2015-09-16T23:30-0400';
   
-- Finally, reapply the groupby and sort.
DROP TABLE IF EXISTS final_counts;
CREATE TABLE final_counts AS
SELECT subject, word, time, sum(count) as count
FROM counts
GROUP BY subject, word, time
ORDER BY time, subject, word;
   
-- Export.
.headers on
.output 20150916_cnn_debate_counts.csv

SELECT * FROM final_counts;