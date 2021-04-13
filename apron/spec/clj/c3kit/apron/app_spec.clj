(ns c3kit.apron.app-spec
  (:require
    [c3kit.apron.app :as app]
    [speclj.core :refer :all]
    ))

(def foo "Foo")
(defonce bar (app/resolution :bar))

(describe "App"

  (it "resolve-var"
    (should= "Foo" (deref (app/resolve-var 'c3kit.apron.app-spec/foo))))

  (it "resolution - missing"
    (alter-var-root #'app/app dissoc :bar)
    (should-throw Exception "Unresolved app component: :bar" @bar))

  (it "resolution - found"
    (alter-var-root #'app/app assoc :bar "bar")
    (should= "bar" @bar))

  (it "find-env"
    (let [token (str (rand))]
      (should= "development" (app/find-env (str "c3.app.spec." token) (str "C3_APP_SPEC_" token))))

    (System/setProperty "c3.app.spec" "test")
    (should= "test" (app/find-env "c3.app.spec" "C3_APP_SPEC")))

  (it "env"
    (app/set-env! "env-test")
    (should= "env-test" @app/env))

)
