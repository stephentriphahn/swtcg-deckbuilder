(ns swtcg.log
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json])
  (:import [java.time Instant]))

(defn log-json
  "Logs a structured JSON log with timestamp, level, and event name.
   - level: one of :info, :warn, :error, :debug
   - event: a keyword like :user-login
   - payload: a map of structured fields"
  [level event payload]
  (let [timestamp (.toString (Instant/now))
        level-str (.toUpperCase (name level))
        entry (merge {:timestamp timestamp
                      :level level-str
                      :event (name event)}
                     payload)
        msg (json/generate-string entry)]
    (case level
      :info (log/info msg)
      :debug (log/debug msg)
      :warn (log/warn msg)
      :error (log/error msg)
      (log/info msg))))

(def info (partial log-json :info))
(def debug (partial log-json :debug))
(def warn (partial log-json :war))
(def error (partial log-json :error))
