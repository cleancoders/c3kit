(ns c3kit.bucket.spec-helper
  (:require-macros [speclj.core :refer [around before stub with-stubs]])
  (:require
    [c3kit.bucket.db :as db]
    [c3kit.apron.legend :as legend]
    [speclj.core]
    ))

(defn with-db-schemas [schemas]
  (let [schemas (if (sequential? schemas) (flatten schemas) [schemas])
        schema-map (reduce #(assoc %1 (-> %2 :kind :value) %2) {} schemas)]
    (around [it]
      (db/clear!)
      (with-redefs [legend/index schema-map]
        (it)))))
