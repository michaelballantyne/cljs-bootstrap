(ns cljs-bootstrap.core
  (:require-macros [cljs.env.macros :refer [ensure]]
                   [cljs.analyzer.macros :refer [no-warn]])
  (:require [cljs.pprint :refer [pprint]]
            [cljs.tagged-literals :as tags]
            [cljs.tools.reader :as r]
            [cljs.tools.reader.reader-types :refer [string-push-back-reader]]
            [cljs.analyzer :as ana]
            [cljs.compiler :as c]
            [cljs.env :as env]))

(set! *target* "nodejs")
(apply load-file ["./.cljs_node_repl/cljs/core$macros.js"])

(def cenv (env/default-compiler-env))

(comment
  ;; NOTE: pprint'ing the AST seems to fail

  ;; works
  (js/eval
    (with-out-str
      (c/emit
        (ensure
          (ana/analyze-keyword
            (assoc (ana/empty-env) :context :expr)
            :foo)))))

  ;; works
  (js/eval
    (with-out-str
      (c/emit
        (ensure
          (ana/analyze
            (assoc (ana/empty-env) :context :expr)
            '(+ 1 2))))))

  ;; works
  (ensure
    (ana/get-expander
      (first '(first [1 2 3]))
      (assoc (ana/empty-env) :context :expr)))

  ;; works
  (let [form  '(second [1 2 3])
        mform (ensure
                (ana/macroexpand-1
                  (assoc (ana/empty-env) :context :expr) form))]
    (identical? form mform))

  ;; get the expected error if we use quote instead of syntax
  ;; quote since cljs.core not yet analyzed
  (ensure
    (ana/parse-invoke
      (assoc (ana/empty-env) :context :expr) `(second [1 2 3])))

  ;; works
  (ensure
    (ana/analyze-seq
      (assoc (ana/empty-env) :context :expr)
      '(first [1 2 3]) nil nil))

  ;; works
  ;; includes warning if not suppressed via no-warn
  (js/eval
    (with-out-str
      (ensure
        (c/emit
          (no-warn
            (ana/analyze-seq
              (assoc (ana/empty-env) :context :expr)
              `(first [1 2 3]) nil nil))))))

  ;; works, same as above
  (js/eval
    (with-out-str
      (ensure
        (c/emit
          (no-warn
            (ana/analyze
              (assoc (ana/empty-env) :context :expr)
              `(first [1 2 3])))))))

  ;; works
  (js/eval
    (with-out-str
      (ensure
        (c/emit
          (no-warn
            (ana/analyze
              (assoc (ana/empty-env) :context :expr)
              `((fn [a# b#] (+ a# b#)) 1 2)))))))

  (def fs (js/require "fs"))

  (def ana (.readFileSync fs "resources/cljs/core.cljs" "utf8"))

  (goog/isString ana)

  ;; 2.5second on work machine
  (time
    (let [rdr (string-push-back-reader ana)
          eof (js-obj)]
     (binding [*ns* (create-ns 'cljs.analyzer)
               r/*data-readers* tags/*cljs-data-readers*]
       (loop []
         (let [x (r/read {:eof eof} rdr)]
           (when-not (identical? eof x)
             (recur)))))))

  ;; doesn't work yet
  (time
    (let [rdr (string-push-back-reader ana)
          eof (js-obj)
          env (ana/empty-env)]
      (binding [*ns* (create-ns 'cljs.analyzer)
                r/*data-readers* tags/*cljs-data-readers*]
        (loop []
          (let [form (r/read {:eof eof} rdr)]
            (when-not (identical? eof form)
              (ana/analyze env form)
              (recur)))))))
  )