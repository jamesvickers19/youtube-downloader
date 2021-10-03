(ns youtube-downloader.section-videos
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :refer [input-stream]])
  (:import (com.googlecode.mp4parser.authoring Track Movie)
           (com.googlecode.mp4parser MemoryDataSourceImpl DataSource)
           (com.googlecode.mp4parser.authoring.container.mp4 MovieCreator)
           (java.util ArrayList)
           (com.googlecode.mp4parser.authoring.tracks CroppedTrack)
           (com.googlecode.mp4parser.authoring.builder DefaultMp4Builder)
           (java.io ByteArrayOutputStream)
           (java.nio.channels Channels)))

(defn sample-index
  [^Track track time]
  (let [durations (.getSampleDurations track)
        timescale (double (.getTimescale (.getTrackMetaData track)))]
    (loop [i 0
           currentTime 0]
      (if (>= i (alength durations))
        i
        (if (>= currentTime time)
            i
            (recur (inc i) (+ currentTime (/ (double (aget durations i)) timescale))))))))

(defn get-section
  [^bytes input start end]
  (let [^DataSource source (MemoryDataSourceImpl. input)
        ^Movie movie (MovieCreator/build source)
        tracks (ArrayList. (.getTracks movie))]
    (.clear (.getTracks movie)) ; need to clear old movie tracks to replace
    (doseq [t tracks]
      (.addTrack movie (CroppedTrack. t (sample-index t start) (sample-index t end))))
    (with-open [^ByteArrayOutputStream bos (ByteArrayOutputStream.)]
      (.writeContainer (.build (DefaultMp4Builder.) movie) (Channels/newChannel bos))
      (.toByteArray bos))))

(defn section-video
  [^bytes input sections]
  (map #(assoc % :data (get-section input (:start %) (:end %))) sections))
