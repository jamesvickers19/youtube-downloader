(ns youtube_downloader.download
  (:require [clojure.tools.trace :refer :all]
            [clojure.string :refer [split-lines]]
            [youtube-downloader.files :refer :all])
  (:import
    (com.github.kiulian.downloader YoutubeDownloader)
    (com.github.kiulian.downloader.model.videos VideoInfo)
    (com.github.kiulian.downloader.downloader.request RequestVideoInfo RequestVideoStreamDownload)
    (java.io ByteArrayOutputStream)
    (com.github.kiulian.downloader.model.videos.formats Format)))

(defn parse-int [s] (Integer/parseInt s))

; (duration-to-seconds "1:30") => 90
(defn duration-to-seconds [s]
  (let [amounts (map parse-int (-> s (.split ":") reverse))
        to-seconds (fn [idx num] (int (* num (Math/pow 60 idx))))]
    (reduce + (map-indexed to-seconds amounts))))


(defn video-description [^VideoInfo video]
  (-> video .details .description))

(defn video-title [^VideoInfo video]
  (-> video .details .title))

(defn video-length [^VideoInfo video]
  (-> video .details .lengthSeconds))

(defn live-video? [^VideoInfo video]
  (-> video .details .isLiveContent))

(defn get-video-info ^VideoInfo [video-id]
  (let [downloader (YoutubeDownloader.)
        request (RequestVideoInfo. video-id)
        response (.getVideoInfo downloader request)]
    (.data response)))

(defn get-video-info-not-live ^VideoInfo [video-id]
  (let [v (get-video-info video-id)]
    (if (live-video? v)
      (throw (Exception. (str "Video '" video-id "' is a live video")))
      v)))

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

(defn get-meta
  ([video-id]
   (let [vid-info (get-video-info-not-live video-id)]
     (get-meta (video-title vid-info) (video-description vid-info) (video-length vid-info))))
  ([title description overall-length]
   (let [sections (->> description section-strings (map make-section) (sort-by :start))
         with-end-time (fn [idx m]
                         (let [end (or (:start (next-or-nil sections idx))
                                       overall-length)]
                           (assoc m :end end)))]
     {:title title
      :length overall-length
      :sections (map-indexed with-end-time sections)})))

(defn download-format
  [video-id format]
  (let [os (ByteArrayOutputStream.)
        request (RequestVideoStreamDownload. format os)
        response (.downloadVideoStream (YoutubeDownloader.) request)]
    (if (.ok response)
      (.toByteArray os)
      (throw (Exception. (str "Couldn't get data contents for video id " video-id))))))

(defn download
  ([video-id include-video]
   (let [vid (get-video-info-not-live video-id)
         format (if include-video (.bestVideoWithAudioFormat vid)
                                  (.bestAudioFormat vid))]
     (download-format video-id format))))

(comment
  (get-meta "HjxZYiTpU3k")
  (download-audio "HjxZYiTpU3k" "C:\\Users\\james\\Downloads\\" "test")
  (let [response (download-audio-bytes "2dNGPkoDzh0")]
    (with-open [w (java.io.BufferedOutputStream. (java.io.FileOutputStream. "C:\\Users\\james\\Downloads\\test.mp4"))]
      (.write w response))))