# Resets influxDB by clearing data from previous runs.
influx -execute "DROP DATABASE IF EXISTS profanity"
influx -execute "CREATE DATABASE profanity"
