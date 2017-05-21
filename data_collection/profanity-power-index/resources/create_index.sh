curl 'localhost:9200/profanity_power_index_20170520' \
    --request PUT \
    --header 'Content-Type: application/json' \
    --data @tweet_index.json