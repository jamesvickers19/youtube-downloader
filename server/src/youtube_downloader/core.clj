(ns youtube-downloader.core
  (:require [compojure.core :refer [defroutes GET POST OPTIONS]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.adapter.jetty :as ring]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [youtube_downloader.download :refer [download get-sections]]
            [youtube-downloader.section-videos :refer [section-video]]
            [clojure.data.json :as json]
            [ring.util.io :as ring-io]
            [clojure.java.io :as io]
            [clojure.tools.trace :refer :all])
  (:import (java.util.zip ZipOutputStream ZipEntry Deflater))
  (:gen-class))

(defn zip-and-delete-files
  "Returns an inputstream (piped-input-stream) to be used directly in Ring HTTP responses"
  [filenames]
  (ring-io/piped-input-stream
    (fn [output-stream]
      (with-open [zip-output-stream (ZipOutputStream. output-stream)]
        (.setLevel zip-output-stream Deflater/BEST_SPEED)
        (doseq [f filenames]
          (.putNextEntry zip-output-stream (ZipEntry. ^String f))
          (io/copy (io/file f) zip-output-stream)
          (.closeEntry zip-output-stream)
          (io/delete-file f))))))

(defn sections-handler
  [video-id]
  {:headers {"Content-type" "application/json"}
   :body    (json/write-str (get-sections video-id))})

(defn download-handler
  [{{:keys [video-id sections include-video]} :params}]
  (let [content (download video-id include-video)
        body (if sections
               (zip-and-delete-files (map :filename (section-video content sections)))
               content)]
    {:status 200
     :headers {"Content-Type" "application/octet-stream; charset=utf-8"}
     :body body}))
    ; TODO include filename in response, use in client


(defn json-handler [handler]
  (-> handler wrap-keyword-params wrap-json-params))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (println "Error in handler: " handler "; " (.getMessage e))
           {:status 500
            :body "error"
            :headers {"Content-Type" "application/text"}}))))

(defroutes routes
  (GET "/" [] (resource-response "public/index.html"))
  (GET "/sections/:v" [v] ((wrap-exception sections-handler) v))
  (POST "/download" req ((-> download-handler json-handler wrap-exception) req))
  (OPTIONS "/download" req {:status 200}))

(def app
  (-> routes
      (wrap-resource "public"))) ;; files from resources/public are served

(defn -main [& args]
  ; TODO check that ffmpeg is available?
  (let [port (if (seq args)
               (Integer/parseInt (first args))
               8080)]
    (ring/run-jetty app {:port port :join? false})))