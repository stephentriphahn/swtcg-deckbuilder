(ns swtcg.game.game
  (:require [clojure.string :as str]))

(defn gen-short-id
  "First section of a UUID.  Does not guarantee minimal collisions. Fine for 120 cards."
  []
  (-> (random-uuid)
      str
      (str/split #"-")
      first))

(defn- add-session-card-id
  "Adds short UUID to card id. The same card may show up in both decks (or multiple in the same deck)
   A unique 'session-id' for each card ensures there are no collisions."
  [card]
  (let [session-id (str (:id card) "-" (gen-short-id))]
    (assoc card :session-card-id session-id)))

(defn draw
  "Given a player state, moves n cards from players deck to players hand."
  [player-state n]
  (let [[drawn remaining] (split-at n (:deck player-state))
        ;; TODO refactor cards-map
        cards-map (reduce merge (map #(hash-map (keyword (:session-card-id %)) %) drawn))]
    (-> player-state
        (update :hand merge cards-map)
        (assoc :deck remaining))))

(defn move-card
  "Given a game state and a player key, moves the given card from 'from' to 'to'"
  [s player from to card-session-id]
  {:pre [(#{:lightside :darkside} player)]}
  (let [card (-> s
                 player
                 :hand
                 (get card-session-id))]
    (-> s
        (update-in [player from] dissoc card-session-id)
        (update-in [player to] assoc card-session-id card))))

(defn hand->build-zone
  [player-state card-session-id]
  (let [card (-> player-state
                 :hand
                 (get card-session-id))]
    (-> player-state
        (update :hand dissoc card-session-id)
        (update :build-zone assoc card-session-id card))))

(defn empty-player
  [cards]
  {:deck cards :hand {}
   :build-zone {} :force 8 :discard {}
   :space {} :ground {} :character {}})

(defn init-player
  "Given a sequence of cards in a deck, returns an initialized player map to start a game."
  [cards]
  (let [session-cards (map add-session-card-id cards)]
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
  (init-game (take 20 (constantly {})) (take 20 (repeat {})))
  (def game-state (atom  (init-game test-cards test-cards)))
  @game-state
  (swap! game-state update :lightside #(draw % 1))
  (swap! game-state update :lightside hand->build-zone :14-9c80be8e)
  (swap! game-state move-card :lightsi :hand :discard :11-c513bcad)
  #_())
