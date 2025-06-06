(ns swtcg.web.middleware
  (:require [ring.middleware.nested-params :as nested]
            [ring.middleware.keyword-params :as kw-params]
            [swtcg.web.error :as error]))

(def nested-params {:name ::wrap-nested-params :wrap nested/wrap-nested-params})
(def keyword-params {:name ::wrap-kw-params :wrap kw-params/wrap-keyword-params})
(def translate-http-error {:Name ::wrap-translate-http-error :wrap error/translate-error-middleware})

(def add-db
  {:name ::db
   :compile (fn [{:keys [db]} _]
              (fn [handler]
                (fn [req]
                  (handler (assoc req :db db)))))})
