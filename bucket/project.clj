(defproject c3kit/bucket "0.1.0-SNAPSHOT"

  :description "Clean Coders Clojure (C3) Kit - Bucket: Database API for datomic and in-memory."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [c3kit/apron "0.1.0-SNAPSHOT"]
                 [com.datomic/datomic-free "0.9.5697" :exclusions [com.google.guava/guava]]
                 [jstrutz/hashids "1.0.1"]
                 [org.clojure/clojure "1.10.3"]
                 ]

  :profiles {:dev {:resource-paths ["resources" "dev"]
                   :dependencies [
                                  [c3kit/scaffold "0.1.0-SNAPSHOT"]
                                  [cljsjs/hashids "1.0.2-0"]
                                  [speclj "3.3.2"]
                                  ]}}

  :plugins [[speclj "3.3.2"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["spec/clj" "spec/cljc" "spec/cljs"]
  :resource-paths ["resources"]

  :aliases {"cljs" ["run" "-m" "c3kit.scaffold.cljs"]}
  )
