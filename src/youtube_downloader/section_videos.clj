(ns youtube-downloader.section-videos
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :refer [input-stream]]
            [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream)))

(defn section-exec
  [input {:keys [start end]}]
  (sh "ffmpeg" "-y" "-i" "pipe:" "-f" "mp3" "-ss" (str start) "-t" (str (- end start)) "-"
      :in input
      :out-enc :bytes))

; only for testing, can delete
(defn write-file [input]
  (with-open [w (java.io.BufferedOutputStream. (java.io.FileOutputStream. "C:\\Users\\James\\Downloads\\test-section.mp4"))]
    (.write w input)))

(comment
  (write-file
    (:out
      (section-exec (input-stream "C:\\Users\\James\\Downloads\\test.mp4") {:start 2 :end 4}))))

; associates sh result with each section map
; TODO put extension on :name ?  otherwise it's file won't have like .mp4
(defn section-video
  [input sections]
  (map #(assoc % :result (section-exec input %)) sections))

; only for testing, can delete
(defn file->bytes [file]
  (with-open [xin (io/input-stream file)
              xout (ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(comment
  (section-video
    (file->bytes "C:\\Users\\James\\Downloads\\test.mp4")
    [{:name "name 1" :start 1 :end 2}
     {:name "name 2" :start 3 :end 4}]))