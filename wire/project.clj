(defproject c3kit/wire "0.1.0-SNAPSHOT"

  :description "Clean Coders Clojure (C3) Kit - Wire: API for AJAX and WebSocket."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [c3kit/apron "0.1.0-SNAPSHOT"]
                 [c3kit/scaffold "0.1.0-SNAPSHOT"]
                 [cljs-http/cljs-http "0.1.46"]
                 [http-kit "2.5.1"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/core.async "1.3.610"]
                 [reagent "1.0.0-alpha2"]
                 [ring/ring "1.8.1"]
                 ]

  :profiles {:dev {:resource-paths ["resources"]
                   :dependencies [[speclj "3.3.2"]]}}

  :plugins [[speclj "3.3.2"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["spec/clj" "spec/cljc" "spec/cljs"]
  :resource-paths ["resources"]


  :aliases {"cljs" ["run" "-m" "c3kit.scaffold.cljs"]}
  )
