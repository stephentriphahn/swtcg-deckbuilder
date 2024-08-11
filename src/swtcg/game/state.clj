(ns swtcg.game.state
  (:require [clojure.string :as str]))

(defn gen-short-id
  "First section of a UUID.  Does not guarantee minimal collisions. Fine for 120 cards."
  []
  (-> (random-uuid)
      str
      (str/split #"-")
      first))

(defn- add-card-sid
  "Adds short UUID to card id. The same card may show up in both decks (or multiple in the same deck)
   A unique 'session-id (sid)' for each card ensures there are no collisions."
  [card]
  (let [session-id (str (:id card) "-" (gen-short-id))]
    (assoc card :card-sid session-id)))

(defn draw
  "Given a player state, moves n cards from players deck to players hand."
  [player-state n]
  (let [[drawn remaining] (split-at n (:deck player-state))
        ;; TODO refactor cards-map
        cards-map (reduce merge (map #(hash-map (keyword (:card-sid %)) %) drawn))]
    (-> player-state
        (update :hand merge cards-map)
        (assoc :deck remaining))))

(defn move-card
  "Given a game state and a player key, moves the given card from 'from' to 'to'"
  ([s player from to card-sid]
   (move-card s player from to card-sid identity))
  ([s player from to card-sid decorator]
   {:pre [(#{:lightside :darkside} player)]}
   (let [card (-> s
                  player
                  :hand
                  (get card-sid))
         ;; TODO maybe validate movement before calling this
         _ (when-not card
             (throw (ex-info "invalid movement" {:card-sid card-sid})))]
     (-> s
         (update-in [player from] dissoc card-sid)
         (update-in [player to] assoc card-sid (decorator card))))))

(defn add-status
  [card]
  (assoc card :status {:bp 0 :tapped? false :damage 0}))

(defn hand->build-zone
  [state player card-sid]
  (move-card state player :hand :build-zone card-sid add-build-status))

(defn get-arena-type
  [card]
  (-> card :type str/lower-case keyword))

(defn build-zone->arena
  [state player card-sid]
  (let [arena-type (-> player :build-zone card-sid get-arena-type)]
    (move-card state player :build-zone arena-type card-sid)))

(defn change-force
  [state player n]
  (update-in state [player :force] + n))

(defn empty-player
  [cards]
  {:deck cards :hand {}
   :build-zone {} :force 8 :discard {}
   :space {} :ground {} :character {}})

(defn init-player
  "Given a sequence of cards in a deck, returns an initialized player map to start a game."
  [cards]
  (let [session-cards (map add-card-sid cards)]
    (-> session-cards
        shuffle
        empty-player
        (draw 7))))

(defn init-game
  [light-deck dark-deck]
  {:lightside (init-player light-deck)
   :darkside (init-player dark-deck)})

(comment
  (def test-cards (for [i (range 20)] {:id i}))
  (init-game (take 20 (repeat {})) (take 20 (repeat {})))
  (def game-state (atom  (init-game test-cards test-cards)))
  @game-state
  (change-force @game-state :darkside -4)
  (hand->build-zone @game-state :lightside :7-2907c8dc)
  #_())
