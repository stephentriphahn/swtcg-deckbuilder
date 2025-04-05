(ns swtcg-ui.core
  (:require
   [reagent.dom :as rd]
   [swtcg-ui.views :as views]
   [swtcg-ui.subs]
   [swtcg-ui.events]))

(defn init []
  (rd/render (views/main) (.getElementById js/document "app")))
