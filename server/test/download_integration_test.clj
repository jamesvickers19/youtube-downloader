(ns download_integration_test
  (:require [clojure.test :refer :all]
            [clojure.tools.trace :refer :all]
            [youtube_downloader.download :refer :all]
            [youtube-downloader.section-videos :refer :all]
            [byte-streams :refer [to-byte-array]]
            [clojure.java.io :as io])
  (:import (java.io File)))

(def video-id "qz1EpqwMmf4")

(deftest get-meta-from-youtube-tests
  (is (= {:title "Voldemort Laughing"
          :length 3
          :sections [{:start 0 :end 2 :name "section 1"}
                     {:start 2 :end 3 :name "section 2"}]}
         (get-meta video-id))))

(defn assert-size-bytes [^String filename expected-length]
  (let [contents (to-byte-array (File. filename))]
    (is (= expected-length (alength contents)))))

(defn verify-section-result-and-remove-file
  [{:keys [name] :as section-input} section-result expected-length]
  (let [expected-filename (str name ".mp4")]
    (is (= (assoc section-input :filename expected-filename)
           section-result))
    (assert-size-bytes expected-filename expected-length)
    (io/delete-file expected-filename)))

(deftest download-audio-tests
  (let [audio-bytes (download video-id false)]
    (is (= '[B (type audio-bytes)]))
    (is (= 55230 (alength audio-bytes)))
    (let [name1 "section 1"
          name2 "section 2"
          section-input-1 {:name name1 :start 0 :end 2}
          section-input-2 {:name name2 :start 2 :end 3}
          sections [section-input-1 section-input-2]
          [section-result-1 section-result-2] (section-video audio-bytes sections)]
      (verify-section-result-and-remove-file section-input-1 section-result-1 34017)
      (verify-section-result-and-remove-file section-input-2 section-result-2 16865))))

; TODO refactor for conciseness
; TODO use power assert