(ns youtube-downloader.core
  (:require [compojure.core :refer [defroutes GET POST OPTIONS]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.adapter.jetty :as ring]
            [ring.util.response :refer [response]]
            [youtube_downloader.download :refer [download-audio get-sections get-video video-title]]
            [youtube-downloader.section-files :refer [clean-filename section-file]]
            [clojure.data.json :as json]
            [ring.util.io :as ring-io]
            [clojure.java.io :as io]
            [clojure.tools.trace :refer :all])
  (:import (java.util.zip ZipOutputStream ZipEntry)
           (java.io File)))

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

; TODO use middleware or something
(def allow-all-origin-header
  {"Access-Control-Allow-Origin" "*"
   "Access-Control-Allow-Headers" "*"
   "Access-Control-Allow-Methods" "GET, POST, OPTIONS"})

; TODO make temporary dir
(def base-dir "C:\\Users\\james\\Downloads\\test\\")

(defn title-handler
  [video-id]
  {:headers (merge allow-all-origin-header {"Content-type" "application/text"})
   :body (video-title (get-video video-id))})

(defn sections-handler
  [video-id]
  {:headers (merge allow-all-origin-header {"Content-type" "application/json"})
   :body    (json/write-str (get-sections video-id))})

(defn download-video-handler
  [video-id]
  {:headers (merge allow-all-origin-header
                   {"Content-Type" "application/octet-stream; charset=utf-8"})
   :body (download-audio video-id base-dir)})

(defn download-handler
  [{{:keys [video-id sections]} :params}]
  (let [file (download-audio video-id base-dir)
        sections (section-file (.getAbsolutePath file) sections)]
    {:status 200
     :headers (merge allow-all-origin-header
                     {"Content-Type" "application/octet-stream; charset=utf-8"})
     :body (zip-files sections)}))

(defn json-handler [handler]
  (-> handler wrap-keyword-params wrap-json-params))

(defroutes routes
  (GET "/title/:v" [v] (title-handler v))
  (GET "/sections/:v" [v] (sections-handler v))
  (GET "/download/:v" [v] (download-video-handler v))
  (POST "/download" req ((json-handler download-handler) req))
  (OPTIONS "/download" req {:status 200 :headers allow-all-origin-header}))

(defn -main []
  (ring/run-jetty #'routes {:port 8080 :join? false}))