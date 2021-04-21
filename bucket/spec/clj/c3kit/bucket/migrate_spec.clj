(ns c3kit.bucket.migrate-spec
  (:require
    [c3kit.bucket.migrate :as sut]
    [speclj.core :refer :all]
    ))

(def pet
  {:kind        (schema/kind :pet)
   :id          schema/id
   :species     {:type     :string
                 :validate [#{"dog" "cat" "snake"}]
                 :message  "must be a pet species"}
   :birthday    {:type    :instant
                 :message "must be a date"}
   :length      {:type    :float
                 :message "must be unit in feet"}
   :teeth       {:type     :int
                 :validate [#(and (<= 0 %) (<= % 999))]
                 :message  "must be between 0 and 999"}
   :name        {:type     :string
                 :db       [:unique-value]
                 :coerce   #(str % "y")
                 :validate #(> (count %) 1)
                 :message  "must be nice and unique name"}
   :owner       {:type     :ref
                 :validate [schema/present?]
                 :message  "must be a valid reference format"}
   :colors      {:type [:string]}
   :uuid        {:type :uuid
                 :db   [:unique-identity]}
   :temperament {:type :kw-ref}})

(def temperaments
  {:enum   :temperament
   :values [:wild :domestic]})

(describe "Migrate"

  (context "db schema"
    (it "converts to db format"
      (let [db-schema (sut/db-schema pet)]
        (should-not-contain [:kind :keyword] db-schema)
        (should-contain [:species :string] db-schema)
        (should-contain [:birthday :instant] db-schema)
        (should-contain [:length :float] db-schema)
        (should-contain [:teeth :int] db-schema)
        (should-contain [:name :string :unique-value] db-schema)
        (should-contain [:owner :ref] db-schema)
        (should-contain [:colors :string :many] db-schema)
        (should-contain [:uuid :uuid :unique-identity] db-schema)
        (should-contain [:temperament :kw-ref] db-schema)))

    (it "converts emum to db format"
      (let [enum-schema (sut/db-schema temperaments)]
        (should= [:wild :domestic] enum-schema)))
    )

)
