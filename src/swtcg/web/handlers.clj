(ns swtcg.web.handlers
  (:require [swtcg.data.card :as card]
            [ring.util.response :as response]))

(defn parse-int
  [[k v]]
  (if (map? v)
    [k (into {} (map parse-int v))]
    [k (Integer/parseInt v)]))

(defn normalize-params
  [params]
  (into {} (map parse-int params)))

(defn list-cards
  [opts]
  (let [db (card/connect "memory://foo")
        params (normalize-params opts)]
    (response/response {:cards (card/list-all db params)})))

(comment
  (normalize-params {:foo "2" :bar {:gte "3"}})
  (def params {:speed {:gte 60}})
  (def db (card/connect "memory://foo"))
  (card/list-all db params)
  (list-cards {:speed "60"})
  #_())
