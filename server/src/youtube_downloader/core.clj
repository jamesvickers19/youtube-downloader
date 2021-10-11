(ns youtube-downloader.core
  (:require [compojure.core :refer [defroutes GET POST OPTIONS]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.adapter.jetty :as ring]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [youtube_downloader.download :refer [download get-meta]]
            [youtube-downloader.section-videos :refer [section-video]]
            [clojure.data.json :as json]
            [header-utils.content-disposition :refer [encode]]
            [ring.util.io :as ring-io]
            [clojure.java.io :as io]
            [clojure.tools.trace :refer :all])
  (:import (java.util.zip ZipOutputStream ZipEntry Deflater))
  (:gen-class))

(defn zip-sections
  "Returns an inputstream (piped-input-stream) to be used directly in Ring HTTP responses"
  [sections]
  (ring-io/piped-input-stream
    (fn [output-stream]
      (with-open [zip-output-stream (ZipOutputStream. output-stream)]
        (.setLevel zip-output-stream Deflater/BEST_SPEED)
        (doseq [section sections]
          (.putNextEntry zip-output-stream (ZipEntry. (str (:name section) ".mp4")))
          (io/copy (:data section) zip-output-stream)
          (.closeEntry zip-output-stream))))))

(defn meta-handler
  [video-id]
  {:headers {"Content-type" "application/json"}
   :body    (json/write-str (get-meta video-id))})

(defn download-handler
  [{{:keys [filename video-id sections include-video]} :params}]
  (let [content (download video-id include-video)
        section-count (count sections)
        multiple (> section-count 1)
        body (if (nil? sections)
               content
               (let [sectioned (section-video content sections)]
                 (if multiple
                   (zip-sections sectioned)
                   (:data (first sectioned)))))
        [content-type extension] (if multiple ["application/zip" ".zip"]
                                              ["video/mp4" ".mp4"])]
    {:status 200
     :headers {"Content-Type" content-type
               "Content-Disposition" (encode "attachment" (str filename extension))}
     :body body}))

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
  (GET "/meta/:v" [v] ((wrap-exception meta-handler) v))
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