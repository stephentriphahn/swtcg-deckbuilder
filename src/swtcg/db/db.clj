(ns swtcg.db.db
  (:require
   [swtcg.db.connection :as conn]
   [clojure.string :as string])
  (:import
   (java.net URI)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; generic structure for connection string uri to clojure map
;;; TODO implement schema check (is spec still relevant?)
;;; {:scheme :sqlite              ;; or :postgres, :mongo, :memory
;;;  :database "cards"
;;;  :host nil
;;;  :port nil
;;;  :user nil
;;;  :password nil
;;;  :options {}                  ;; optional query params (e.g. ?ssl=true)
;;;  :raw-uri "sqlite://cards.db" ;; for trace/debugging
;;; }

(defn parse-connection-string [conn-str]
  (let [uri (URI. conn-str)
        backend (keyword (.getScheme uri))
        host (.getHost uri)
        port (.getPort uri)
        path (.getPath uri)
        user-info (.getUserInfo uri)
        [user password] (when user-info (clojure.string/split user-info #":" 2))]
    {:scheme backend
     :database (some-> path (clojure.string/replace #"^/" ""))
     :host host
     :port (when (not= port -1) port)
     :user user
     :password password
     :options {} ;; parse ?query=string here if needed
     :raw-uri conn-str}))

(defn parsed-cs->jdbc-config
  [{:keys [scheme database host port user password]}]
  (cond-> {:dbtype (name scheme)
           :dbname database}
    (= scheme :sqlite) (assoc :dbname host) ;; FIXME this is a hack for sqlite cs without a hostname
    host (assoc :host host)
    port (assoc :port port)
    (= scheme :sqlite) (dissoc :host)
    user (assoc :user user)
    password (assoc :password password)))

(defprotocol CardDatabase
  ;; Read-only card operations
  (list-cards [this opts] "Returns all cards in the system. Opts map equivalent to where [key] = [value]")
  (get-card-by-id [this id] "Returns a single card by Id")

  ;; deck meta-data creation
  (add-deck [this deck] "Creates a new deck. Expects {:name :owner :side}")
  (delete-deck [this deck-id] "Removes a deck from the system.")
  (get-deck-by-id [this deck-id] "Retrieves deck metadata")

  ;; associating cards with decks
  (get-deck-cards [this deck-id])
  (add-card-to-deck [this deck-id card-id quantity] "Adds or updates a card in a deck.")
  (remove-card-from-deck [this deck-id card-id] "Removes a card entirely from a deck."))

(defmulti create-database
  "Create database implementation from connection"
  conn/db-type)

(comment
  (def test-cs "sqlite://memory/cards.db")
  (parse-connection-string test-cs)
  #_())
