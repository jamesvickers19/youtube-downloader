(ns download_integration_test
  (:require [clojure.test :refer :all]
            [erdos.assert :as pa]
            [clojure.tools.trace :refer :all]
            [youtube_downloader.download :refer :all]
            [youtube-downloader.section-videos :refer :all]
            [byte-streams :refer [to-byte-array]]))

(def video-id "qz1EpqwMmf4")

(deftest get-meta-from-youtube-tests
  (pa/is (= {:title "Voldemort Laughing"
             :length 3
             :sections [{:start 0 :end 2 :name "section 1"}
                        {:start 2 :end 3 :name "section 2"}]}
            (get-meta video-id))))

(deftest download-audio-tests
  (let [audio-bytes (download video-id false)]
    (pa/is (= 55230 (alength audio-bytes)))
    (let [name1 "section 1"
          name2 "section 2"
          section-input-1 {:name name1 :start 0 :end 2}
          section-input-2 {:name name2 :start 2 :end 3}
          sections [section-input-1 section-input-2]
          [section-result-1 section-result-2] (section-video audio-bytes sections)]
      (pa/is (= 33799 (alength (:data section-result-1))))
      (pa/is (= 16619 (alength (:data section-result-2)))))))

(deftest download-video-tests
  (let [video-bytes (download video-id true)]
    (pa/is (= 57570 (alength video-bytes)))
    (let [name1 "section 1"
          name2 "section 2"
          section-input-1 {:name name1 :start 0 :end 2}
          section-input-2 {:name name2 :start 2 :end 3}
          sections [section-input-1 section-input-2]
          [section-result-1 section-result-2] (section-video video-bytes sections)]
      (pa/is (= 37937 (alength (:data section-result-1))))
      (pa/is (= 16203 (alength (:data section-result-2)))))))