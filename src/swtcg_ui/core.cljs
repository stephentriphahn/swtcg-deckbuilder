(ns swtcg-ui.core
  (:require
   [reagent.dom :as rd]
   [re-frame.core :as re-frame]
   [swtcg-ui.subs]
   [swtcg-ui.routes :as routes]
   [swtcg-ui.events]))

#_(defn init []
    (routes/start!)
    (rd/render (views/main) (.getElementById js/document "app")))

(def debug? ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (routes/init-routes!) ;; Reset routes on figwheel reload
  (rd/render [routes/router-component {:router routes/router}]
             (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
