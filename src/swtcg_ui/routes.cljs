(ns swtcg-ui.routes
  (:require
   [reitit.core :as r]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [re-frame.core :as re-frame]
   [swtcg-ui.views :as views]))

;; (re-frame/reg-fx
;;  ::navigate!
;;  (fn [k params query]
;;    (rfe/push-state k params query)))

;; (def routes
;;   [["/" {:name ::home
;;          :view (fn [] [:div "Welcome to SWTGC"])}]

;;    ["/cards" {:name ::cards
;;               :view (fn [] [:div [:h1 "Cards Page"]])}]

;;    ["/decks" {:name ::decks
;;               :view (fn [] [:div [:h1 "Decks Page"]])}]])

;; (def router
;;   (rf/router routes))

;; (defn on-navigate [new-match]
;;   (when new-match
;;     (re-frame/dispatch [:route/navigated new-match])))

;; (defn start! []
;;   (rfe/start!
;;    router
;;    on-navigate
;;    {:use-fragment false}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; routes
;; Triggering navigation from events.
(re-frame/reg-fx
 :navigate!
 (fn [k params query]
   (rfe/push-state k params query)))

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))

(def routes
  ["/"
   [""
    {:name      ::home
     :view      views/home-page
     :link-text "Home"
     :controllers
     [{;; Do whatever initialization needed for home page
       ;; I.e (re-frame/dispatch [::events/load-something-with-ajax])
       :start (fn [& params] (js/console.log "Entering home page"))
       ;; Teardown can be done here.
       :stop  (fn [& params] (js/console.log "Leaving home page"))}]}]
   ["cards"
    {:name ::cards
     :view      views/cards-page
     :link-text "Cards"
     :controllers
     [{:start (fn [& params] (js/console.log "Entering sub-page 1"))
       :stop  (fn [& params] (js/console.log "Leaving sub-page 1"))}]}]
   ["decks"
    {:name      ::decks
     :view      views/decks-page
     :link-text "Decks"
     :controllers
     [{:start (fn [& params] (js/console.log "Entering sub-page 2"))
       :stop  (fn [& params] (js/console.log "Leaving sub-page 2"))}]}]])

(defn on-navigate [new-match]
  (let [old-match (re-frame/subscribe [:current-route])]
    (when new-match
      (let [cs (rfc/apply-controllers (:controllers @old-match) new-match)
            m  (assoc new-match :controllers cs)]
        (re-frame/dispatch [:navigated m])))))

(def router
  (rf/router
   routes))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
   router
   on-navigate
   {:use-fragment true}))

(defn nav [{:keys [router current-route]}]
  (into
   [:ul]
   (for [route-name (r/route-names router)
         :let       [route (r/match-by-name router route-name)
                     text (-> route :data :link-text)]]
     [:li
      (when (= route-name (-> current-route :data :name))
        "> ")
      ;; Create a normal links that user can click
      [:a {:href (href route-name)} text]])))

(defn router-component [{:keys [router]}]
  (let [current-route @(re-frame/subscribe [:current-route])]
    [:div
     [nav {:router router :current-route current-route}]
     (when current-route
       [(-> current-route :data :view)])]))
