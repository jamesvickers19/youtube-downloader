(ns youtube-downloader.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.adapter.jetty :as ring]
            [ring.util.response :refer [response]]
            [youtube_downloader.download :refer [download-audio get-sections]]
            [youtube-downloader.section-files :refer [section-file]]
            [clojure.data.json :as json]
            [ring.util.io :as ring-io]
            [clojure.java.io :as io]
            [clojure.tools.trace :refer :all])
  (:import (java.util.zip ZipOutputStream ZipEntry)))

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
   :body    (json/write-str (get-sections video-id))})

(defn download-files-zip
  [{{:keys [video-id sections]} :params}]
  (let [filename "C:\\Users\\james\\Downloads\\test" ; TODO make temporary dir
        file (download-audio video-id filename)
        downloaded-filename (.getAbsolutePath file)
        sections (section-file downloaded-filename sections)]
    {:status 200
     :headers {"Content-Type" "application/zip, application/octet-stream"
               "Content-Disposition" "attachment; filename=\"files.zip\""}
     :body (zip-files sections)}))

(defn json-handler [handler]
  (-> handler wrap-keyword-params wrap-json-params))

(defroutes routes
   (GET "/sections/:v" [v] (get-sections-handler v))
   (POST "/download" req ((json-handler download-files-zip) req)))

(defn -main []
  (ring/run-jetty #'routes {:port 8080 :join? false}))