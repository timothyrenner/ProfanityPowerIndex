(ns site-generator.core
  (:require [hiccup.page :refer [html5 include-css include-js]]
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
                      "bootstrap/3.3.4/css/bootstrap-theme.min.css"))])

(defn row [id-base cand-name picture-link colors]
  (let [barchart-id  (str id-base "-barchart")
        image-id     (str id-base "-image")
        sparkline-id (str id-base "-sparkline")]
    [:div.row
      [:div.col-md-5 {:id barchart-id}]
      [:div.col-md-2 {:id image-id}
        [:img.img-responsive {:src picture-link 
                              :style (str/join ";" ["width: 180px"
                                                    "height: 229px"
                                                    "border:2px solid black"
                                                    "margin: 0 auto"])}]]
      [:div.col-md-5 {:id sparkline-id}]]))

(defn js-call [name & args]
  (str name "(" (str/join "," args) ")"))

(defn -main [& args]

 "Two arguments: the first is the name of a JSON file with the following 
  structure:

  `[{
     \"name\": \"Rand Paul\",
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
   }]`

   The second is a tab separated data file with the following columns, headers
   included: candidate, word, time, count

   The time column is in ISO 8601 or some other format recognizable by
   `new Date(time)` in Javascript."

  (let [candidates (json/parse-string (slurp (first args)) true)
        title "Fuck this Debate"]

    (println 
      (html5 {:lang "en"} 
        (head title)
          [:body 
            (include-js (str "https://ajax.googleapis.com/"
                             "ajax/libs/jquery/1.11.2/jquery.min.js"))
            (include-js (str "https://maxcdn.bootstrapcdn.com/"
                             "bootstrap/3.3.4/js/bootstrap.min.js"))
            (include-js "http://d3js.org/d3.v3.min.js")
            (include-js "js/setup.js")
          [:div.container
            [:div.row
              [:h1.text-center "Fuck This Debate"]]
            [:hr]
            [:div.row
              [:div.col-md-6
                [:h2.text-center "1,000,000,000 Tweets"]]
              [:div.col-md-6
                [:h2.text-center "Some other shit here."]]]
            [:hr]
            (interpose [:hr]
              (map (fn [c] (row (:id c) 
                                (:name c) 
                                (:picture c) 
                                (:colors c)))
                   candidates))]
            [:script (js-call "d3.tsv" 
                              (str "\"" (second args) "\"")
                              (js-call "tsvCallback" 
                                       (json/generate-string candidates)))]
        ]))))