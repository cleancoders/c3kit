(defproject com.cleancoders.c3kit/scaffold "1.0.11"
  :description "Clean Coders Clojure (C3) Kit - Scaffold: Build tools."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [com.cleancoders.c3kit/apron "1.0.10"]
                 [garden "1.3.10"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.10.764" :exclusions [org.clojure/tools.reader com.google.code.findbugs/jsr305]]
                 [org.clojure/tools.namespace "1.2.0"]
                 ]

  :profiles {:dev {:resource-paths ["resources"]
                   :dependencies   [[speclj "3.4.1"]]}}

  :plugins [[speclj "3.4.1"]]

  :source-paths ["src"]
  :test-paths ["spec"]
  :resource-paths []

  :aliases {"cljs" ["run" "-m" "c3kit.scaffold.cljs"]
            "css"  ["run" "-m" "c3kit.scaffold.css"]}
  )
