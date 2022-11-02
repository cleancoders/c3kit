(defproject com.cleancoders.c3kit/apron "1.0.10"
  :description "Clean Coders Clojure (C3) Kit - Apron: c3kit essentials. Where is thy leather apron and thy rule?."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.cognitect/transit-clj "1.0.329"]
                 [com.cognitect/transit-cljs "0.8.269"]
                 [com.taoensso/timbre "5.2.1"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.namespace "1.3.0"]
                 ]

  :profiles {:dev {:resource-paths ["dev"]
                   :dependencies [
                                  [org.clojure/clojurescript "1.10.764"]
                                  [speclj "3.4.1"]
                                  ]}}

  :plugins [[speclj "3.4.1"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["spec/clj" "spec/cljc" "spec/cljs"]
  :resource-paths []

  :aliases {"cljs" ["run" "-m" "c3kit.apron.cljs"]}
  )
