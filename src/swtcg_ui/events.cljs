(ns swtcg-ui.events
  (:require [re-frame.core :as re-frame]))

;; (rf/reg-event-db
;;  :initialize
;;  (fn [_ _]
;;    {:decks []}))

;; (rf/reg-event-db
;;  :route/navigated
;;  (fn [db [_ new-match]]
;;    (js/console.log new-match)
;;    (assoc db :current-route new-match)))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   {:current-route nil}))

(re-frame/reg-event-fx
 :navigate
 (fn [db [_ route]]
   ;; See `navigate` effect in routes.cljs
   {:navigate! route}))

(re-frame/reg-event-db
 :navigated
 (fn [db [_ new-match]]
   (assoc db :current-route new-match)))
