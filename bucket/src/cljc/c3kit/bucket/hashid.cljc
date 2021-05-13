(ns c3kit.bucket.hashid
  (:require
    [c3kit.apron.log :as log]
     #?(:clj [hashids.core :as hashid])
     #?(:cljs [cljsjs.hashids])
     ))

(defn- -id->hash [conf id]
  (when id
    (try
      #?(:clj  (hashid/encode conf id)
         :cljs (.encode conf id))
      (catch #?(:clj Throwable :cljs :default) e
        (log/warn "failed to convert id to hash:" hash)
        (log/warn e)
        nil))))

(defn- -hash->id [conf hash]
  (when (and hash (string? hash) (not= "" hash))
    (try
      #?(:clj  (first (hashid/decode conf hash))
         :cljs (first (.decode conf hash)))
      (catch #?(:clj Throwable :cljs :default) e
        (log/warn "failed to convert hash to id:" hash)
        (log/warn e)
        nil))))

(defn hashid-fns
  "Given the salt and length, returns a map of fns:
    :id->hash - turns id into a hashid
    :hash->id - turns hashid into an int"
  [salt min-length]
  (let [conf #?(:clj {:salt salt :min-length min-length}
                :cljs (js/Hashids. salt min-length))]
    {:id->hash (fn [id] (-id->hash conf id))
     :hash->id (fn [hash] (-hash->id conf hash))}))
