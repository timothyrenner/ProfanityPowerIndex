(ns profanity-power-index.site-generator
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.util :refer [escape-html]]
            [hiccup.element :refer [link-to]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defn- head [title]
  [:head
    [:meta {:charset "utf-8"}]
    [:title title]
    (include-css (str "https://maxcdn.bootstrapcdn.com/"
                      "bootstrap/3.3.4/css/bootstrap.min.css"))
    (include-css (str "https://maxcdn.bootstrapcdn.com/"
                      "bootstrap/3.3.4/css/bootstrap-theme.min.css"))])

(defn- row [id-base subj-name picture-link colors]
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

(defn- js-call [name & args]
  (str name "(" (str/join "," args) ")"))

(defn- strip-leading-dir [file-name]
  (->> (str/split file-name  #"/")
       ;; Take out first element (which is output-directory in this app).
       rest
       ;; Make the path great again.
       (str/join "/")))

(defn generate [options]

  (let [config-file (first (:arguments options))
        output-directory (get-in options [:options :output-directory])
        index-file (io/file output-directory "index.html")
        data-file 
          (io/file output-directory "data" (second (:arguments options)))
        js-file (io/file output-directory "js" "profanitypowerindex.js")
        config (-> config-file slurp json/read-json)
        subjects (:subjects config)
        start (:startTime config)
        stop  (:stopTime config)]

    (io/make-parents index-file)
    (io/make-parents data-file)
    (io/make-parents js-file)

    ;; Current bug: the site/ we're using to write this bad boy isn't the file
    ;; name we need to be injecting into the HTML.
    ;; Copy data.
    (io/copy (io/file (second (:arguments options))) data-file)

    ;; Copy js.
    (io/copy (-> "js/profanitypowerindex.js" io/resource io/file) js-file)

    ;; Write index.html out.
    (spit index-file
      (html5 {:lang "en"} 
        (head "Profanity Power Index")
          [:body 
            (include-js (str "https://ajax.googleapis.com/"
                             "ajax/libs/jquery/1.11.2/jquery.min.js"))
            (include-js (str "https://maxcdn.bootstrapcdn.com/"
                             "bootstrap/3.3.4/js/bootstrap.min.js"))
            (include-js "https://d3js.org/d3.v3.min.js")
            (include-js (strip-leading-dir (str js-file)))
          [:div.container
            [:div.row
              [:h1.text-center "Profanity Power Index"]]
            [:hr]
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
                              (str "\"" 
                                   (strip-leading-dir (str data-file)) 
                                   "\"")
                              (js-call "tsvCallback" 
                                          (json/write-str subjects) 
                                       (str "\"" start "\"")
                                       (str "\"" stop "\"")))]
            [:hr]
            [:div.row 
              [:p.text-center
                "Technical shit: Collected with "
                (link-to
                  "https://github.com/timothyrenner/turbine" "Turbine")
                " and "
                (link-to
                  "https://github.com/elastic/elasticsearch" "Elasticsearch. ")
                "Check out the code on " 
                (link-to 
                  "https://www.github.com/timothyrenner/ProfanityPowerIndex" 
                  "Github") "."]]]))))