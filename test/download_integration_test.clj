(ns download_integration_test
  (:require [clojure.test :refer :all]
            [clojure.tools.trace :refer :all]
            [youtube_downloader.download :refer :all]
            [youtube-downloader.section-videos :refer :all]))

(def video-id "qz1EpqwMmf4")

(deftest get-sections-from-youtube-tests
  (is (= {:title "Voldemort Laughing"
          :length 3
          :sections [{:start 0 :end 2 :name "section 1"}
                     {:start 2 :end 3 :name "section 2"}]}
         (get-sections video-id))))

(defn assert-bytes [byte-arr]
  (is (> (alength byte-arr) 0)))

(deftest download-audio-tests
  (let [audio-bytes (download-audio-bytes video-id)]
    (assert-bytes audio-bytes)
    (let [name1 "section 1"
          name2 "section 2"
          sections [{:name name1 :start 0 :end 2}
                    {:name name2 :start 2 :end 3}]
          [section1 section2] (section-video audio-bytes sections)]
      (is (= name1 (:name section1)))
      (assert-bytes (-> section1 :result :out))
      (is (= name2 (:name section2)))
      (assert-bytes (-> section2 :result :out)))))

; TODO refactor for conciseness
; TODO use power assert