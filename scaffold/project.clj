(defproject c3kit/scaffold "0.1.0-SNAPSHOT"
  :description "Clean Coders Clojure (C3) Kit - Scaffold: Build tools."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [c3kit/apron "0.1.0-SNAPSHOT"]
                 [com.taoensso/timbre "4.11.0-alpha1"]
                 [garden "1.3.10"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.764"]
                 [org.clojure/tools.namespace "1.0.0"]
                 ]

  :profiles {:dev {:dependencies [[speclj "3.3.2"]]}}

  :plugins [[speclj "3.3.2"]]

  :source-paths ["src"]
  :test-paths ["spec"]
  :resource-paths ["resources"]

  :aliases {"cljs" ["run" "-m" "c3kit.scaffold.cljs"]
            "css"  ["run" "-m" "c3kit.scaffold.css"]}
  )
