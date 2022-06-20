(ns c3kit.apron.util
  (:import [java.security MessageDigest DigestInputStream]
           (java.io InputStream OutputStream))
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [c3kit.apron.utilc :as utilc]
    [c3kit.apron.log :as log]))

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

(defn clj-files-in
  "Return a list of filenames of .clj files located within the specified path, recursively"
  [path]
  (files-in #(.endsWith % ".clj") path))

(defn filename->ns [filename]
  (-> filename
      (str/replace "src/clj/" "")
      (str/replace "/" ".")
      (str/replace "_" "-")
      (str/replace ".clj" "")))

(defn establish-path
  "Create any missing directories in path"
  [path]
  (let [file (io/file path)
        parent (.getParentFile file)]
    (when (not (.exists parent))
      (.mkdirs parent))))

(defn resolve-var
  "Given the symbol of a fully qualified var name, load the namespace and return the var.
  Throws an exception if the var doesnt exist.  Deref the var to get it's value.
  Use to decouple code; dynamically load code."
  [var-sym]
  (if-let [var (requiring-resolve var-sym)]
    var
    (throw (Exception. (str "No such var " var-sym)))))

(defn var-value
  "Using resolve-var, return the value of the var, or nil if it fails to resolve"
  [var-sym]
  (when var-sym
    (try
      @(resolve-var var-sym)
      (catch Exception e
        (log/warn "Unable to resolve var:" var-sym (str e))
        nil))))

(defn config-value
  "Given a symbol, extract value from resolved var, else return the given value"
  [sym-or-val]
  (when sym-or-val
    (if (symbol? sym-or-val)
      (var-value sym-or-val)
      sym-or-val)))

(defn md5
  "MD5 hash the string"
  [^String s]
  (let [alg (MessageDigest/getInstance "md5")
        bytes (.getBytes s "UTF-8")]
    (format "%032x" (BigInteger. 1 (.digest alg bytes)))))

(def null-output-stream (proxy [OutputStream] [] (write ([_]) ([_ _ _]))))

(defn stream->md5 [^InputStream s]
  "MD5 hash the input stream"
  (let [alg (MessageDigest/getInstance "md5")
        dis (DigestInputStream. s alg)]
    (io/copy dis null-output-stream)
    (format "%032x" (BigInteger. 1 (.digest alg)))))

(defn read-edn-resource
  "Find file in classpath, read as EDN, and return form."
  [path]
  (if-let [result (io/resource path)]
    (utilc/<-edn (slurp result))
    (throw (ex-info (str "Failed to read edn resource: " path) {:path path}))))

