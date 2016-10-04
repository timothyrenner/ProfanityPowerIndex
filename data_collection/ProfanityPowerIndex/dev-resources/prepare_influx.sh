# Resets influxDB by clearing data from previous runs.
influx -execute "DROP DATABASE profanity"
influx -execute "CREATE DATABASE profanity"
