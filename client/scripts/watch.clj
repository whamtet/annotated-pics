(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'tap-chinese.core
   :output-to "out/tap_chinese.js"
   :output-dir "out"})
