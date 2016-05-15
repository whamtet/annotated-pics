(require '[cljs.build.api :as b])

(b/watch "src"
         {:output-to "release/tap_chinese.js"
          :output-dir "release"
          :optimizations :advanced
          :verbose true})
