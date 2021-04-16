(ns c3kit.apron.legend-spec
  (:require
    [c3kit.apron.schema :as schema]
    [c3kit.apron.legend :as legend]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it xit should= should-contain
                                                      should-not-contain should-throw should-not-be-nil with]]))



(describe "Schema Legend"

  ;(it "presents an entity contained"
  ;  (let [bob {:kind :author :name "Bob"}
  ;        presentation (legend/present! bob)]
  ;    (should= (schema/present! episode.schema/author bob) presentation)))
  ;
  ;(it "complains when the entity kind is not in the legend"
  ;  (should-throw (legend/present! {:kind :unknown})))
  ;
  ;(it "coerces an entity contained"
  ;  (let [bob {:kind :author :name "Bob"}
  ;        coersion (legend/coerce! bob)]
  ;    (should= (schema/coerce! episode.schema/author bob) coersion)))
  ;
  ;(it "confirms an entity contained"
  ;  (let [bob {:kind :author :name "Bob"}
  ;        conformation (legend/conform! bob)]
  ;    (should= (schema/conform! episode.schema/author bob) conformation)))

  )
