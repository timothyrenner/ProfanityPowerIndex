(ns profanity-power-index.data-extraction
    (:require [clojure.data.json :as json]
              [clojurewerkz.elastisch.rest :as esr]
              [clojurewerkz.elastisch.rest.document :as esd]
              [clojure.java.io :as io]
              [clojure.data.csv :as csv]))

;; These two aggregations are fixed.
(def tweets-per-minute
    {
        :tweets_per_minute {
            :date_histogram {
                :field "created_at"
                :interval "minute"
                :format "date_time_no_millis"
                :time_zone "-05:00"
            }
        }
    }
)

(def profanity
    {
        :profanity {
            :filters {
                :filters {
                    :fuck   {:match {:text "fuck*"}}
                    :shit   {:match {:text "shit*"}}
                    :bitch  {:match {:text "bitch*"}}
                    :ass    {:match {:text "ass*"}}
                    :dick   {:match {:text "dick*"}}
                    :douche {:match {:text "douche*"}}
                    :covfefe {:match {:text "covfefe"}}
                }
            }
        }
    }
)

;; The time query is a function of start and end.
(defn- time-query [start end]
    {
        :range {
            :created_at {
                :gte start
                :lte end
                :format "date_time_no_millis"
            }
        }
    }
)

 ;; The target aggregation is a function of the matches and target names.
 ;; TODO: Look at quoted text as well.
 (defn- target [targets]
   {
        :target {
            :filters {
                :filters
                    ;; Use the map transducer to build the nested map for the 
                    ;; query string. 
                    (into {} (map (fn [[q t]] [t {:match {:text q}}])) targets)
            }
        }
   }
 )

(defn- make-query [start end targets]
    (-> {
            :size 0
            :search_type "query_then_fetch"
            :scroll "1m"
        }
        (assoc :query (time-query start end))
        (assoc :aggregations tweets-per-minute)
        (assoc-in [:aggregations :tweets_per_minute 
                   :aggregations] profanity)
        (assoc-in [:aggregations :tweets_per_minute
                   :aggregations :profanity
                   :aggregations] (target targets))))

(defn extract [env options]
    (let [start (get-in options [:options :start])
          end (get-in options [:options :end])
          output (get-in options [:options :output])
          ;; Because our :target option performs a conj on a list, items are 
          ;; appended to the front, but the arguments are appended to the back.
          ;; So we have to reverse the arguments to match the options.
          ;; Yes I'm aware mashing the arguments with the options is pretty 
          ;; janky, but it's not straightforward to make an option take multiple
          ;; requirements.
          target-filters (map vector (get-in options [:options :target])
                                     (reverse (:arguments options)))
         ;; Build the query with the options.
         es-query (make-query start end target-filters)
         ;; Extract the elasticsearch parameters.
         elastic-url (:elasticsearch-url env "http://localhost:9200")
         elastic-index (:elasticsearch-index env "profanity_power_index")
         ;; Create the elasticsearch connection.
         es-conn (esr/connect elastic-url)
         es-res (esd/search es-conn elastic-index "tweet" es-query)]
    
    (with-open [writer (io/writer "test.csv")]
        (csv/write-csv writer 
                       [["time" "word" "subject" "count"]]
                       :separator \tab)
        (doseq
            [minute-bucket 
                (get-in es-res [:aggregations :tweets_per_minute :buckets]) 
             profanity-bucket 
                (into [] (get-in minute-bucket [:profanity :buckets])) 
             target-bucket 
                (into [] (get-in (second profanity-bucket) [:target :buckets]))]
            (csv/write-csv writer
                [[(:key_as_string minute-bucket) 
                 (name (first profanity-bucket))
                 (name (first target-bucket))
                 (:doc_count (second target-bucket))]]
                :separator \tab)))))