(ns youtube-downloader.core
  (:require [compojure.core :refer [defroutes GET]]
            [ring.adapter.jetty :as ring]
            [ring.util.response :refer [response]]
            [youtube_downloader.download :as dl]
            [youtube-downloader.section-files :as sect]
            [clojure.data.json :as json]
            [ring.util.io :as ring-io]
            [clojure.java.io :as io])
  (:import (java.util.zip ZipOutputStream ZipEntry)))

; TODO start client code in clojure, put in this repo?
; or keep using ReactJS?

(defn zip-files
  "Returns an inputstream (piped-input-stream) to be used directly in Ring HTTP responses"
  [files]
  (ring-io/piped-input-stream
    (fn [output-stream]
      (with-open [zip-output-stream (ZipOutputStream. output-stream)]
        (doseq [file files]
          (let [f (io/file file)]
            (.putNextEntry zip-output-stream (ZipEntry. (.getName f)))
            (io/copy f zip-output-stream)
            (.closeEntry zip-output-stream)))))))

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
   :body (zip-files ["C:\\Users\\james\\Downloads\\SPYDER550 - GODDD MODE.mp4"
                     "C:\\Users\\james\\Downloads\\ViNi $AN - FOR REAL.mp4"])})

(defroutes routes
   (GET "/sections/:v" [v] (get-sections-handler v))
   ; TODO post with body params ?
   (GET "/download" [] (download-files-zip)))

(defn -main []
  (ring/run-jetty #'routes {:port 8080 :join? false}))