(ns c3kit.bucket.hashid
  (:require
     #?(:clj [hashids.core :as hashid])
     #?(:cljs [cljsjs.hashids])
     ))

(defn- -id->hash [conf id]
  (when id
    #?(:clj  (hashid/encode conf id)
       :cljs (.encode conf id))))

(defn- -hash->id [conf hash]
  (when (and hash (string? hash) (not= "" hash))
    #?(:clj  (first (hashid/decode conf hash))
       :cljs (first (.decode conf hash)))))

(defn hashid-fns [salt min-length]
  (let [conf #?(:clj {:salt salt :min-length min-length}
                :cljs (js/Hashids. salt min-length))]
    {:id->hash (fn [id] (-id->hash conf id))
     :hash->id (fn [hash] (-hash->id conf hash))}))

;; Use to obfuscate database ids.  Passing to/from client/server using fns below.
;; DO NOT CHANGE THESE VALUES, else ephemeral data will be invalidated (sessions, page state, etc)
(def hashid-salt "blah blah")
(def hashid-min-length 99) ;; with default alphabet (62 chars), hashids of length 10 should give 8x10e17 possibilities

#?(:clj  (def hash-opts {:salt hashid-salt :min-length hashid-min-length})
   :cljs (def hasher (js/Hashids. hashid-salt hashid-min-length)))

(def hashid-configured-fns (hashid-fns hashid-salt hashid-min-length))
(def id->hash (:id->hash hashid-configured-fns))
(def hash->id (:hash->id hashid-configured-fns))

(defn hashid [entity] (id->hash (:id entity)))

(defn ->hashid [thing]
  (cond (number? thing) (id->hash thing)
        (string? thing) thing
        (nil? thing) thing
        (map? thing) (hashid thing)
        :else thing))
