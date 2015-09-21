(ns site-generator.core
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.util :refer [escape-html]]
            [hiccup.element :refer [link-to]]
            [clojure.string :as str]
            [cheshire.core :as json])
  (:gen-class))

(defn head [title]
  [:head
    [:meta {:charset "utf-8"}]
    [:title title]
    (include-css (str "https://maxcdn.bootstrapcdn.com/"
                      "bootstrap/3.3.4/css/bootstrap.min.css"))
    (include-css (str "https://maxcdn.bootstrapcdn.com/"
                      "bootstrap/3.3.4/css/bootstrap-theme.min.css"))
    (include-css "css/style.css")])

(defn row [id-base subj-name picture-link colors]
  (let [barchart-id  (str id-base "-barchart")
        image-id     (str id-base "-image")
        sparkline-id (str id-base "-sparkline")]
    [:div.row
      [:div.col-md-5 {:id barchart-id}]
      [:div.col-md-2 
        [:div.row {:id image-id}
            [:img {:src picture-link 
                   :style (str/join ";" ["width: 180px"
                                         "height: 180px"
                                         "border:2px solid black"
                                         "margin: 1em auto 0"])}]]
        [:div.row.text-center 
         [:h5 (escape-html subj-name)]]]
      [:div.col-md-5 {:id sparkline-id}]]))

(defn js-call [name & args]
  (str name "(" (str/join "," args) ")"))

(defn -main [& args]

 "Two arguments: the first is the name of a JSON file with the following 
  structure:

  `{
      \"subjects\": [{
        \"name\": \"Rand Paul\",
        \"display_name\": \"Rand \\\"Filibuster\\\" Paul\",
        \"picture\": \"www.wikipedia.org\", 
        \"id\": \"rand-paul\",
        \"colors\": {
          \"sparkline\": [{ \"offset\": \"xxx\", \"color\": \"xxx\"}, ... ],
          \"barchart\": { \"base\": \"xxx\", \"hover\": \"xxx\" }
        }
      },
      {
        \"name\": \"Ted Cruz\",
        ... and so forth
      }],
    \"startTime\":\"YYYY-MM-DDTHH:MM-ZZZZ\"
    \"stopTime\":\"YYYY-MM-DDTHH:MM-ZZZZ\"
  }`

   The second is a tab separated data file with the following columns, headers
   included: subject, word, time, count

   The time column is in ISO 8601 or some other format recognizable by
   `new Date(time)` in Javascript."

  (let [config-string (slurp (first args))
        config (json/parse-string config-string true)
        subjects (:subjects config)
        start (:startTime config)
        stop  (:stopTime config)]

    (println 
      (html5 {:lang "en"} 
        (head "Profanity Power Index")
          [:body 
            (include-js (str "https://ajax.googleapis.com/"
                             "ajax/libs/jquery/1.11.2/jquery.min.js"))
            (include-js (str "https://maxcdn.bootstrapcdn.com/"
                             "bootstrap/3.3.4/js/bootstrap.min.js"))
            (include-js "http://d3js.org/d3.v3.min.js")
            (include-js "js/profanitypowerindex.js")
          [:div.container
            [:div.row
              [:h1.text-center "Profanity Power Index"]
              [:h3.text-center "2016 Republican Primary Edition"]]
            [:hr]
            [:div.row
                [:h2.text-center "CNN Debate, 9/16/2015, 8-11 ET"]]
            [:div.row
                [:h5.text-center "Collected from the Twitter public timeline."]]
            [:hr]
            (interpose [:hr]
              (map (fn [c] (row (:id c) 
                                (:display_name c) 
                                (:picture c) 
                                (:colors c)))
                   subjects))]
            [:script (js-call "d3.tsv" 
                              (str "\"" (second args) "\"")
                              (js-call "tsvCallback" 
                                          (json/generate-string subjects) 
                                       (str "\"" start "\"")
                                       (str "\"" stop "\"")))]
            [:hr]
            [:div.row 
              [:p.text-center
                "Technical shit: Collected with "
                (link-to
                    "http://spark.apache.org" "Apache Spark")
                " with an Amazon EC2 cluster running "
                (link-to
                    "http://mesosphere.io" "Mesosphere's")
                " DCOS. "
                "Check out the code on " 
                (link-to 
                  "https://www.github.com/timothyrenner/ProfanityPowerIndex" 
                  "Github") "."]]
        ]))))
