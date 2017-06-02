(ns profanity-power-index.core
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [profanity-power-index.data-collection :refer [collect]])
  (:gen-class))

(def collect-spec
  [[:short-opt "-t"
              :long-opt "--track"
              :required "TRACK"
              :desc "A tracking string for the Twitter API."
              :id :track
              :assoc-fn (fn [m k t] (update m k #(conj % t)))]])

(defn -main
  [& args]
  
  (let [env 
          (-> "env.edn" io/resource io/file slurp edn/read-string)
        command (first args)]

    (case command
      "collect" (let [options (parse-opts (rest args) collect-spec)]
                  (collect env options))
      (println (str command "is not a command.")))
))
