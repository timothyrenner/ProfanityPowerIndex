(ns profanity-power-index.core
  (:require [twitter.oauth :refer [make-oauth-creds]]
            [twitter.api.streaming :refer [statuses-filter]]
            [clojure.data.json :as json]
            [clojure.core.async :refer [<! >!! chan go-loop]]
            [clojure.string :as str]
            [turbine.core :refer [make-topology]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [twitter.callbacks.protocols AsyncStreamingCallback])
  (:gen-class))

;; Partitioning transducer.
(defn partitioner-xform []
  (comp
    ;; Split on the carriage return. These occur only at the end of a tweet.
    (mapcat (fn [s] (str/split s #"\r")))
    ;; Now partition the incoming vectors by a blank string, which works because
    ;; the tweet boundary is \r\n, so \n returns true for str/blank?
    ;; This groups the tweet chunks into vectors, and the boundaries into
    ;; empty strings.
    (partition-by str/blank?)
    ;; Mash everything together into a string.
    (map (fn [s] (apply str s)))
    ;; Finally, filter out the newline boundaries.
    (filter (fn [s] (not (str/blank? s))))))

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
      (println "ERROR:" err)))))

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
      {:text ""}))))

(defn parse-filter []
  (comp
    (map safe-parse-json)
    (filter 
      (fn [t] 
        (-> t
            ;; Pull the text out, or an empty string. 
            (:text "")
            ;; Convert to lower case.
            str/lower-case 
            ;; Check for profanity.
            contains-profanity?)))))

(defn turbine-topology-vector [elastic-url elastic-index]
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
            elastic-index
            ;; Type
            "tweet" 
            ;; Extract the id from the tweet.
            (:id_str t) 
            ;; The tweet itself, which doesn't need to be converted to JSON
            ;; explicitly.
            t))]]))

(defn -main
  [& args]
  
  (let [env 
          (-> "env.edn" io/resource io/file slurp edn/read-string)
        options 
          (parse-opts
            args
            [[:short-opt "-t"
              :long-opt "--track"
              :required "TRACK"
              :desc "A tracking string for the Twitter API."
              :id :track
              :assoc-fn (fn [m k t] (update m k #(conj % t)))]])

        creds (make-oauth-creds (:twitter-consumer-key env)
                                (:twitter-consumer-secret env)
                                (:twitter-api-key env)
                                (:twitter-api-secret env))
        turbine-in (first 
                    (make-topology 
                      (turbine-topology-vector 
                        (:elasticsearch-url env "http://localhost:9200")
                        (:elasticsearch-index env "profanity_power_index"))))] 

    ;; Read from the dechunker chan in a go loop and drop into turbine.
    (go-loop []
      (let [tweet-chunk (<! transfer-chan)]
        (turbine-in tweet-chunk))
      (recur))
    
    (statuses-filter :params {:track (get-in options [:options :track])}
                     :oauth-creds creds
                     :callbacks *callback*)))
