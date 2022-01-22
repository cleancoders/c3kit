(defproject com.cleancoders.c3kit/wire "1.0.17"

  :description "Clean Coders Clojure (C3) Kit - Wire: Rich-client webapp tools."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [com.cleancoders.c3kit/apron "1.0.5"]
                 [com.cleancoders.c3kit/scaffold "1.0.6"]
                 [cljs-http/cljs-http "0.1.46"]
                 [http-kit "2.5.1"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/core.async "1.3.610"]
                 [reagent "1.1.0"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [ring/ring "1.8.1"]
                 ]

  :profiles {:dev {:resource-paths ["dev"]
                   :dependencies [[speclj "3.3.2"]]}}

  :plugins [[speclj "3.3.2"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["spec/clj" "spec/cljc" "spec/cljs"]
  :resource-paths []

  :aliases {"cljs" ["run" "-m" "c3kit.scaffold.cljs"]}
  )
