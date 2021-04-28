(ns c3kit.bucket.migrate-spec
  (:require
    [c3kit.apron.log :as log]
    [c3kit.apron.schema :as schema]
    [c3kit.bucket.dbc-spec :as dbc-spec]
    [c3kit.bucket.db :as db]
    [c3kit.bucket.migrate :as sut]
    [c3kit.bucket.spec-helper :as helper]
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
   :teeth       {:type     :long
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

;; Used in test migration
(def test-schema [pet temperaments])


(describe "Migrate"

  (context "db schema"
    (it "converts to db format"
      (let [db-schema (sut/db-schema pet)]
        (should-not-contain [:kind :keyword] db-schema)
        (should-contain [:species :string] db-schema)
        (should-contain [:birthday :instant] db-schema)
        (should-contain [:length :float] db-schema)
        (should-contain [:teeth :long] db-schema)
        (should-contain [:name :string :unique-value] db-schema)
        (should-contain [:owner :ref] db-schema)
        (should-contain [:colors :string :many] db-schema)
        (should-contain [:uuid :uuid :unique-identity] db-schema)
        (should-contain [:temperament :kw-ref] db-schema)))

    (it "converts emum to db format"
      (let [enum-schema (sut/db-schema temperaments)]
        (should= [:wild :domestic] enum-schema)))
    )

  (context "retract"

    (helper/with-db-schemas [dbc-spec/bibelot
                             dbc-spec/gewgaw
                             dbc-spec/doodad])

    (it "retract datoms simple"
      (let [blue (db/tx :kind :bibelot :name "Bluey" :color "blue" :size 123)
            red (db/tx :kind :bibelot :name "Redy" :color "red" :size 456)
            green (db/tx :kind :bibelot :name "Greeny" :color "green" :size 789)
            datoms (sut/retract-datoms :bibelot/name)]
        (should-contain [:db/retract (:id blue) :bibelot/name "Bluey"] datoms)
        (should-contain [:db/retract (:id red) :bibelot/name "Redy"] datoms)
        (should-contain [:db/retract (:id green) :bibelot/name "Greeny"] datoms)))

    (it "retract datoms ref"
      (let [blue (db/tx :kind :bibelot :name "Bluey" :color "blue" :size 123)
            sky (db/tx :kind :gewgaw :name "Sky" :thing (:id blue))
            baby (db/tx :kind :gewgaw :name "Baby" :thing (:id blue))
            datoms (sut/retract-datoms :gewgaw/thing)]
        (should-contain [:db/retract (:id sky) :gewgaw/thing (:id blue)] datoms)
        (should-contain [:db/retract (:id baby) :gewgaw/thing (:id blue)] datoms)))

    (it "retract datoms []"
      (let [blue (db/tx :kind :bibelot :name "Bluey" :color "blue" :size 123)
            red (db/tx :kind :bibelot :name "Redy" :color "red" :size 456)
            rainbow (db/tx :kind :doodad :names ["Rain" "Bow"] :numbers [1 2 3] :things [(:id blue) (:id red)])
            aurora (db/tx :kind :doodad :names ["Aurora" "Borealis"] :numbers [4 5 6] :things [(:id blue) (:id red)])
            datoms (sut/retract-datoms :doodad/numbers)]
        (should-contain [:db/retract (:id rainbow) :doodad/numbers 1] datoms)
        (should-contain [:db/retract (:id rainbow) :doodad/numbers 2] datoms)
        (should-contain [:db/retract (:id rainbow) :doodad/numbers 3] datoms)
        (should-contain [:db/retract (:id aurora) :doodad/numbers 4] datoms)
        (should-contain [:db/retract (:id aurora) :doodad/numbers 5] datoms)
        (should-contain [:db/retract (:id aurora) :doodad/numbers 6] datoms)))

    (it "retract datoms [ref]"
      (let [blue (db/tx :kind :bibelot :name "Bluey" :color "blue" :size 123)
            red (db/tx :kind :bibelot :name "Redy" :color "red" :size 456)
            rainbow (db/tx :kind :doodad :names ["Rain" "Bow"] :numbers [1 2 3] :things [(:id blue) (:id red)])
            aurora (db/tx :kind :doodad :names ["Aurora" "Borealis"] :numbers [4 5 6] :things [(:id blue) (:id red)])
            datoms (sut/retract-datoms :doodad/things)]
        (should-contain [:db/retract (:id rainbow) :doodad/things (:id blue)] datoms)
        (should-contain [:db/retract (:id rainbow) :doodad/things (:id red)] datoms)
        (should-contain [:db/retract (:id aurora) :doodad/things (:id blue)] datoms)
        (should-contain [:db/retract (:id aurora) :doodad/things (:id red)] datoms))))

  )
