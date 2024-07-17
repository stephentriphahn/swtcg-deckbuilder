(ns swtcg.web.routes
  (:require  [compojure.core :refer [context defroutes GET routes]]
             [compojure.route :as route]
             [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
             [ring.middleware.json :as json]
             [ring.middleware.nested-params :as nested]
             [ring.middleware.keyword-params :as kw-params]
             [ring.middleware.resource :refer [wrap-resource]]
             [swtcg.web.handlers :as handlers]))

(defroutes card-routes
  (context "/api/v1" []

    (GET "/cards" {params :params}
      (handlers/list-cards params))
    (route/resources "/")))

(defn wrap-internal-error
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (println (.getMessage t))
        {:status 500 :body "Internal server error"}))))

(def app
  (let [rs (routes #'card-routes (route/not-found "not found"))]
    (-> rs
        kw-params/wrap-keyword-params
        nested/wrap-nested-params
        (wrap-defaults site-defaults)
        json/wrap-json-response
        wrap-internal-error)))

(comment
  #_())
