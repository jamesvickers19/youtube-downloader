(ns youtube_downloader.download
  (:require [clojure.java.shell :refer [sh]]
            [clojure.tools.trace :refer :all]
            [clojure.string :refer [split-lines]])
  (:import
    (java.io File)
    (com.github.kiulian.downloader YoutubeDownloader)
    (com.github.kiulian.downloader.model Extension YoutubeVideo)))

(defn parse-int [s] (Integer/parseInt s))

; (duration-to-seconds "1:30") => 90
(defn duration-to-seconds [s]
  (let [amounts (map parse-int (-> s (.split ":") reverse))
        to-seconds (fn [idx num] (* num (Math/pow 60 idx)))]
    (reduce + (map-indexed to-seconds amounts))))

(defn get-video ^YoutubeVideo [video-id]
  (.getVideo (YoutubeDownloader.) video-id))

(defn video-description [^YoutubeVideo video]
  (-> video .details .description))

(defn next-or-nil
  [coll idx]
  (nth coll (inc idx) nil))

(defn section-strings [s]
  (let [lines (split-lines s)
        matches (map #(re-find #"\[*(\d+:\d+:?\d+)\]* (.*)" %) lines)]
    (map rest (remove nil? matches))))

(defn make-section
  [[start name]]
  {:start (duration-to-seconds start) :name name})

(defn get-sections
  ([^YoutubeVideo vid]
   (get-sections (video-description vid) (-> vid .details .lengthSeconds)))
  ([description overall-length]
   (let [sections (->> description section-strings (map make-section) (sort-by :start))
         with-end-time (fn [idx m]
                         (let [end (or (:start (next-or-nil sections idx))
                                       overall-length)]
                           (assoc m :end end)))]
     (map-indexed with-end-time sections))))

(defn highest-quality-mp4
  [^YoutubeVideo vid]
  (let [formats (.findAudioWithExtension vid Extension/M4A)]
    (apply max-key #(.averageBitrate %) formats)))

(defn download-audio
  [video-id out-dir filename]
  (let [vid (get-video video-id)
        audioFormat (highest-quality-mp4 vid)]
    (.download vid audioFormat (File. out-dir) filename true)))

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

(comment
  (get-sections (get-video "HjxZYiTpU3k"))
  (download-audio "HjxZYiTpU3k" "C:\\Users\\james\\Downloads\\" "test")
  (time (section-file "C:\\Users\\james\\Downloads\\test.m4a" (take 3 (get-sections (get-video "HjxZYiTpU3k"))))))