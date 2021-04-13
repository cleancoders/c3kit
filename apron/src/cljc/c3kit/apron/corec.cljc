(ns c3kit.apron.corec
  "Common core code.  This file should have minimal dependencies.
  Clients should be able to safely :refer :all from this namespace."
  #?(:clj (:import (java.util UUID)))
  #?(:cljs (:require-macros [c3kit.apron.corec :refer [for-all]]))
  (:require
    [clojure.edn :as edn]
    ;#?(:clj [hashids.core :as hashid])
    ;#?(:cljs [cljsjs.hashids])
    #?(:cljs [goog.string :as gstring])
    #?(:cljs [goog.string.format])
    ))

#?(:clj (defmacro for-all [bindings body]
          `(doall (for ~bindings ~body))))

;; ----- hashids (https://hashids.org/) --------------------------------------------------------------------------------

;(defn- -id->hash [conf id]
;  (when id
;    #?(:clj  (hashid/encode conf id)
;       :cljs (.encode conf id))))
;
;(defn- -hash->id [conf hash]
;  (when (and hash (string? hash) (not= "" hash))
;    #?(:clj  (first (hashid/decode conf hash))
;       :cljs (first (.decode conf hash)))))
;
;(defn hashid-fns [salt min-length]
;  (let [conf #?(:clj {:salt salt :min-length min-length}
;                :cljs (js/Hashids. salt min-length))]
;    {:id->hash (fn [id] (-id->hash conf id))
;     :hash->id (fn [hash] (-hash->id conf hash))}))
;
;;; Use to obfuscate database ids.  Passing to/from client/server using fns below.
;;; DO NOT CHANGE THESE VALUES, else ephemeral data will be invalidated (sessions, page state, etc)
;(def hashid-salt "how about a game of poker")
;(def hashid-min-length 10) ;; with default alphabet (62 chars), hashids of length 10 should give 8x10e17 possibilities
;
;#?(:clj  (def hash-opts {:salt hashid-salt :min-length hashid-min-length})
;   :cljs (def hasher (js/Hashids. hashid-salt hashid-min-length)))
;
;(def hashid-configured-fns (hashid-fns hashid-salt hashid-min-length))
;(def id->hash (:id->hash hashid-configured-fns))
;(def hash->id (:hash->id hashid-configured-fns))
;
;(defn hashid [entity] (id->hash (:id entity)))
;
;(defn ->hashid [thing]
;  (cond (number? thing) (id->hash thing)
;        (string? thing) thing
;        (nil? thing) thing
;        (map? thing) (hashid thing)
;        :else thing))

;; ^^^^ hashids ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

(defn new-uuid []
  #?(:clj  (UUID/randomUUID)
     :cljs (random-uuid)))

(defn conjv [col item]
  "ensures the seq is a vector before conj-ing"
  (conj (vec col) item))

(defn concatv [& cols]
  "ensures the seq is a vector after concat-ing"
  (vec (apply concat cols)))

(defn dissocv [col i]
  "removes the item at index i from the vector"
  (vec (concat (subvec col 0 i) (subvec col (inc i)))))

(defn removev [pred col]
  "core/remove returning a vector"
  (vec (remove pred col)))

(defn removev= [col item]
  "Using =, returns vector without item"
  (removev #(= % item) col))

(defn vec-remove
  "remove elem in coll"
  [pos coll]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn vec-insert
  "remove elem in coll"
  [pos elem coll]
  (vec (concat (subvec coll 0 pos) [elem] (subvec coll pos))))

(defn ->inspect
  "Insert in threading macro to print the value."
  [v]
  (println "v: " v)
  v)

(defn ->options
  "Takes keyword argument and converts them to a map.  If the args are prefixed with a map, the rest of the
  args are merged in."
  [options]
  (cond
    (nil? options) {}
    (= [nil] options) {}
    (map? (first options)) (merge (first options) (apply hash-map (rest options)))
    :else (apply hash-map options)))

(defn keywordize-kind
  "Makes sure and entity has the keyword as the value of :kind"
  [entity]
  (if-let [kind (:kind entity)]
    (cond
      (keyword? kind) entity
      (string? kind) (assoc entity :kind (keyword kind))
      :else (throw (ex-info "Invalid :kind type" entity)))
    (throw (ex-info "Missing :kind" entity))))

(defn ->edn [v] (if v (pr-str v) nil))
(defn <-edn [s] (edn/read-string s))

(defn formats [format & args]
  #?(:clj  (apply clojure.core/format format args)
     :cljs (apply gstring/format format args)))

(defn remove-nils [e]
  (reduce (fn [r [k v]] (if (= nil v) r (assoc r k v))) {} e))

(defn ex? [e]
  #?(:clj  (instance? Exception e)
     :cljs (instance? js/Error e)))

(defn noop [& _])

(defn index-by-id [entities] (reduce #(assoc %1 (:id %2) %2) {} entities))
