(ns c3kit.apron.schema-spec
  (:require
    [c3kit.apron.schema :as schema]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it xit should= should-contain should-not-contain
                                                      should-throw should-be-a should should-not should-not-throw]]
    [clojure.string :as str]
    [c3kit.apron.utilc :as utilc]
    [c3kit.apron.corec :as ccc]
    [c3kit.apron.schema :as s])
  #?(:clj
     (:import (java.net URI)
              (java.util UUID))))

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

(def now (new #?(:clj java.util.Date :cljs js/Date)))
(def home #?(:clj (URI/create "http://apron.co") :cljs "http://apron.co"))
(def a-uuid #?(:clj (UUID/fromString "1f50be30-1373-40b7-acce-5290b0478fbe") :cljs (uuid "1f50be30-1373-40b7-acce-5290b0478fbe")))

(def valid-pet {:species  "dog"
                :birthday now
                :length   2.5
                :teeth    24
                :name     "Fluffy"
                :owner    12345
                :color    ["brown" "white"]
                :uuid     a-uuid})

(describe "Schema"

  (context "coersion"

    (it "to boolean"
      (should= false (schema/->boolean nil))
      (should= false (schema/->boolean "false"))
      (should= false (schema/->boolean "FALSE"))
      (should= true (schema/->boolean "abc"))
      (should= true (schema/->boolean 1))
      (should= true (schema/->boolean 3.14)))

    (it "to string"
      (should= nil (schema/->string nil))
      (should= "abc" (schema/->string "abc"))
      (should= "1" (schema/->string 1))
      (should= "3.14" (schema/->string 3.14)))

    (it "to keyword"
      (should= nil (schema/->keyword nil))
      (should= :abc (schema/->keyword "abc"))
      (should= :abc (schema/->keyword ":abc"))
      (should= :abc/xyz (schema/->keyword "abc/xyz"))
      (should= :abc/xyz (schema/->keyword ":abc/xyz"))
      (should= :1 (schema/->keyword 1))
      (should= :3.14 (schema/->keyword 3.14))
      (should= :foo (schema/->keyword :foo)))

    (it "to float"
      (should= nil (schema/->float nil))
      (should= nil (schema/->int ""))
      (should= nil (schema/->int "\t"))
      (should= 1.0 (schema/->float 1))
      (should= 3.14 (schema/->float 3.14) 0.00001)
      (should= 3.14 (schema/->float "3.14") 0.00001)
      (should= 42.0 (schema/->float "42") 0.00001)
      (should= 3.14 (schema/->float 3.14M) 0.00001)
      (should-throw schema/stdex (schema/->float "fooey")))

    (it "to int"
      (should= nil (schema/->int nil))
      (should= nil (schema/->int ""))
      (should= nil (schema/->int "\t"))
      (should= 1 (schema/->int 1))
      (should= 3 (schema/->int 3.14))
      (should= 3 (schema/->int 3.9))
      (should= 42 (schema/->int "42"))
      (should= 3 (schema/->int "3.14"))
      (should= 3 (schema/->int 3.14M))
      (should-throw schema/stdex (schema/->int "fooey")))

    (it "to bigdec"
      (should= nil (schema/->bigdec nil))
      (should= nil (schema/->bigdec ""))
      (should= nil (schema/->bigdec "\t"))
      (should= 1M (schema/->bigdec 1))
      (should= 3.14M (schema/->bigdec 3.14))
      (should= 3.9M (schema/->bigdec 3.9))
      (should= 42M (schema/->bigdec "42"))
      (should= 3.14M (schema/->bigdec "3.14"))
      (should= 3.14M (schema/->bigdec 3.14M))
      (should-throw schema/stdex (schema/->bigdec "fooey")))

    (it "to date"
      (should= nil (schema/->date nil))
      (should= now (schema/->date now))
      (should= now (schema/->date (.getTime now)))
      (should-be-a #?(:clj java.util.Date :cljs js/Date) (schema/->date now))
      (should-throw schema/stdex (schema/->date "now"))
      (should= now (schema/->date (pr-str now))))

    (it "to sql date"
      (should= nil (schema/->sql-date nil))
      (should= #?(:clj (java.sql.Date. (.getTime now)) :cljs now) (schema/->sql-date now))
      (should= #?(:clj (java.sql.Date. (.getTime now)) :cljs now) (schema/->sql-date (.getTime now)))
      (should-be-a #?(:clj java.sql.Date :cljs js/Date) (schema/->sql-date now))
      (should-throw schema/stdex (schema/->sql-date "now"))
      (should= #?(:clj (java.sql.Date. (.getTime now)) :cljs now) (schema/->sql-date (pr-str now))))

    (it "to sql timestamp"
      (should= nil (schema/->timestamp nil))
      (should= #?(:clj (java.sql.Timestamp. (.getTime now)) :cljs now) (schema/->timestamp now))
      (should= #?(:clj (java.sql.Timestamp. (.getTime now)) :cljs now) (schema/->timestamp (.getTime now)))
      (should-be-a #?(:clj java.sql.Timestamp :cljs js/Date) (schema/->timestamp now))
      #?(:clj (should-be-a java.sql.Timestamp (schema/->timestamp (java.sql.Date. (.getTime now)))))
      (should-throw schema/stdex (schema/->timestamp "now"))
      (should= #?(:clj (java.sql.Timestamp. (.getTime now)) :cljs now) (schema/->timestamp (pr-str now))))

    (it "to uri"
      (should= nil (schema/->uri nil))
      (should= home (schema/->uri home))
      (should= home (schema/->uri "http://apron.co"))
      (should-throw schema/stdex (schema/->uri 123)))

    (it "to uuid"
      (should= nil (schema/->uuid nil))
      (should= a-uuid (schema/->uuid a-uuid))
      (should= a-uuid (schema/->uuid "1f50be30-1373-40b7-acce-5290b0478fbe"))
      (should= (schema/->uuid "53060bf1-971a-4d18-80fc-92a3112afd6e") (schema/->uuid #uuid "53060bf1-971a-4d18-80fc-92a3112afd6e"))
      (let [uuid2        (ccc/new-uuid)
            transit-uuid (utilc/<-transit (utilc/->transit uuid2))]
        (should= uuid2 (schema/->uuid transit-uuid)))
      (should-throw schema/stdex (schema/->uuid 123)))

    (it "to seq"
      (should= [] (schema/->seq nil))
      (should= ["foo"] (schema/->seq "foo"))
      (should= ["foo"] (schema/->seq ["foo"]))
      (should= ["foo" "bar"] (schema/->seq ["foo" "bar"])))

    (context "from spec"

      (it "with missing type"
        (should-throw schema/stdex "unhandled coersion type: nil" (schema/coerce-value {} 123)))

      (it "of boolean"
        (should= true (schema/coerce-value {:type :boolean} 123)))

      (it "of string"
        (should= "123" (schema/coerce-value {:type :string} 123)))

      (it "of int"
        (should= 123 (schema/coerce-value {:type :int} "123.4")))

      (it "of ref"
        (should= 123 (schema/coerce-value {:type :ref} "123.4")))

      (it "of float"
        (should= 123.4 (schema/coerce-value {:type :float} "123.4") 0.0001)
        (should= 123.4 (schema/coerce-value {:type :double} "123.4") 0.0001))

      (it "of bigdec"
        (should= 123.4M (schema/coerce-value {:type :bigdec} "123.4")))

      (it "with custom coercsions"
        (let [spec {:type :string :coerce [str/trim reverse #(apply str %)]}]
          (should= "321" (schema/coerce-value spec " 123\t"))))

      (it ", custom coersions happen before type coersion"
        (let [spec {:type :string :coerce #(* % %)}]
          (should= "16" (schema/coerce-value spec 4))))

      (it "of sequentials"
        (let [result (schema/coerce-value {:type [:float]} ["123.4" 321 3.1415])]
          (should= 123.4 (first result) 0.0001)
          (should= 321.0 (second result) 0.0001)
          (should= 3.1415 (last result) 0.0001)))

      (it "of sets (sequentials)"
        (let [result (schema/coerce-value {:type [:long]} #{"123" 321 3.14})]
          (should-contain 123 result)
          (should-contain 321 result)
          (should-contain 3 result)))

      (it "of sequentials with customs"
        (let [result (schema/coerce-value {:type [:float] :coerce inc} [321 3.1415])]
          (should= 322.0 (first result) 0.0001)
          (should= 4.1415 (last result) 0.0001)))

      (it "missing multiple type coercer"
        (should-throw schema/stdex "unhandled coersion type: :blah"
                      (schema/coerce-value {:type [:blah]} nil)))

      (it "of entity"
        (let [result (schema/coerce pet {:species  "dog"
                                         :birthday now
                                         :length   "2.3"
                                         :teeth    24.2
                                         :name     "Fluff"
                                         :owner    "12345"
                                         :uuid     a-uuid})]
          (should= false (schema/error? result))
          (should= "dog" (:species result))
          (should= now (:birthday result))
          (should= 2.3 (:length result) 0.001)
          (should= 24 (:teeth result))
          (should= "Fluffy" (:name result))
          (should= 12345 (:owner result))
          (should= a-uuid (:uuid result))))

      (it "of entity, nil values omitted"
        (let [result (schema/coerce pet {:name "Fido"})]
          (should= false (schema/error? result))
          (should= "Fidoy" (:name result))
          (should-not-contain :length result)
          (should-not-contain :species result)
          (should-not-contain :teeth result)
          (should-not-contain :birthday result)
          (should-not-contain :owner result)
          (should-not-contain :uuid result)))

      (it "of entity level coersions"
        (let [result (schema/coerce (assoc pet :* {:stage-name {:type   :string
                                                                :coerce #(str (:name %) " the " (:species %))}}) valid-pet)]
          (should= "Fluffyy the dog" (:stage-name result))))
      )
    )

  (context "validation"

    (it "of presence"
      (should= false (schema/present? nil))
      (should= false (schema/present? ""))
      (should= true (schema/present? 1))
      (should= true (schema/present? "abc")))

    (it "of email?"
      (should (schema/email? "micahmartin@gmail.com"))
      (should (schema/email? "micah@clenacoders.com"))
      (should (schema/email? "vikas.rao@rsa.rohde-schwarz.com"))
      (should-not (schema/email? "micah@clenacoders"))
      (should-not (schema/email? "micah")))

    (it "of enum"
      (let [is-temperament? (schema/is-enum? temperaments)]
        (should (is-temperament? nil))
        (should (is-temperament? :temperament/wild))
        (should (is-temperament? :temperament/domestic))
        (should-not (is-temperament? ":temperament/savage"))
        (should-not (is-temperament? :wild))
        (should-not (is-temperament? :temperament/savage))))

    (context "from spec"

      (it "with missing type"
        (should-throw schema/stdex "unhandled validation type: nil" (schema/validate-value! {} 123)))

      (it "of booleans"
        (should= true (schema/valid-value? {:type :boolean} true))
        (should= true (schema/valid-value? {:type :boolean} false))
        (should= false (schema/valid-value? {:type :boolean} 123)))

      (it "of strings"
        (should= true (schema/valid-value? {:type :string} "123"))
        (should= false (schema/valid-value? {:type :string} 123)))

      (it "of keywords"
        (should= true (schema/valid-value? {:type :keyword} :abc))
        (should= false (schema/valid-value? {:type :keyword} "abc"))
        (should= false (schema/valid-value? {:type :keyword} 123)))

      (it "of kw-ref"
        (should= true (schema/valid-value? {:type :kw-ref} :abc))
        (should= false (schema/valid-value? {:type :kw-ref} "abc"))
        (should= false (schema/valid-value? {:type :kw-ref} 123)))

      (it "of int"
        (should= true (schema/valid-value? {:type :int} 123))
        (should= false (schema/valid-value? {:type :int} 123.45))
        (should= true (schema/valid-value? {:type :long} 123))
        (should= false (schema/valid-value? {:type :long} 123.45)))

      (it "of ref"
        (should= true (schema/valid-value? {:type :ref} 123))
        (should= false (schema/valid-value? {:type :ref} 123.45)))

      (it "of float"
        (should= true (schema/valid-value? {:type :float} 123.456))
        #?(:clj (should= false (schema/valid-value? {:type :float} 123)))
        #?(:clj (should= false (schema/valid-value? {:type :float} 123M)))
        (should= false (schema/valid-value? {:type :float} "123"))
        (should= true (schema/valid-value? {:type :double} 123.456))
        #?(:clj (should= false (schema/valid-value? {:type :double} 123)))
        #?(:clj (should= false (schema/valid-value? {:type :double} 123M)))
        (should= false (schema/valid-value? {:type :double} "123")))

      (it "of bigdec"
        (should= true (schema/valid-value? {:type :bigdec} 123.456M))
        #?(:clj (should= false (schema/valid-value? {:type :bigdec} 123.456)))
        #?(:clj (should= false (schema/valid-value? {:type :bigdec} 123)))
        (should= false (schema/valid-value? {:type :bigdec} "123")))

      (it "of date/instant"
        (should= true (schema/valid-value? {:type :instant} nil))
        (should= false (schema/valid-value? {:type :instant} "foo"))
        (should= false (schema/valid-value? {:type :instant} 123))
        #?(:clj (should= true (schema/valid-value? {:type :instant} (java.util.Date.))))
        #?(:cljs (should= true (schema/valid-value? {:type :instant} (js/Date.))))
        #?(:cljs (should= false (schema/valid-value? {:type :instant} (js/goog.date.Date.)))))

      (it "of sql-date"
        (should= true (schema/valid-value? {:type :date} nil))
        (should= false (schema/valid-value? {:type :date} "foo"))
        (should= false (schema/valid-value? {:type :date} 123))
        #?(:clj (should= false (schema/valid-value? {:type :date} (java.util.Date.))))
        #?(:clj (should= true (schema/valid-value? {:type :date} (java.sql.Date. (System/currentTimeMillis)))))
        #?(:cljs (should= true (schema/valid-value? {:type :date} (js/Date.))))
        #?(:cljs (should= false (schema/valid-value? {:type :date} (js/goog.date.Date.)))))

      (it "of timestamp"
        (should= true (schema/valid-value? {:type :timestamp} nil))
        (should= false (schema/valid-value? {:type :timestamp} "foo"))
        (should= false (schema/valid-value? {:type :timestamp} 123))
        #?(:clj (should= false (schema/valid-value? {:type :timestamp} (java.util.Date.))))
        #?(:clj (should= true (schema/valid-value? {:type :timestamp} (java.sql.Timestamp. (System/currentTimeMillis)))))
        #?(:cljs (should= true (schema/valid-value? {:type :timestamp} (js/Date.))))
        #?(:cljs (should= false (schema/valid-value? {:type :timestamp} (js/goog.date.Date.)))))

      (it "of URI"
        (should= true (schema/valid-value? {:type :uri} nil))
        (should= #?(:clj false :cljs true) (schema/valid-value? {:type :uri} "foo"))
        #?(:clj (should= true (schema/valid-value? {:type :uri} (URI/create "foo"))))
        (should= false (schema/valid-value? {:type :uri} 123)))

      (it "of UUID"
        (should= true (schema/valid-value? {:type :uuid} nil))
        (should= false (schema/valid-value? {:type :uuid} "foo"))
        (should= true (schema/valid-value? {:type :uuid} a-uuid))
        (should= false (schema/valid-value? {:type :uuid} "1234"))
        (should= false (schema/valid-value? {:type :uuid} 123)))

      (it "of custom validation"
        (let [spec {:type :string :validate #(re-matches #"x+" %)}]
          (should= true (schema/valid-value? spec "xxx"))
          (should= false (schema/valid-value? spec "xox"))))

      (it "of multiple custom validations"
        (let [spec {:type :string :validate [#(not (nil? %)) #(<= 5 (count %))]}]
          (should= true (schema/valid-value? spec "abcdef"))
          (should= false (schema/valid-value? spec nil))))

      (it "allows nils, unless specified"
        (should= true (schema/valid-value? {:type :string} nil))
        (should= false (schema/valid-value? {:type :string :validate [schema/present?]} nil))
        (should= true (schema/valid-value? {:type :int} nil))
        (should= false (schema/valid-value? {:type :int :validate [schema/present?]} nil))
        (should= true (schema/valid-value? {:type :ref} nil))
        (should= false (schema/valid-value? {:type :ref :validate [schema/present?]} nil))
        (should= true (schema/valid-value? {:type :float} nil))
        (should= false (schema/valid-value? {:type :float :validate [schema/present?]} nil))
        (should= true (schema/valid-value? {:type :instant} nil))
        (should= false (schema/valid-value? {:type :instant :validate [schema/present?]} nil)))

      (it "of sequentials"
        (should= true (schema/valid-value? {:type [:float]} [32.1 3.1415]))
        (should= false (schema/valid-value? {:type [:float]} 3.1415))
        (should= false (schema/valid-value? {:type [:float]} ["3.1415"]))
        (should= true (schema/valid-value? {:type [:float]} nil)))

      (it "of sequentials with customs"
        (should= true (schema/valid-value? {:type [:float] :validate pos?} [32.1 3.1415]))
        (should= false (schema/valid-value? {:type [:float] :validate pos?} [32.1 -3.1415])))

      (it "missing multiple type coercer"
        (should-throw schema/stdex "unhandled validation type: :blah"
                      (schema/validate-value! {:type [:blah]} nil)))

      (it "of invalid entity"
        (let [result (schema/validate pet {:species  321
                                           :birthday "yesterday"
                                           :length   "foo"
                                           :teeth    1000
                                           :name     ""
                                           :owner    nil})
              errors (:errors result)]
          (should= true (schema/error? result))
          (should= "invalid" (schema/exmessage (:species errors)))
          (should= "invalid" (schema/exmessage (:birthday errors)))
          (should= "invalid" (schema/exmessage (:length errors)))
          (should= "invalid" (schema/exmessage (:teeth errors)))
          (should= "invalid" (schema/exmessage (:name errors)))
          (should= "invalid" (schema/exmessage (:owner errors)))))

      (it "of valid entity"
        (let [result (schema/validate pet valid-pet)]
          (should= false (schema/error? result))))

      (it "of entity with missing(required) fields"
        (let [result (schema/validate pet {})]
          (should= true (schema/error? result))
          (should-contain :owner (:errors result))
          (should-not-contain :birthday (:errors result))))

      (it "of entity level validations"
        (let [spec    (assoc pet :* {:species {:validate #(not (and (= "snake" (:species %))
                                                                    (= "Fluffy" (:name %))))
                                               :message  "Snakes are not fluffy!"}})
              result1 (schema/validate spec valid-pet)
              result2 (schema/validate spec (assoc valid-pet :name "Fluffy" :species "snake"))]
          (should= false (schema/error? result1))
          (should= true (schema/error? result2))
          (should= "Snakes are not fluffy!" (:species (schema/error-message-map result2)))))

      (it ":validations validations/message pairs"
        (let [spec    (merge-with merge pet
                                  {:species {:validate    nil
                                             :validations [{:validate nil? :message "species not nil"}]}
                                   :name    {:validate    nil
                                             :validations [{:validate [s/present? #(= "blah" %)] :message "bad name"}]}})
              result1 (schema/validate spec (assoc valid-pet :species nil :name "blah"))
              result2 (schema/validate spec (assoc valid-pet :name "Fluffy" :species "snake"))]
          (should= false (schema/error? result1))
          (should= true (schema/error? result2))
          (should= "species not nil" (:species (schema/error-message-map result2)))
          (should= "bad name" (:name (schema/error-message-map result2)))))

      (it "validations stop on first failure"
        (let [spec    (merge-with merge pet
                                  {:species {:validate #(str/starts-with? % "s")
                                             :message  "not s species"
                                             :validations
                                             [{:validate #(str/ends-with? % "e") :message "not *e species"}
                                              {:validate #(= "snake" %) :message "not snake"}]}})
              result1 (schema/validate spec (assoc valid-pet :species "snake"))
              result2 (schema/validate spec (assoc valid-pet :species "swine"))
              result3 (schema/validate spec (assoc valid-pet :species "snail"))
              result4 (schema/validate spec (assoc valid-pet :species "crab"))]
          (should= false (schema/error? result1))
          (should= true (schema/error? result2))
          (should= "not snake" (:species (schema/error-message-map result2)))
          (should= true (schema/error? result3))
          (should= "not *e species" (:species (schema/error-message-map result3)))
          (should= true (schema/error? result4))
          (should= "not s species" (:species (schema/error-message-map result4)))))

      (it ":validation at entity level"
        (let [spec    (assoc pet :* {:species {:validations [{:validate #(not (and (= "snake" (:species %))
                                                                                   (= "Fluffy" (:name %))))
                                                              :message  "Snakes are not fluffy!"}]}})
              result1 (schema/validate spec valid-pet)
              result2 (schema/validate spec (assoc valid-pet :name "Fluffy" :species "snake"))]
          (should= false (schema/error? result1))
          (should= true (schema/error? result2))
          (should= "Snakes are not fluffy!" (:species (schema/error-message-map result2)))))
      )
    )

  (context "coersion"

    (it "multi field with nil value"
      (should= nil (schema/coerce-value {:type [:int]} nil)))

    (it "multi field with empty list"
      (should= () (schema/coerce-value {:type [:int]} ())))

    (it "entity - with an empty seq value"
      (let [result (schema/coerce pet {:colors []})]
        (should= [] (:colors result))))

    )

  (context "conforming"

    (it "with failed coersion"
      (should-throw schema/stdex "coersion failed"
                    (schema/conform-value {:type :int :message "oh no!"} "foo")))

    (it "with failed validation"
      (should-throw schema/stdex "invalid"
                    (schema/conform-value {:type :int :validate even? :message "oh no!"} "123")))

    (it "of int the must be present"
      (should-throw schema/stdex "invalid"
                    (schema/conform-value {:type :int :validate [schema/present?]} ""))
      (should-throw schema/stdex "invalid"
                    (schema/conform-value {:type :long :validate schema/present?} "")))

    (it "success"
      (should= 123 (schema/conform-value {:type :int :message "oh no!"} "123")))

    (it "of sequentials"
      (should= [123 321 3] (schema/conform-value {:type [:int]} ["123.4" 321 3.1415])))

    (it "of sequentials - empty"
      (should= [] (schema/conform-value {:type [:int]} [])))

    (it "a valid entity"
      (let [result (schema/conform pet {:species  "dog"
                                        :birthday now
                                        :length   "2.3"
                                        :teeth    24.2
                                        :name     "Fluff"
                                        :owner    "12345"})]
        (should= false (schema/error? result))
        (should= "dog" (:species result))
        (should= now (:birthday result))
        (should= 2.3 (:length result) 0.001)
        (should= 24 (:teeth result))
        (should= "Fluffy" (:name result))
        (should= 12345 (:owner result))))

    (it "entity - with an empty seq value"
      (let [result (schema/conform pet {:species  "dog"
                                        :birthday now
                                        :length   "2.3"
                                        :teeth    24.2
                                        :name     "Fluff"
                                        :owner    "12345"
                                        :colors   []})]
        (should= false (schema/error? result))
        (should= [] (:colors result))))

    (it "of entity level operations"
      (let [spec    (assoc pet :* {:species {:type     :ignore
                                             :coerce   (constantly "snake")
                                             :validate #(not (and (= "snake" (:species %))
                                                                  (= "Fluffyy" (:name %))))
                                             :message  "Snakes are not fluffy!"}})
            result1 (schema/conform spec (assoc valid-pet :name "Slimey"))
            result2 (schema/conform spec valid-pet)]
        (should= false (schema/error? result1))
        (should= "snake" (:species result1))
        (should= true (schema/error? result2))
        (should= "Snakes are not fluffy!" (:species (schema/error-message-map result2)))))

    (it "a invalid entity"
      (let [result (schema/conform pet {:species  321
                                        :birthday "yesterday"
                                        :length   "foo"
                                        :teeth    1000
                                        :name     ""
                                        :owner    nil})
            errors (:errors result)]
        (should= true (schema/error? result))
        (should= "invalid" (schema/exmessage (:species errors)))
        (should= "coersion failed" (schema/exmessage (:birthday errors)))
        (should= "coersion failed" (schema/exmessage (:length errors)))
        (should= "invalid" (schema/exmessage (:teeth errors)))
        (should= "invalid" (schema/exmessage (:name errors)))
        (should= "invalid" (schema/exmessage (:owner errors)))))

    (it "removed extra fields"
      (let [crufty (assoc valid-pet :garbage "yuk!")
            result (schema/conform pet crufty)]
        (should= nil (:garbage result))
        (should-not-contain :garbage result)))

    (it ":validations errors"
      (let [spec    (merge-with merge pet
                                {:species {:validate    nil
                                           :validations [{:validate nil? :message "species not nil"}]}
                                 :name    {:validate    nil
                                           :coerce      nil
                                           :validations [{:validate [s/present? #(= "blah" %)] :message "bad name"}]}})
            result1 (schema/conform spec (assoc valid-pet :species nil :name "blah"))
            result2 (schema/conform spec (assoc valid-pet :name "Fluffy" :species "snake"))]
        (should= false (schema/error? result1))
        (should= true (schema/error? result2))
        (should= "species not nil" (:species (schema/error-message-map result2)))
        (should= "bad name" (:name (schema/error-message-map result2)))))
    )

  (context "error messages"

    (it "are nil when there are none"
      (should= nil (schema/error-message-map (schema/make-error {} pet nil nil))))

    (it "are only given for failed results"
      (should= {:name "must be nice and unique name"}
               (-> {:name (ex-info "blah" {:message "must be nice and unique name"})}
                   (schema/make-error pet nil nil)
                   (schema/error-message-map))))

    (it "with missing message"
      (should= {:foo "is invalid"}
               (-> {:foo (ex-info "blah" {})}
                   (schema/make-error pet nil nil)
                   (schema/error-message-map))))

    )

  (context "presentation"

    (it "of int"
      (should= 123 (schema/present-value {:type :int} 123))
      (should= 123 (schema/present-value {:type :long} 123)))

    (it "of float"
      (should= 12.34 (schema/present-value {:type :float} 12.34))
      (should= 12.34 (schema/present-value {:type :double} 12.34)))

    (it "of string"
      (should= "foo" (schema/present-value {:type :string} "foo")))

    (it "of date"
      (should= now (schema/present-value {:type :instant} now)))

    (it "applies custom presenter"
      (should= 124 (schema/present-value {:type :long :present inc} 123)))

    (it "ommited"
      (should= nil (schema/present-value {:type :long :present schema/omit} 123)))

    (it "applies multiple custom presenters"
      (should= 62 (schema/present-value {:type :long :present [inc #(/ % 2)]} 123)))

    (it "of sequentials"
      (should= [123 456] (schema/present-value {:type [:int]} [123 456])))

    (it "of sequentials - empty"
      (should= [] (schema/present-value {:type [:int]} [])))

    (it "of sequentials - nil"
      (should= nil (schema/present-value {:type [:int]} nil)))

    (it "of sequentials with customs"
      (should= ["123" "456"] (schema/present-value {:type [:int] :present str} [123 456]))
      (should= ["2" "3" "4" "5"] (schema/present-value {:type [:float] :present [inc str]} [1 2 3 4])))

    (it "of sequentials when omitted"
      (should= [] (schema/present-value {:type [:int] :present schema/omit} [123 456])))

    (context "of entity"

      (it "doesn't present omitted (nil) results"
        (let [schema (assoc-in pet [:owner :present] schema/omit)
              result (schema/present schema (assoc valid-pet :owner "George"))]
          (should-not-contain :id result)
          (should-not-contain :owner result))
        )

      (it "with entity level presentation"
        (let [result (schema/present (assoc pet :* {:stage-name {:present #(str (:name %) " the " (:species %))}}) valid-pet)]
          (should= "Fluffy the dog" (:stage-name result))))

      (it "with error on entity level presentation"
        (let [result (schema/present (assoc pet :* {:stage-name {:present #(throw (ex-info "blah" {:x %}))}}) valid-pet)]
          (should= true (schema/error? result))
          (should-contain :stage-name (:errors result))))

      (it "with error on entity level presentation!"
        (should-throw schema/stdex
                      (schema/present!
                        (assoc pet :* {:stage-name {:present #(throw (ex-info "blah" {:x %}))}}) valid-pet)))
      )
    )

  (context "kind"

    (it "is enforced on validate!"
      (let [result (schema/validate pet (assoc valid-pet :kind :beast))]
        (should= true (schema/error? result))
        (should= ["kind mismatch; must be :pet"] (schema/messages result))))

    (it "can be left out"
      (should= false (schema/error? (schema/validate pet (dissoc valid-pet :kind)))))

    (it "will be added if missing by conform"
      (let [result (schema/conform pet (dissoc valid-pet :kind))]
        (should= false (schema/error? result))
        (should= :pet (:kind result))))

    )
  )
