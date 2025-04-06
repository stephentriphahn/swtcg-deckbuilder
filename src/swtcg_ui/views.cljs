(ns swtcg-ui.views
  (:require [re-frame.core :as re-frame]))

(defn home-page []
  [:div
   [:h1 "This is home page"]
   [:button
    ;; Dispatch navigate event that triggers a (side)effect.
    {:on-click #(re-frame/dispatch [:navigate :swtcg-ui.routes/cards])}
    "Go to sub-page 2"]])

(defn card-view [card]
  [:div.col-md-3.mb-4
   [:div.card
    [:img.card-img-top
     {:src (str "/setimages/" (:set_code card) "/" (:image_file card) ".jpg")
      :alt (:name card)
      :style {:width "100%"}}]
    [:div.card-body
     [:h5.card-title (:name card)]
     [:p.card-text (:subtype card)]]]])

(defn cards-page []
  (let [cards @(re-frame/subscribe [:cards])]
    [:div.row
     (for [card cards]
       ^{:key (:card_id card)}
       [card-view card])]))

(defn decks-page []
  [:div
   [:h1 "This is decks page"]])
