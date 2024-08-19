(ns swtcg.web.handlers
  (:require [swtcg.data.protocols :as protocols]
            [swtcg.data.card :as card]
            [ring.util.response :as response]))

(defn parse
  [k v]
  (if (or (card/num-fields k) (#{:gt :gte :lt :lte} k))
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
  (let [card-repo (:card-repo db)]
    (response/response {:cards (protocols/list-all card-repo (normalize-opts params))})))

(comment
  (normalize-params {:foo "2" :bar {:gte "3"}})
  (def params {:speed {:gte 60}})
  (def db (card/connect "memory://foo"))
  (card/list-all db params)
  (list-cards {:speed "60"})
  (#{:speed} :foo)
  #_())
