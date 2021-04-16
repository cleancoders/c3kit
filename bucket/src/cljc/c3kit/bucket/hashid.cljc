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

(defn hashid-fns
  "Given the salt and legth, returns a map of fns:
    :id->hash - turns id into a hashid
    :hash->id - turns hashid into an int"
  [salt min-length]
  (let [conf #?(:clj {:salt salt :min-length min-length}
                :cljs (js/Hashids. salt min-length))]
    {:id->hash (fn [id] (-id->hash conf id))
     :hash->id (fn [hash] (-hash->id conf hash))}))
