curl 'localhost:9200/profanity_power_index' \
    --request PUT \
    --header 'Content-Type: application/json' \
    --data @tweet_index.json