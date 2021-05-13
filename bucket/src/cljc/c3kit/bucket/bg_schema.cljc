(ns c3kit.bucket.bg-schema
  (:require
    [c3kit.apron.schema :as s]
    ))

(def bg-task
  {:kind        (s/kind :bg-task)
   :id          s/id
   :key         {:type :keyword :db [:unique-identity]}
   :last-ran-at {:type :instant :db [:no-history]}
   })
