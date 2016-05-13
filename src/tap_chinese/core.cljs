(ns tap-chinese.core
  (:require
    [clojure.browser.repl :as repl]
    [alandipert.storage-atom :refer [local-storage]]
    [reagent.core :as reagent :refer [atom]]
    ))

(def data (local-storage (cljs.core/atom {}) :data))
(def pics (atom []))

(defn append-pic [file]
  (let [fr (js/FileReader.)]
    (set! (.-onload fr) (fn [e]
                          (-> e .-target .-result js/console.log)
                          (swap! pics conj (-> e .-target .-result))))
    (.readAsDataURL fr file)))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)

(println "Hello world!")

;fs.root.getFile('log.txt', {create: true, exclusive: true}, function(fileEntry) {
(def get-file (js/Function. "f" "opts" "cb" "e" "return tap_chinese.core.fs.root.getFile(f, opts, cb, e)"))
(def request-quota (js/Function. "size" "f" "return navigator.webkitPersistentStorage.requestQuota(size, f)"))

(defn files-added [e]
  #_(get-file "log.txt" #js {}
            (fn [file-entry]
              (set! js/a file-entry))
            error-handler
            )
  (set! js/a (-> e .-target .-files array-seq first append-pic)))

(defn body []
  [:div
   [:input {:type "file"
            :multiple true
            :on-change files-added
            }]
   (str (count @pics))
   ])

;render the bitch!
(reagent/render-component [body]
                          (js/document.getElementById "container"))

(request-quota 100000000
               (fn [bytes-granted]
                 (js/window.webkitRequestFileSystem
                   js/window.PERSISTENT
                   100000000
                   #(do
                      (def fs %)
                      ))))

(def error-handler (js/Function. "e"
                                 "
                                 var msg = '';

                                 switch (e.code) {
                                 case FileError.QUOTA_EXCEEDED_ERR:
                                 msg = 'QUOTA_EXCEEDED_ERR';
                                 break;
                                 case FileError.NOT_FOUND_ERR:
                                 msg = 'NOT_FOUND_ERR';
                                 break;
                                 case FileError.SECURITY_ERR:
                                 msg = 'SECURITY_ERR';
                                 break;
                                 case FileError.INVALID_MODIFICATION_ERR:
                                 msg = 'INVALID_MODIFICATION_ERR';
                                 break;
                                 case FileError.INVALID_STATE_ERR:
                                 msg = 'INVALID_STATE_ERR';
                                 break;
                                 default:
                                 msg = 'Unknown Error';
                                 break;
                                 };

                                 console.log('Error: ' + msg);"))
