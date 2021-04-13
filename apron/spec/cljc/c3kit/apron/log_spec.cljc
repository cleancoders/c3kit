(ns c3kit.apron.log-spec
  (:require
    [c3kit.apron.log :as log]
    [speclj.core #?(:clj :refer :cljs :refer-macros) [context describe it xit should= should-contain
                                                      should-not-contain should-throw should-be-a with
                                                      should-not=]]
    ))

(describe "Log"

  ;(it "includes stacktrace"
  ;  (log/report (Exception. "for bar")))
  )

