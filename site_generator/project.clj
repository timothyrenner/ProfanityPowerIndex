(defproject site_generator "0.0.1"
  :description "Creates the index page elements for the debate graphic."
  :url "http://timothyrenner.github.io"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [cheshire "5.4.0"]]
  :main ^:skip-aot site-generator.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
