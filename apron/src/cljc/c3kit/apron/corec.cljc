(ns c3kit.apron.corec
  "Common core code.  This file should have minimal dependencies.
  Clients should be able to safely :refer :all from this namespace."
  #?(:clj (:import (java.util UUID)))
  #?(:cljs (:require-macros [c3kit.apron.corec :refer [for-all]]))
  #?(:cljs (:require
    ;#?(:clj [hashids.core :as hashid])
    ;#?(:cljs [cljsjs.hashids])
    [goog.string :as gstring]
    [goog.string.format]
    )))

#?(:clj (defmacro for-all [bindings body]
          `(doall (for ~bindings ~body))))

(defn new-uuid []
  #?(:clj  (UUID/randomUUID)
     :cljs (random-uuid)))

(defn conjv
  "ensures the seq is a vector before conj-ing"
  [col item]
  (conj (vec col) item))

(defn concatv
  "ensures the seq is a vector after concat-ing"
  [& cols]
  (vec (apply concat cols)))

(defn dissocv
  "removes the item at index i from the vector"
  [col i]
  (vec (concat (subvec col 0 i) (subvec col (inc i)))))

(defn assocv
  "insert elem into vector at index "
  [coll i elem]
  (vec (concat (subvec coll 0 i) [elem] (subvec coll i))))

(defn removev [pred col]
  "core/remove returning a vector"
  (vec (remove pred col)))

(defn removev= [col item]
  "Using =, returns vector without item"
  (removev #(= % item) col))

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


(defn formats
  "Platform agnostic string format fm"
  [format & args]
  #?(:clj  (apply clojure.core/format format args)
     :cljs (apply gstring/format format args)))

(defn remove-nils
  "Return a map where all the keys with nil values are removed"
  [e]
  (reduce (fn [r [k v]] (if (= nil v) r (assoc r k v))) {} e))

(defn ex?
  "Returns true is e is an exception/error for the running platform"
  [e]
  #?(:clj  (instance? Exception e)
     :cljs (instance? js/Error e)))

(defn noop
  "Does nothing"
  [& _])
