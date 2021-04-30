(defproject com.cleancoders.c3kit/scaffold "1.0.1"
  :description "Clean Coders Clojure (C3) Kit - Scaffold: Build tools."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [com.cleancoders.c3kit/apron "1.0.1"]
                 [com.taoensso/timbre "4.11.0-alpha1"]
                 [garden "1.3.10"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.764"]
                 [org.clojure/tools.namespace "1.0.0"]
                 ]

  :profiles {:dev {:resource-paths ["resources"]
                   :dependencies [[speclj "3.3.2"]]}}

  :plugins [[speclj "3.3.2"]]

  :source-paths ["src"]
  :test-paths ["spec"]
  :resource-paths []

  :aliases {"cljs" ["run" "-m" "c3kit.scaffold.cljs"]
            "css"  ["run" "-m" "c3kit.scaffold.css"]}
  )
