(ns swtcg-ui.routes
  (:require
   [reitit.core :as r]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [re-frame.core :as re-frame]
   [swtcg-ui.views :as views]))

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

(def router (rf/router routes))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
   router
   on-navigate
   {:use-fragment false}))

(defn nav [{:keys [router]}]
  [:nav.navbar.navbar-expand-lg.navbar-light.bg-light.mb-4
   [:div.container-fluid
    [:a.navbar-brand {:href "/"} "SWTGC"]
    [:ul.navbar-nav.me-auto.mb-2.mb-lg-0
     (for [route-name (r/route-names router)
           :let       [route (r/match-by-name router route-name)
                       text (-> route :data :link-text)]]
       ^{:key text}
       [:li.nav-item
        [:a.nav-link {:href (href route-name)} text]])]]])

(defn router-component [{:keys [router]}]
  (let [current-route @(re-frame/subscribe [:current-route])]
    [:div
     [nav {:router router :current-route current-route}]
     (when current-route
       [(-> current-route :data :view)])]))
