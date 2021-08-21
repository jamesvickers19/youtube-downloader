(ns youtube-downloader.section-videos
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :refer [input-stream]]))

(defn time-args [{:keys [filename start end]}]
  ["-ss" (str start) "-t" (str (- end start)) "-c" "copy" filename])

(defn section-video
  [input sections]
  (let [sections (map #(assoc % :filename (str (:name %) ".mp4")) sections)
        section-args (map time-args sections)
        all-args (flatten ["ffmpeg" "-y" "-i" "pipe:" "-f" "mp4" section-args :in input])
        exec-result (apply sh all-args)]
    (if (not= 0 (:exit exec-result))
      (throw (Exception. (str "Error sectioning videos: " (:err exec-result))))
      sections)))