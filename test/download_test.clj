(ns download_test
  (:require [clojure.test :refer :all]
            [youtube_downloader.download :refer :all]))

(deftest get-sections-tests
 (is (empty? (get-sections "no timestamps in here" nil))))