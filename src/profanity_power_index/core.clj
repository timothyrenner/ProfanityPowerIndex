(ns profanity-power-index.core
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [profanity-power-index.data-collection :refer [collect]]
            [profanity-power-index.data-extraction :refer [extract]])
  (:gen-class))

(def collect-spec
  [[:short-opt "-t"
    :long-opt "--track"
    :required "TRACK"
    :desc "A tracking string for the Twitter API."
    :id :track
    :assoc-fn (fn [m k t] (update m k #(conj % t)))]])

;; TODO: Figure out how best to specify the mapping between the filter
;; query for elasticsearch and the target name. Could be paired args as 
;; command line arguments (if supported) or a JSON file.
;; It doesn't look like multiple arguments are supported directly. However, the
;; tracking queries end up in :options, and the target names end up in 
;; :arguments. As long as they come in pairs, they can be zipped together and
;; used. This is much better than a JSON file.
(def extract-spec
  [[:short-opt "-s"
    :long-opt "--start"
    :required "START_TIME"
    :desc "The start time for the extraction."
    :id :start]
   [:short-opt "-e"
    :long-opt "--end"
    :required "END_TIME"
    :desc "The end time for the extraction."
    :id :end]
   [:short-opt "-t"
    :long-opt "--target"
    :required ["QUERY" "TARGET"]
    :desc "The query for target extraction and the target name."
    :id :target
    :assoc-fn (fn [m k t] (update m k #(conj % t)))]
   [:short-opt "-o"
    :long-opt "--output"
    :required "OUTPUT_FILE"
    :desc "The name of the file to store the output."
    :id :output]])

(defn -main
  [& args]
  
  (let [env 
          (-> "env.edn" io/resource io/file slurp edn/read-string)
        command (first args)]

    (case command
      "collect" (let [options (parse-opts (rest args) collect-spec)]
                  (collect env options))
      "extract" (let [options (parse-opts (rest args) extract-spec)]
                  (extract env options))
      (println (str command "is not a command.")))
))
