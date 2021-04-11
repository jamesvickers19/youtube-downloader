(ns youtube_downloader.download
  (:require [clojure.tools.trace :refer :all]
            [clojure.string :refer [split-lines]]
            [youtube-downloader.files :refer :all]
            [clj-http.client :as client])
  (:import
    (java.io File)
    (com.github.kiulian.downloader YoutubeDownloader)
    (com.github.kiulian.downloader.model Extension YoutubeVideo)))

(defn parse-int [s] (Integer/parseInt s))

; (duration-to-seconds "1:30") => 90
(defn duration-to-seconds [s]
  (let [amounts (map parse-int (-> s (.split ":") reverse))
        to-seconds (fn [idx num] (int (* num (Math/pow 60 idx))))]
    (reduce + (map-indexed to-seconds amounts))))

(defn get-video ^YoutubeVideo [video-id]
  (.getVideo (YoutubeDownloader.) video-id))

(defn video-description [^YoutubeVideo video]
  (-> video .details .description))

(defn video-title [^YoutubeVideo video]
  (-> video .details .title))

(defn video-length [^YoutubeVideo video]
  (-> video .details .lengthSeconds))

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
  ([video-id]
   (let [vid (get-video video-id)]
     (get-sections (video-title vid) (video-description vid) (video-length vid))))
  ([title description overall-length]
   (let [sections (->> description section-strings (map make-section) (sort-by :start))
         with-end-time (fn [idx m]
                         (let [end (or (:start (next-or-nil sections idx))
                                       overall-length)]
                           (assoc m :end end)))]
     {:title title
      :length overall-length
      :sections (map-indexed with-end-time sections)})))

(defn highest-quality-mp4
  [^YoutubeVideo vid]
  (let [formats (.findAudioWithExtension vid Extension/M4A)]
    (apply max-key #(.averageBitrate %) formats)))

(defn download-audio
  [video-id filename]
  (let [vid (get-video video-id)
        audioFormat (highest-quality-mp4 vid)
        out-dir (dir filename)]
    (.download vid audioFormat (File. out-dir) (file-name filename) true)))

(defn download-audio-bytes
  [video-id]
  (let [vid (get-video video-id)
        audioFormat (highest-quality-mp4 vid)
        url (.url audioFormat)
        response (client/get url {:as :byte-array})
        status (:status response)]
    (if (not= 200 status)
      (throw (Exception. "Bad status code for audio" status))
      (:body response))))

(comment
  (get-sections "HjxZYiTpU3k")
  (download-audio "HjxZYiTpU3k" "C:\\Users\\james\\Downloads\\" "test")
  (let [response (download-audio-bytes "2dNGPkoDzh0")]
    (with-open [w (java.io.BufferedOutputStream. (java.io.FileOutputStream. "C:\\Users\\james\\Downloads\\test.mp4"))]
      (.write w response))))