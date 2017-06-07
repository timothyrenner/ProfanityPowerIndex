(defproject profanity-power-index "2.0"
  :description "Command line tool for the Profanity Power Index."
  :url "https://github.com/timothyrenner/profanitypowerindex"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [turbine "0.1.0-SNAPSHOT"]
                 [twitter-api "1.8.0"]
                 [org.clojure/core.async "0.2.395"]
                 [clojurewerkz/elastisch "3.0.0-beta2"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.csv "0.1.4"]
                 [hiccup "1.0.5"]]
  :main ^:skip-aot profanity-power-index.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
