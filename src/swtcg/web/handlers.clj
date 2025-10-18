(ns swtcg.web.handlers
  (:require
   [ring.util.response :as response]
   [swtcg.log :as log]
   [swtcg.db.db :as db]
   [swtcg.db.memory :as memory]
   [swtcg.services.deck-service :as deck-service]
   [swtcg.web.error :as error]))

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
;;; handlers

(defn list-cards
  [{:keys [db params]}]
  (response/response {:cards (db/list-cards db (normalize-opts params))}))

(defn get-card-by-id
  [{:keys [db path-params]}]
  (let [id (get-in path-params [:id])
        card (db/get-card-by-id db id)]
    (if-not card
      (throw (error/not-found {:card-id id}))
      (response/response card))))

(defn create-deck
  [{:keys [db parameters]}]
  (let [deck (db/add-deck db (:body parameters))]
    {:status 201
     :headers {"Location" (str "/api/v1/decks/" (:id deck))}
     :body deck}))

(defn get-deck-by-id
  [{:keys [db parameters]}]
  {:status 200 :body (deck-service/get-deck db (:id (:path parameters)))})

(defn delete-deck [arg1])

(defn add-card-to-deck [{:keys [db parameters]}]
  (let [{:keys [card-id quantity]} (:body parameters)
        {:keys [id]} (:path parameters)]
    (db/add-card-to-deck db id card-id quantity)))

(defn remove-card-from-deck [arg1])
(defn list-decks [arg1])

(comment
  (normalize-opts {:foo "2" :bar {:gte "3"}})
  (def params {:speed {:gte 60}})
  (def db (db/connect (db/parse-connection-string "memory://foo")))
  (db/list-cards db params)
  (list-cards {:speed "60"})
  (#{:speed} :foo)
  #_())
