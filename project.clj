(defproject tap-chinese "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [reagent "0.6.0-alpha2"]
                 [alandipert/storage-atom "1.2.4"]
;                 [frankiesardo/linked "1.2.6"]
                 [redlobster "0.2.2-SNAPSHOT"]
                 ]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :plugins [[lein-npm "0.6.1"]]
  :npm {:dependencies [[source-map-support "0.4.0"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :target-path "target")
