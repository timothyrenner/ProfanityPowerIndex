(ns profanity-power-index.core
  (:require [twitter.oauth :refer [make-oauth-creds]]
            [twitter.api.streaming :refer [statuses-filter]]
            [clojure.data.json :as json]
            [clojure.core.async :refer [<! >!! chan go-loop]]
            [clojure.string :as str]
            [turbine.core :refer [make-topology]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd])
  (:import [twitter.callbacks.protocols AsyncStreamingCallback])
  (:gen-class))

;; Partitioning transducer.
(defn partitioner-xform []
  (comp
    ;; Partition by whether it's a tweet terminator.
    (partition-by #(str/includes? % "\r\n"))
    ;; Merge the terminator with the rest of the tweet.
    (partition-all 2)
    ;; Flatten the nested sequence of chunks.
    (map flatten)
    ;; And concatenate into one string.
    (map #(apply str %))
    ;; And finally filter out empty strings.
    (filter #(not (str/blank? %)))))


;; (def transfer-chan (chan 5))
(def transfer-chan (chan 5))

;; Apparently _this_ is the best way to do streaming?
(def ^:dynamic *callback*
  (AsyncStreamingCallback.
    ;; resp is the HTTP response, baos is the stream itself.
    ;; This is the 'on-body' function for the callback.
    ;; Note that tweets can be chunked. This is a problem ... for turbine.
    ;; A partition-by transducer with (= % "\n") might do the trick.
    (fn [response baos] (>!! transfer-chan (.toString baos)))
    ;; This is the 'failure' function for the callback.
    (fn [resp] (binding [*out* *err*] (println "failed")))
    ;; This is the 'error' function for the callback. It has a response and
    ;; a throwable.
    (fn [err thr] (binding [*out* *err*] 
      (println "ERROR:" err) 
      (println (str thr))))))

;;;; PROCESSING HELPERS ;;;;
(defn contains-profanity? [text]
  (or (str/includes? text "fuck")
      (str/includes? text "shit")
      (str/includes? text "bitch")
      (str/includes? text "dick")
      (str/includes? text "douche")
      (str/includes? text " ass ")
      (str/includes? text "asshole")
      (str/includes? text "asshat")
      (str/includes? text "jackass")
      (str/includes? text "dumbass")))

(defn safe-parse-json [text]
  (try
    (json/read-json text)
    (catch Exception e 
    (do
      (println e)
      (println text)
      {:text ""}))))

(defn parse-filter []
  (comp
    (map safe-parse-json)
    (filter (fn [t] (-> t :text str/lower-case contains-profanity?)))))

(defn turbine-topology-vector [elastic-url]
  (let [es-conn (esr/connect elastic-url)]
    [[:in :input (partitioner-xform)]

    ;; JSON deserialization is probably the slowest part of this, and we can
    ;; filter in parallel too. Most of the data will be dropped by the filters,
    ;; but we need to deserialize to execute the filters.
    [:spread :input 
      [[:deser1 (parse-filter)]
      [:deser2 (parse-filter)]
      [:deser3 (parse-filter)]]]
    ;; Unify all of the parsed values asynchronously.
    [:union [:deser1 :deser2 :deser3]
             [:to-out (map identity)]]
    ;; Send to Elasticsearch.
    [:sink :to-out
      (fn [t] 
          (esd/put 
            es-conn 
            ;; Name of the index.
            "profanity_power_index_20170520" 
            ;; Type
            "tweet" 
            ;; Extract the id from the tweet.
            (:id_str t) 
            ;; Convert the document to JSON for elasticsearch indexing.
            t))]]))

(defn -main
  [& args]
  
  (let [creds (make-oauth-creds (System/getenv "CONSUMER_KEY")
                                (System/getenv "CONSUMER_SECRET")
                                (System/getenv "API_KEY")
                                (System/getenv "API_SECRET"))
        turbine-in (first 
                    (make-topology 
                      (turbine-topology-vector "http://localhost:9200")))] 
    
    ;; Read from the dechunker chan in a go loop and drop into turbine.
    (go-loop []
      (let [tweet-chunk (<! transfer-chan)]
        ; (println tweet-chunk)
        (turbine-in tweet-chunk))
      (recur))
    
    (statuses-filter :params {:track "trump"}
                     :oauth-creds creds
                     :callbacks *callback*)))
