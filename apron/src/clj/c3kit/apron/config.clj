(ns c3kit.apron.config
  (:require
    [clojure.java.io :as io]
    ))


(defn read-config
  "Find file in classpath, read as EDN, and return form."
  [path]
  (if-let [result (io/resource path)]
    (read-string (slurp result))
    (throw (ex-info (str "Failed to read config: " path) {:path path}))))
