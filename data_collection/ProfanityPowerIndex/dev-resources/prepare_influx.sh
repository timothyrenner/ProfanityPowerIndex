# Resets influxDB by clearing data from previous runs.
influx -execute "DROP DATABASE IF EXISTS profanity"
influx -execute "CREATE DATABASE profanity"
# This may need to go in the code.
influx -execute 'CREATE CONTINUOUS QUERY profanity_by_minute ON profanity BEGIN SELECT COUNT(id) AS cnt INTO profanity_agg FROM profanity."default".profanity WHERE time >= now() - 1d and time <= now() GROUP BY subject, word, time(1m) fill(0) END'
