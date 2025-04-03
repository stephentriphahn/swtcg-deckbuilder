(ns swtcg.web.handlers
  (:require
   [ring.util.response :as response]
   [swtcg.db.db :as db]
   [swtcg.db.memory :as memory]
   [swtcg.web.error :as error]))

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

(comment
  (normalize-opts {:foo "2" :bar {:gte "3"}})
  (def params {:speed {:gte 60}})
  (def db (db/connect (db/parse-connection-string "memory://foo")))
  (db/list-cards db params)
  (list-cards {:speed "60"})
  (#{:speed} :foo)
  #_())
