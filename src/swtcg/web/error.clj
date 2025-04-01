(ns swtcg.web.error
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defmacro defexceptions
  "Registers functions for each item in the error registry.
  These error constructors return an Exception that can be thrown"
  [registry-sym]
  (let [registry (eval registry-sym)
        types (for [{:keys [name status message]} registry
                    :let [data-sym (gensym "data")
                          name-sym (symbol name)]]
                [`(defn ~name-sym
                    [^clojure.lang.IPersistentMap ~data-sym]
                    (ex-info ~message (merge {:status ~status :type ~name} ~data-sym)))
                 `(defmethod render-exception ~name [e#]
                    {:status ~status
                     :body {:error (.getMessage e#)}})])]
    `(do ~@(reduce concat types))))

(defmulti render-exception #(-> % ex-data :type))

(defn load-error-registry []
  (with-open [r (-> "http-errors.edn"
                    io/resource
                    io/reader)]
    (edn/read (java.io.PushbackReader. r))))

(defonce error-registry (load-error-registry))
(defexceptions error-registry)

(defn translate-error-middleware
  "Catches exceptions and renders HTTP responses."
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (render-exception e)))))

(comment
  (load-error-registry)
  (ex-message (not-found "not found" {:foo "bar"}))
  (render-exception (not-found "not found" {:foo "bar"}))
  (macroexpand-1 '(defexceptions error-registry))
  (macroexpand-1 '(defrenderers error-registry))
  #_())
