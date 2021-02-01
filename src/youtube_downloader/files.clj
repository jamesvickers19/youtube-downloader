(ns youtube-downloader.files
  (:require [clojure.java.io :as io])
  (:import (java.io File)))

(defn dir [path]
  (.getParent (File. path)))

(defn file-name [path]
  (.getName (File. path)))

(defn file-exists? [path]
  (.exists (io/file path)))