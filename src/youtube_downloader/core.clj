(ns youtube-downloader.core
  (:require [compojure.core :refer [defroutes GET POST OPTIONS]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.adapter.jetty :as ring]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [youtube_downloader.download :refer [download-audio-bytes get-sections]]
            [youtube-downloader.section-videos :refer [section-video]]
            [clojure.data.json :as json]
            [ring.util.io :as ring-io]
            [clojure.java.io :as io]
            [clojure.tools.trace :refer :all])
  (:import (java.util.zip ZipOutputStream ZipEntry Deflater))
  (:gen-class))

(defmacro time
  {:added "1.0"}
  [label expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (prn (str "Elapsed time " ~label ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " ms"))
     ret#))

(defn zip-files
  "Returns an inputstream (piped-input-stream) to be used directly in Ring HTTP responses"
  [sections] ; TODO destructure?
  (ring-io/piped-input-stream
    (fn [output-stream]
      (with-open [zip-output-stream (ZipOutputStream. output-stream)]
        (.setLevel zip-output-stream Deflater/BEST_SPEED)
        (doseq [section sections]
          (let [^String name (:name section)
                bytes (get-in section [:result :out])]
            (.putNextEntry zip-output-stream (ZipEntry. (str name ".mp3")))
            (io/copy bytes zip-output-stream)
            (.closeEntry zip-output-stream)))))))

(defn sections-handler
  [video-id]
  {:headers {"Content-type" "application/json"
             "Access-Control-Allow-Origin" "*"}
   :body    (json/write-str (get-sections video-id))})

(defn download-video-handler
  [video-id]
  {:headers {"Content-Type" "application/octet-stream; charset=utf-8"
             "Access-Control-Allow-Origin" "*"}
   :body (time "download-audio (full)" (download-audio-bytes video-id))})

(defn download-handler
  [{{:keys [video-id sections]} :params}]
  (let [audio-bytes (time "download-audio" (download-audio-bytes video-id))
        sections (time "section-video" (doall (section-video audio-bytes sections)))
        failures (filter #(not= 0 (get-in % [:result :exit])) sections)
        error-messages (vec (map #(get-in % [:result :err]) failures))
        body (time "zip-files" (do (zip-files sections)))]
    (if (seq failures)
      {:status 500
       :headers {"Content-Type" "application/text"}
       :body "Failed to download sections"}
      ;:body (str error-messages)} ; TODO log error messages
      {:status 200
       :headers {"Content-Type" "application/octet-stream; charset=utf-8"}
       :body body})))

(defn json-handler [handler]
  (-> handler wrap-keyword-params wrap-json-params))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           {:status 500
            :body (.getMessage e)
            :headers {"Content-Type" "application/text"}}))))

(defroutes routes
  (GET "/" [] (resource-response "public/index.html"))
  (GET "/sections/:v" [v] ((wrap-exception sections-handler) v))
  (GET "/download/:v" [v] ((wrap-exception download-video-handler) v))
  (POST "/download" req ((-> download-handler json-handler wrap-exception) req))
  (OPTIONS "/download" req {:status 200}))

(def app
  (-> routes
      (wrap-resource "public"))) ;; files from resources/public are served

(defn -main [& args]
  (let [port (if (seq args)
               (Integer/parseInt (first args))
               8080)]
    (ring/run-jetty app {:port port :join? false})))