(ns c3kit.apron.util
  (:import [java.security MessageDigest DigestInputStream]
           (java.io InputStream OutputStream))
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
;     [poker.config :as config]
    ))

(defn files-in
  ([pred path] (files-in pred path (.list (io/file path)) []))
  ([pred base [name & more] result]
   (if (not name)
     result
     (let [path (str base "/" name)]
       (cond
         (#{"." ".."} name) (recur pred base more result)
         (.isDirectory (io/file path)) (recur pred base more (concat result (files-in pred path)))
         (pred name) (recur pred base more (conj result (str base "/" name)))
         :else (recur pred base more result))))))

(defn clj-files-in [path]
  (files-in #(.endsWith % ".clj") path))

(defn filename->ns [filename]
  (-> filename
      (str/replace "src/clj/" "")
      (str/replace "/" ".")
      (str/replace "_" "-")
      (str/replace ".clj" "")))

(defn establish-path [path]
  (let [file (io/file path)
        parent (.getParentFile file)]
    (when (not (.exists parent))
      (.mkdirs parent))))

;(defn load-namespace [name]
;  (try
;    (let [ns-sym (symbol name)]
;      (require [ns-sym :reload true])
;      (the-ns ns-sym))
;    (catch Error e
;      (println "Failed to load ns:" name)
;      (println e))))

(defn resolve-var [var-sym]
  (let [ns-sym (symbol (namespace var-sym))
        var-sym (symbol (name var-sym))]
    (require ns-sym)
    (if-let [var (ns-resolve (the-ns ns-sym) var-sym)]
      var
      (throw (Exception. (str "No such var " (name ns-sym) "/" (name var-sym)))))))

(defn resolve-var-or-nil [var-sym]
  (try
    (resolve-var var-sym)
    (catch Exception e
      ;(log/error e)
      nil)))

; (def dev-only-refresh
;   (if config/development?
;     @(resolve-var 'poker.refresh/refresh!)
;     (fn [])))

(defn md5 [^String s]
  (let [alg (MessageDigest/getInstance "md5")
        bytes (.getBytes s "UTF-8")]
    (format "%032x" (BigInteger. 1 (.digest alg bytes)))))

(def null-output-stream (proxy [OutputStream] [] (write ([_]) ([_ _ _]))))

(defn stream->md5 [^InputStream s]
  (let [alg (MessageDigest/getInstance "md5")
        dis (DigestInputStream. s alg)]
    (io/copy dis null-output-stream)
    (format "%032x" (BigInteger. 1 (.digest alg)))))

;(defn schema->db-schema [schema]
;  (if-let [enum (:enum schema)]
;    (db/build-enum-schema enum (schema/db-schema schema))
;    (let [kind (get-in schema [:kind :value])]
;      (assert kind (str "kind missing: " schema))
;      (assert (keyword? kind) (str "kind must be keyword: " kind))
;      (db/build-schema kind (schema/db-schema schema)))))
