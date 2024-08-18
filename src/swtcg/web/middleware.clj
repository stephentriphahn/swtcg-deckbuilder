(ns swtcg.web.middleware
  (:require [ring.middleware.nested-params :as nested]
            [ring.middleware.keyword-params :as kw-params]))

(def nested-params {:name ::wrap-nested-params :wrap nested/wrap-nested-params})
(def keyword-params {:name ::wrap-kw-params :wrap kw-params/wrap-keyword-params})

(def add-db
  {:name ::db
   :compile (fn [{:keys [db]} _]
              (fn [handler]
                (fn [req]
                  (handler (assoc req :db db)))))})
