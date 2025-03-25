(ns swtcg.web.handlers
  (:require
   [ring.util.response :as response]
   [swtcg.data.db :as db]
   [swtcg.data.memory :as memory]))

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
  (let [id (get-in path-params [:id])]
    (response/response (db/get-card-by-id db id))))

(comment
  (normalize-opts {:foo "2" :bar {:gte "3"}})
  (def params {:speed {:gte 60}})
  (def db (db/connect (db/parse-connection-string "memory://foo")))
  (db/list-cards db params)
  (list-cards {:speed "60"})
  (#{:speed} :foo)
  #_())
