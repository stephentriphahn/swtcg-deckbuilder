(ns swtcg-ui.events
  (:require [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(re-frame/reg-event-db
 :get-cards-success
 (fn [db [_ results]]
   (-> db
       (assoc :cards (:cards results))
       (assoc :fetching? false))))

(re-frame/reg-event-db
 :get-cards-failure
 (fn [db [_ results]]
   (assoc db :error (str results))))

#_(re-frame/reg-event-db
   :group-cards-by
   (fn [db [_ field]]
     (-> db
         (update :cards (partial group-by field))
         (assoc :cards/grouped-by field))))

(re-frame/reg-event-fx
 :load-cards
 (fn [{:keys [db]} [_ _]]
   {:db (assoc db :fetching? true)
    :http-xhrio {:uri "http://localhost:3000/api/v1/cards"
                 :method :get
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
                 :on-success      [:get-cards-success]
                 :on-failure      [:get-cards-failure]}}))

(re-frame/reg-event-fx
 :initialize-db
 (fn [{:keys [db]} _]
   {:db {:current-route nil
         :cards []
         :cards/grouped-by nil
         :fetching? false}
    :dispatch [:load-cards]}))

(re-frame/reg-event-fx
 :navigate
 (fn [_cofx [_ route]]
   ;; See `navigate` effect in routes.cljs
   {:navigate! route}))

(re-frame/reg-event-db
 :navigated
 (fn [db [_ new-match]]
   (assoc db :current-route new-match)))
