(ns download_test
  (:require [clojure.test :refer :all]
            [youtube_downloader.download :refer :all]))

(deftest duration-to-seconds-tests
  (is (= 12 (duration-to-seconds "12")))
  (is (= 12 (duration-to-seconds "0:12")))
  (is (= 90 (duration-to-seconds "1:30")))
  (is (= 90 (duration-to-seconds "01:30")))
  (is (= 5025 (duration-to-seconds "1:23:45")))
  (is (= 5025 (duration-to-seconds "01:23:45"))))