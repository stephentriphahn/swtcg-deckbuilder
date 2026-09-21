(ns stevetrip.swtcg.deck-builder.web.handlers
  (:require
   [ring.util.response :as response]
   [stevetrip.swtcg.deck-builder.log :as log]
   [stevetrip.swtcg.deck-builder.db.db :as db]
   [stevetrip.swtcg.deck-builder.db.memory :as memory]
   [stevetrip.swtcg.deck-builder.services.deck-service :as deck-service]
   [stevetrip.swtcg.deck-builder.web.error :as error]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; helpers

(defn parse
  [k v]
  (if (or (memory/num-fields k) (#{:gt :gte :lt :lte} k))
    (Integer/parseInt v)
    v))

(defn parse-int
  [[k v]]
  [k (if (map? v)
       (into {} (map parse-int v))
       (parse k v))])

(defn normalize-opts
  [params]
  (into {} (map parse-int params)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; request lenses
(defn req->deck-id
  [req]
  (get-in req [:parameters :path :deck-id]))

(defn req->path-card-id
  [req]
  (get-in req [:parameters :path :card-id]))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; handlers

(defn list-cards
  [{:keys [db params]}]
  (response/response {:cards (db/list-cards db (normalize-opts params))}))

(defn get-card-by-id
  [{:keys [db] :as req}]
  (let [id (req->path-card-id req)
        card (db/get-card-by-id db id)]
    (if-not card
      (throw (error/not-found {:card-id id}))
      (response/response card))))

(defn create-deck
  [{:keys [db parameters]}]
  (let [deck (db/add-deck db (:body parameters))]
    (response/created (str "/api/v1/decks/" (:deck-id deck)) deck)))

(defn get-deck-by-id
  [{:keys [db] :as req}]
  (->> req
       req->deck-id
       (deck-service/get-deck db)
       response/response))

(defn delete-deck
  [{:keys [db] :as req}]
  (deck-service/delete-deck db (req->deck-id req))
  (response/status (response/response nil) 204))

(defn add-card-to-deck
  [{:keys [db parameters] :as req}]
  (let [{:keys [quantity]} (:body parameters)]
    (response/response
     (deck-service/add-card db (req->deck-id req) (req->path-card-id req) quantity))))

(defn add-cards-to-deck
  [{:keys [db parameters] :as req}]
  (response/response
   (deck-service/add-cards db (req->deck-id req) (:body parameters))))

(defn remove-card-from-deck
  [{:keys [db] :as req}]
  (deck-service/remove-card db (req->deck-id req) (req->path-card-id req))
  (response/status (response/response nil) 204))

(defn list-decks
  [{:keys [db]}]
  (response/response (deck-service/list-decks db)))

(comment
  (normalize-opts {:foo "2" :bar {:gte "3"}})
  (def params {:speed {:gte 60}})
  (def db (db/connect (db/parse-connection-string "memory://foo")))
  (db/list-cards db params)
  (list-cards {:speed "60"})
  (#{:speed} :foo)
  #_())
