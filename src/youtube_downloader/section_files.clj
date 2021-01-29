(ns youtube-downloader.section-files
  (:require [clojure.java.shell :refer [sh]])
  (:import (java.io File)))


(defn dir [filename]
  (.getParent (File. filename)))

(defn quoted [s] (str "\"" s "\""))

(defn output-mp4-filename
  [input-file name]
  (quoted (str (dir input-file) File/separator name ".mp4")))

; TODO make this a bash script with a couple args?
(defn ffmpeg-args
  [input {:keys [start end output]}]
  ["ffmpeg" "-y" "-i" (quoted input) "-ss" (str start) "-t" (str (- end start)) output])

(defn run-in-bg
  [arg-seqs]
  (let [futures (doall (map #(future (apply sh %)) arg-seqs))]
    (filter #(not= 0 (:exit @%)) futures)))

(defn with-output
  [input-name {:keys [name] :as section}]
  (assoc section :output (output-mp4-filename input-name name)))

(defn section-file
  [filename sections]
  (let [sections (map (partial with-output filename) sections)
        args (map (partial ffmpeg-args filename) sections)
        failures (run-in-bg args)]
    (if (seq failures)
      (throw (Exception. (str (vec (map #(:err @%) failures)))))
      (map :output sections))))