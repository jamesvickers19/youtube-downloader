(ns youtube-downloader.section-videos
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :refer [input-stream]]))

(defn section-exec
  [input {:keys [start end]}]
  (sh "ffmpeg" "-y" "-i" "pipe:" "-f" "mp3" "-ss" (str start) "-t" (str (- end start)) "-"
      :in input
      :out-enc :bytes))

(defn section-video
  [input sections]
  (map #(assoc % :result (section-exec input %)) sections))