(ns stevetrip.swtcg.deck-builder.services.pack-service
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [stevetrip.swtcg.deck-builder.db.db :as swtcg-db]
   [stevetrip.swtcg.deck-builder.tools.load-cards :as load-cards]
   [stevetrip.swtcg.deck-builder.web.error :as error]))

;; See doc/pack-opening-design.md: a standard pack is 7 Common, 3 Uncommon, 1 Rare,
;; confirmed against resources/public/packs/packdefinitions.xml for every official set.
(def pack-composition [["C" 7] ["U" 3] ["R" 1]])

(defn official-sets
  "The 10 official WOTC sets loaded into cards.db — the only ones a pack can be opened
  from. Reuses tools.load-cards/original-sets, the canonical list (see CLAUDE.md)."
  []
  load-cards/original-sets)

(defn- load-set-names
  "resources/set-name-to-code.edn maps full name -> code; inverted here for display."
  []
  (with-open [r (-> "set-name-to-code.edn" io/resource io/reader)]
    (into {} (map (fn [[full-name code]] [code full-name])) (edn/read (java.io.PushbackReader. r)))))

(defonce ^:private code->name (load-set-names))

(defn list-packs
  "One pack per official set, with its wrapper art (already served statically, see
  doc/pack-opening-design.md §2 — no route needed for /packs/{SET}.jpg or /cardback.jpg)."
  []
  (for [set-code (sort (official-sets))]
    {:set-code set-code
     :name (get code->name set-code set-code)
     :image (str "/packs/" set-code ".jpg")}))

(defn- require-official-set!
  [set-code]
  (when-not (contains? (official-sets) set-code)
    (throw (error/bad-request {:set-code set-code :reason "not an official set"}))))

(defn- draw-rarity
  "Picks `n` card-ids at random, with replacement, from one rarity's pool in a set — a
  real booster can (and does) repeat a common, so this isn't a unique draw."
  [db set-code rarity n]
  (let [pool (swtcg-db/list-cards-by-set-and-rarity db set-code rarity)]
    (when (empty? pool)
      (throw (error/bad-request {:set-code set-code :rarity rarity
                                  :reason "no cards of this rarity in this set"})))
    (repeatedly n #(:card-id (rand-nth pool)))))

(defn- hydrate
  "Looks each card up once and maps the ordered, possibly-repeating, id list back onto
  full card records — hydrated results, not just ids, per the pack endpoints' contract."
  [db card-ids]
  (let [by-id (into {} (map (juxt identity #(swtcg-db/get-card-by-id db %)))
                    (distinct card-ids))]
    (mapv by-id card-ids)))

(defn- opening->response
  [db {:keys [card-ids] :as opening}]
  (-> opening
      (dissoc :card-ids)
      (assoc :cards (hydrate db card-ids))))

(defn open-pack
  "Draws a pack for `set-code`, credits every card to `owner`'s collection, and logs
  the opening — all in one transaction (db.sqlite/open-pack*). Returns the opening with
  its cards hydrated, in reveal order: all 7 commons, then the 3 uncommons, then the rare
  last, matching pack-composition's own order — the UI reveals one at a time in this order,
  building up to the rare (not a full shuffle across rarities)."
  [db owner set-code]
  (require-official-set! set-code)
  (let [card-ids (vec (mapcat (fn [[rarity n]] (draw-rarity db set-code rarity n))
                               pack-composition))
        opening (swtcg-db/record-pack-opening db {:owner owner :set-code set-code :cards card-ids})]
    (opening->response db opening)))

(defn get-opening
  "Re-fetches a previously recorded opening, e.g. after a refresh mid-reveal."
  [db opening-id]
  (if-let [opening (swtcg-db/get-pack-opening-by-id db opening-id)]
    (opening->response db opening)
    (throw (error/not-found {:opening-id opening-id}))))

(defn get-collection
  "Unhydrated for now (card-id + owned quantity only) — see doc/pack-opening-design.md
  §8 phase 1; hydration lands with the deck-cards hydration fix it's paired with there."
  [db owner]
  (swtcg-db/get-collection db owner))
