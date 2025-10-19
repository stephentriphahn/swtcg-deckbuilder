(ns swtcg.db.connection
  (:require [clojure.string :as str]))

(defprotocol ConnectionProvider
  "Protocol for database connections"
  (get-db [this] "Get the underlying database/state")
  (db-type [this] "Get the database type (:memory, :sqlite, etc.)")
  (close [this] "Close/cleanup the connection"))

(defn parse-connection-string
  "Parse connection string into {:scheme :path :params}"
  [conn-str]
  (let [[scheme rest] (str/split conn-str #"://" 2)
        [path query] (str/split (or rest "") #"\?" 2)
        params (when query
                 (into {}
                       (map #(let [[k v] (str/split % #"=")]
                               [(keyword k) v]))
                       (str/split query #"&")))]
    {:scheme (keyword scheme)
     :path path
     :params params}))

(defmulti connect
  "Create a connection based on connection string scheme"
  (fn [conn-str]
    (-> conn-str
        parse-connection-string
        :scheme)))

(comment
  (parse-connection-string "sqlite://cards.db")
  (parse-connection-string "postgres://some.host.com/swtcg")
  (parse-connection-string "postgres://some.host.com/swtcg?mode=ro")
  #_())
