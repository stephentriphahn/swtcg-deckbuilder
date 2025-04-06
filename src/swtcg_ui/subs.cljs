(ns swtcg-ui.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :cards
 (fn [db]
   (:cards db)))

(re-frame/reg-sub
 :current-route
 (fn [db]
   (:current-route db)))
