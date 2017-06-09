(ns profanity-power-index.core
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [profanity-power-index.data-collection :refer [collect]]
            [profanity-power-index.data-extraction :refer [extract]]
            [profanity-power-index.site-generator :refer [generate]])
  (:gen-class))

(def collect-spec
  [[:short-opt "-t"
    :long-opt "--track"
    :required "TRACK"
    :desc "A tracking string for the Twitter API."
    :id :track
    :assoc-fn (fn [m k t] (update m k #(conj % t)))]])

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

(def generate-spec
  [[:short-opt "-o"
   :long-opt "--output-directory"
   :required "DIRECTORY_NAME"
   :desc "The name of the output directory for the site."
   :id :output-directory
   :default "site"]])

(def main-spec
  [[:long-opt "--env"
    :required "ENV_FILE_NAME"
    :desc "The name of the environment file."
    :default "env.edn"
    :id :env]])

(defn -main
  [& args]
  
  (let [options (parse-opts args main-spec)
        env-file (get-in options [:options :env])
        env (-> env-file slurp edn/read-string)
        command (first args)]
    
    (case command
      "collect" (let [options (parse-opts (rest args) collect-spec)]
                  (collect env options))
      "extract" (let [options (parse-opts (rest args) extract-spec)]
                  (extract env options))
      "generate" (let [options (parse-opts (rest args) generate-spec)]
                    (generate options))
      (println (str command "is not a command.")))))
