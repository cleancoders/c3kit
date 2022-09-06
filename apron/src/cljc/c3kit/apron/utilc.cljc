(ns c3kit.apron.utilc
  (:refer-clojure :exclude [format])
  #?(:clj (:import (java.util UUID)
                   (java.io ByteArrayInputStream ByteArrayOutputStream)))
  (:require #?(:clj [clojure.data.json :as json])
            [c3kit.apron.schema :as schema]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [cognitect.transit :as transit]))

(defn ->edn
  "Convenience.  Convert the form to EDN"
  [v] (if v (pr-str v) nil))

(defn <-edn
  "Convenience.  Convert the EDN string to a Clojure form"
  [s] (edn/read-string s))

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

(defn ->uuid-or-nil
  "Parse a string into a UUID or return nil if it's not a vlid UUID format"
  [uuid-str]
  (try (schema/->uuid uuid-str)
       (catch #?(:clj Exception :cljs :default) _
         nil)))

; ----- Transit -----

#?(:cljs (def transit-reader (transit/reader :json {:handlers {"f" js/parseFloat "n" js/parseInt}})))
#?(:cljs (def transit-writer (transit/writer :json)))

(defn ->transit
  "Convert data into transit string"
  ([type opts data]
   #?(:clj  (let [baos   (ByteArrayOutputStream.)
                  writer (transit/writer baos type opts)]
              (transit/write writer data)
              (.close baos)
              (.toString baos))
      :cljs (transit/write (transit/writer type opts) data)))
  ([data]
   #?(:clj  (->transit :json {} data)
      :cljs (transit/write transit-writer data))))

(defn <-transit
  "Convert transit string into data"
  ([type opts ^String transit-str]
   #?(:clj  (with-open [in (ByteArrayInputStream. (.getBytes transit-str))]
              (transit/read (transit/reader in type opts)))
      :cljs (transit/read (transit/reader type opts) transit-str)))
  ([^String transit-str]
   #?(:clj  (<-transit :json {} transit-str)
      :cljs (transit/read transit-reader transit-str))))

; ^^^^^ Transit ^^^^^

; ----- JSON -----

(defn ->json
  "Convert the clj data structure to JSON.
  Note: this transition may be lossy since some clj data types (keywords) have no JSON equivalent."
  [v]
  #?(:clj  (json/write-str v)
     :cljs (.stringify js/JSON (clj->js v))))

(defn <-json
  "Convert JSON into clj data structure."
  [v]
  (when (and v (not (str/blank? v)))
    #?(:clj  (json/read-str v)
       :cljs (js->clj (.parse js/JSON v)))))

(defn <-json-kw
  "Convert JSON into clj data structure with all keys as keywords"
  [v]
  #?(:clj  (json/read-str v :key-fn keyword)
     :cljs (walk/keywordize-keys (<-json v))))

; ^^^^^ JSON ^^^^^

; ----- CSV -----

(defn- csv-maybe-quote [value]
  (if (or (str/index-of value ",") (str/index-of value "\""))
    (str "\"" (str/replace value "\"" "\"\"") "\"")
    value))

(defn cell->csv [cell]
  (-> (str cell)
      csv-maybe-quote))

(defn row->csv [row]
  (str/join "," (map cell->csv row)))

(defn ->csv
  "Simple CSV generator for a list of lists"
  [rows]
  (str/join "\r\n" (map row->csv rows)))

; ^^^^^ CSV ^^^^^

(defn ->filename
  "Sanatize string into valid filename"
  ([name] (-> (str name)
              (str/replace #"[ -]" "_")
              (str/replace #"[',.-/\\<>:\"\\|?*\[\]]" "")))
  ([name ext] (str (->filename name) "." ext)))
