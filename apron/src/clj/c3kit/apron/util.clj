(ns c3kit.apron.util
  (:import [java.security MessageDigest DigestInputStream]
           (java.io InputStream OutputStream))
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
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

(defn establish-path [path]
  (let [file (io/file path)
        parent (.getParentFile file)]
    (when (not (.exists parent))
      (.mkdirs parent))))

(defn resolve-var
  "Given the symbol of a fully qualified var name, load the namespace and return the var.
  Throws an exception if the var doesnt exist.  Deref the var to get it's value.
  Use to decouple code; dynamically load code."
  [var-sym]
  (let [ns-sym (symbol (namespace var-sym))
        var-sym (symbol (name var-sym))]
    (require ns-sym)
    (if-let [var (ns-resolve (the-ns ns-sym) var-sym)]
      var
      (throw (Exception. (str "No such var " (name ns-sym) "/" (name var-sym)))))))

(defn resolve-var-or-nil
  "Same as resolve-var except that it returns nil if the var doesn't exist"
  [var-sym]
  (try
    (resolve-var var-sym)
    (catch Exception e
      ;(log/error e)
      nil)))

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

(defn ->edn
  "Convenience.  Convert the form to EDN"
  [v] (if v (pr-str v) nil))

(defn <-edn
  "Convenience.  Convert the EDN string to a Clojure form"
  [s] (edn/read-string s))

(defn read-edn-resource
  "Find file in classpath, read as EDN, and return form."
  [path]
  (if-let [result (io/resource path)]
    (<-edn (slurp result))
    (throw (ex-info (str "Failed to read edn resource: " path) {:path path}))))

(defn index-by-id
  "Give a list of entities with unique :id's, return a map with the ids as keys and the entities as values"
  [entities]
  (reduce #(assoc %1 (:id %2) %2) {} entities))

(defn keywordize-kind
  "Makes sure and entity has the keyword as the value of :kind"
  [entity]
  (if-let [kind (:kind entity)]
    (cond
      (keyword? kind) entity
      (string? kind) (assoc entity :kind (keyword kind))
      :else (throw (ex-info "Invalid :kind type" entity)))
    (throw (ex-info "Missing :kind" entity))))
