(ns youtube-downloader.core
  (:require [compojure.core :refer [defroutes GET]]
            [ring.adapter.jetty :as ring]
            [ring.util.response :refer [response]]
            [youtube_downloader.download :as dl]
            [youtube-downloader.section-files :as sect]
            [clojure.data.json :as json]))

; TODO start client code in clojure, put in this repo?
; or keep using ReactJS?

(defn get-sections-handler
  [video-id]
  {:headers {"Content-type" "application/json"
             "Access-Control-Allow-Origin" "*"}
   :body    (json/write-str (dl/get-sections video-id))})

(defn download-files-zip
  []
  {:status 200
   :headers {"Content-Type" "application/zip, application/octet-stream"
             "Content-Disposition" (str "attachment; filename=\"files.zip\"")}
   :body (sect/zip-files ["C:\\Users\\james\\Downloads\\SPYDER550 - GODDD MODE.mp4"
                          "C:\\Users\\james\\Downloads\\ViNi $AN - FOR REAL.mp4"])})

(defroutes routes
   (GET "/sections/:v" [v] (get-sections-handler v))
   (GET "/download" [] (download-files-zip)))

(defn -main []
  (ring/run-jetty #'routes {:port 8080 :join? false}))