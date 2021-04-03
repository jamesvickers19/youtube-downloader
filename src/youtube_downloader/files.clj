(ns youtube-downloader.files
  (:require [clojure.java.io :as io])
  (:import (java.io File)))

(defn ^String dir [^String path]
  (.getParent (File. path)))

(defn ^String file-name [^String path]
  (.getName (File. path)))

(defn file-exists? [^String path]
  (.exists (io/file path)))