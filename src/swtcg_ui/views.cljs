(ns swtcg-ui.views
  (:require [re-frame.core :as re-frame]))

(defn home-page []
  [:div
   [:h1 "This is home page"]
   [:button
    ;; Dispatch navigate event that triggers a (side)effect.
    {:on-click #(re-frame/dispatch [:navigate :swtcg-ui.routes/cards])}
    "Go to sub-page 2"]])

(defn cards-page []
  [:div
   [:h1 "This is cards"]])

(defn decks-page []
  [:div
   [:h1 "This is decks page"]])
