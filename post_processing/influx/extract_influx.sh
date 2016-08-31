START_TIME="'$(gdate -d $1 -u --iso-8601=seconds)'"
STOP_TIME="'$(gdate -d $2 -u --iso-8601=seconds)'"

influx -database 'profanity' \
-execute "SELECT COUNT(id) AS count FROM profanity WHERE time >= $START_TIME AND time <= $STOP_TIME GROUP BY subject, word, time(1m) FILL(0)" \
-format csv \
-precision rfc3339 | awk -f process_influx.awk
