(ns swtcg-ui.subs
  (:require [re-frame.core :as re-frame]))

;; (rf/reg-sub
;;  :decks
;;  (fn [db _]
;;    (:decks db)))

;; (rf/reg-sub
;;  :current-route
;;  (fn [db _]
;;    (js/console.log db)
;;    (:current-route db)))

(re-frame/reg-sub
 :current-route
 (fn [db]
   (:current-route db)))
