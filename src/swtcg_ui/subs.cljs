(ns swtcg-ui.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :decks
 (fn [db _]
   (:decks db)))
