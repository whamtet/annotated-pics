(ns tap-chinese.core
  (:require
    [clojure.browser.repl :as repl]
    [alandipert.storage-atom :refer [local-storage]]
    [reagent.core :as reagent :refer [atom]]
    [clojure.set :as set]
    [cljs.reader :refer [read-string]]
    [redlobster.promise :as p])
  (:use-macros
    [redlobster.macros :only [promise let-realised]]))

(enable-console-print!)

(println "Hello world!")

(def pics (atom (sorted-set)))

(def current-chapter (local-storage (atom "default") :current-chapter))
(def chapters (local-storage (atom (sorted-set @current-chapter)) :chapters))
(def new-chapter (atom ""))
(def annotations (local-storage (atom {}) :annotations))
(def reference (local-storage (atom {}) :reference))

(defn dissoc-in [m v]
  (if (empty? v)
    {}
    (let [
           root (dissoc (get-in m (pop v)) (peek v))
           ]
      (if (empty? root)
        (dissoc-in m (pop v))
        (assoc-in m (pop v) root)))))

(defn chapter-div []
  [:div
   [:div
    [:select
     {:value @current-chapter
      :on-change #(reset! current-chapter (-> % .-target .-value))
      }
     (for [chapter @chapters]
       ^{:key chapter}
       [:option {:value chapter} chapter])]
    " "
    [:input {:type "text"
             :value @new-chapter
             :on-change #(reset! new-chapter (-> % .-target .-value))}]
    [:input {:type "button"
             :value "Create"
             :on-click #(when (not= "" (.trim @new-chapter))
                          (swap! chapters conj @new-chapter)
                          (reset! current-chapter @new-chapter)
                          (reset! new-chapter ""))}]
    [:input {:type "button"
             :value "Delete"
             :on-click delete-chapter}]
    ]])

(defn delete-chapter []
  (when (and (> (count @chapters) 1)
             (js/confirm (str "Delete " @current-chapter "?")))
    (let-realised [chapter-files (chapter-fs @current-chapter)]
                  (dorun (map #(.remove % (fn [])) @chapter-files))
                  (swap! chapters disj @current-chapter)
                  (reset! current-chapter (first @chapters)))))

(defn valid-pic? [s] (.endsWith s ".jpg"))


;fs.root.getFile('log.txt', {create: true, exclusive: true}, function(fileEntry) {
(def get-file (js/Function. "f" "opts" "cb" "e" "fs.root.getFile(f, opts, cb, e)"))
(def request-quota (js/Function. "size" "f" "return navigator.webkitPersistentStorage.requestQuota(size, f)"))

(defn files-added [e]
  (let [files (-> e .-target .-files array-seq)]
    (dorun (map save-file files))))

(defn save-file [file]
  (when (valid-pic? (.-name file))
    (get-file
      (str @current-chapter (.-name file))
      #js {:create true}
      (fn [file-entry]
        (println "got file")
        (.createWriter file-entry
                       (fn [writer]
                         (set! (.-onerror writer) error-handler)
                         (set! (.-onwriteend writer) #(do
                                                        (println "written" (.toURL file-entry))
                                                        (swap! pics conj (.toURL file-entry))))
                         (let [fr (js/FileReader.)]
                           (set! (.-onerror fr) error-handler)
                           (set! (.-onloadend fr)
                                 (fn []
                                   (.write writer (js/Blob. #js [(.-result fr)]))))
                           (.readAsArrayBuffer fr file)))))
      error-handler)))

(defn read-reference [s]
  (into {}
        (for [line (.split s "\n")
              :when (not-empty (.trim line))
              ]
          (let [[k & vs] (.split line " ")]
            [(int k) (apply str (interpose " " vs))]))))

(defn reference-added [e]
  (let [
         file (-> e .-target .-files array-seq first)
         fr (js/FileReader.)
         ]
    (set! (.-onerror fr) error-handler)
    (set! (.-onloadend fr)
          (fn []
            (swap! reference assoc @current-chapter (read-reference (.-result fr)))))
    (.readAsText fr file)))

(defn bullets [src]
  [:div
   (for [[[position-x position-y] note] (get-in @annotations [@current-chapter src])]
     ^{:key (str position-x position-y note)}
     [:img {:style {:position "absolute" :left position-x :top position-y :width 32 :height 32}
            :on-click #(js/alert note)
            :src "cross.png"
            }
      ])])

(defn cancellation [src]
  [:div
   [:input {:type "button"
            :value "X"
            :on-click (fn []
                        (when (js/confirm "Delete Pic?")
                          (swap! pics disj src)
                          (js/webkitResolveLocalFileSystemURL src #(.remove % (fn [])))))
            }] " "
   (for [[[position-x position-y] note] (get-in @annotations [@current-chapter src])]
     ^{:key (str position-x position-y note)}
     [:input {:type "button"
              :value note
              :on-click #(swap! annotations dissoc-in [@current-chapter src [position-x position-y]])}])])

(defn img [src]
  (let [
         file-name (js/decodeURIComponent (last (.split src "/")))
         pattern (re-pattern (str @current-chapter ".*jpg"))
         ]
    (if (re-find pattern file-name)
      [:div
       [:div
        [cancellation src]
        ]
       [:div {:style {
                       :position "relative"
                       }}
        [:div {:style {:height 1000}}
         [:img {:src src
                :style {:width 1000
                        :-webkit-transform "translateX(-20%) rotate(90deg) translateX(100%)"
                        :-webkit-transform-origin "top right"
                        }
                :on-click (fn [e] ;192, 8
                            (let [
                                   target (js/$ (.-target e))
                                   picture-position (.offset target)
                                   picture-x (- (.-pageX e) (.-left picture-position) -58)
                                   picture-y (- (.-pageY e) (.-top picture-position) )
                                   note (js/prompt "Note")
                                   note2 (if (pos? (int note))
                                          (get-in @reference [@current-chapter (int note)])
                                          note)
                                   ]
                              (when (and (pos? (int note)) (not note2))
                                (js/alert "No reference loaded"))
                              (when note2
                                (swap! annotations assoc-in [@current-chapter src [picture-x picture-y]] note2))))
                }]]
        ;BulletFeatUnique.png
        [bullets src]
        ]
       ]
      [:div (println "failed" src)])))

(defn body []
  [:div
   "Picture "
   [:input {:type "file"
            :multiple true
            :on-change files-added
            }] [:br][:br]
   "Reference "
   [:input {:type "file"
            :on-change reference-added}] [:br][:br]
   [chapter-div] [:br][:br]
   (map-indexed
     (fn [i src]
       (with-meta
         [img src]
         {:key i})) @pics)
   ])

(defn list-fs []
  (promise
    (let [
           dir-reader (-> js/fs .-root .createReader)
           all-results (atom ())
           ]
      ((fn f []
         (.readEntries dir-reader
                       (fn [results]
                         (if (not-empty results)
                           (do
                             (swap! all-results concat (array-seq results))
                             (f))
                           (realise @all-results)))))))))

(defn chapter-fs [chapter]
  (let [
         pattern (re-pattern (str chapter ".*jpg"))
         ]
    (let-realised [files (list-fs)]
                  (filter #(re-find pattern (.-name %)) @files))))

(defn fs-ready []
  (let-realised [files (list-fs)]
                (swap! pics set/union (apply sorted-set (filter valid-pic? (map #(.toURL %) @files))))))

;render the bitch!
(reagent/render-component [body]
                          (js/document.getElementById "container"))

(request-quota 100000000
               (fn [bytes-granted]
                 (js/window.webkitRequestFileSystem
                   js/window.PERSISTENT
                   bytes-granted
                   #(do
                      (set! js/fs %)
                      (fs-ready)
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
