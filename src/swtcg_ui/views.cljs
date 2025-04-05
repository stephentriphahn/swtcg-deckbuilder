(ns swtcg-ui.views
  (:require [re-frame.core :as rf]))

(defn main []
  (let [decks @(rf/subscribe [:decks])]
    [:div
     [:h1 "Card Browser"]
     (for [{:keys [name id]} decks]
       [:div {:key id} (str name)])]))
