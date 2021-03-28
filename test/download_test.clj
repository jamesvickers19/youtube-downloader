(ns download_test
  (:require [clojure.test :refer :all]
            [youtube_downloader.download :refer :all]))

(defn section-map
  [start end name]
  {:start start :end end :name name})

(deftest duration-to-seconds-tests
  (is (= 12 (duration-to-seconds "12")))
  (is (= 12 (duration-to-seconds "0:12")))
  (is (= 90 (duration-to-seconds "1:30")))
  (is (= 90 (duration-to-seconds "01:30")))
  (is (= 5025 (duration-to-seconds "1:23:45")))
  (is (= 5025 (duration-to-seconds "01:23:45"))))

(deftest get-sections-tests
 (is (= {:name "video name"
         :length 1234
         :sections []}
        (get-sections "video name" "no timestamps in here" 1234)))
 (is (= {:name "vid name"
         :length 180
         :sections [(section-map 12 154 "a name")
                    (section-map 154 180 "another name")]}
        (get-sections "vid name" "some video\n 00:12 a name\n [02:34] another name\n" 180))))