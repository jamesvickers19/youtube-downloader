(ns youtube-downloader.core
  (:require [compojure.core :refer [defroutes GET]]
            [ring.adapter.jetty :as ring]
            [ring.util.response :refer [response]]
            [youtube_downloader.download :as dl]
            [clojure.data.json :as json]))

(defn get-sections-handler
  [video-id]
  {:headers {"Content-type" "application/json"
             "Access-Control-Allow-Origin" "*"}
   :body    (json/write-str (dl/get-sections video-id))})

(defroutes routes
   (GET "/sections/:v" [v] (get-sections-handler v)))

(defn -main []
  (ring/run-jetty #'routes {:port 8080 :join? false}))