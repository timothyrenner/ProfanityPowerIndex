(defproject profanity-power-index "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [turbine "0.1.0-SNAPSHOT"]
                 [twitter-api "1.8.0"]
                 [org.clojure/core.async "0.2.395"]
                 [clojurewerkz/elastisch "3.0.0-beta2"]
                 [org.clojure/tools.cli "0.3.5"]]
  :main ^:skip-aot profanity-power-index.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
