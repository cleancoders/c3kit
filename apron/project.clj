(defproject com.cleancoders.c3kit/apron "1.0.0"
  :description "Clean Coders Clojure (C3) Kit - Apron: The most essential library.  Put your tool apron on before getting to work."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [org.clojure/clojure "1.10.3"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.cognitect/transit-cljs "0.8.264"]
                 [com.taoensso/timbre "4.11.0-alpha1"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 ]

  :profiles {:dev {:resource-paths ["dev"]
                   :dependencies [
                                  [org.clojure/clojurescript "1.10.764"]
                                  [speclj "3.3.2"]
                                  ]}}

  :plugins [[speclj "3.3.2"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["spec/clj" "spec/cljc" "spec/cljs"]
  :resource-paths []

  :aliases {"cljs" ["run" "-m" "c3kit.apron.cljs"]}
  )
