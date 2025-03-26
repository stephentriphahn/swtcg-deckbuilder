(ns swtcg.log
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json])
  (:import [java.time Instant]))

(defn exception->map [^Throwable e]
  {:type (.getName (class e))
   :message (.getMessage e)
   :stacktrace (->> (.getStackTrace e)
                    (map str)
                    (take 25) ;; limit stack depth
                    vec)})

(defn log-json
  "Logs a structured JSON log with timestamp, level, event name,
   and optional exception (e).
   - level: one of :info, :warn, :error, :debug
   - event: a keyword like :user-login
   - payload: a map of structured fields"
  ([level event payload]
   (log-json level event payload nil))
  ([level event payload ^Throwable e]
   (let [timestamp (.toString (Instant/now))
         level-str (.toUpperCase (name level))
         base-entry {:timestamp timestamp
                     :level level-str
                     :event (name event)}
         error-entry (if e {:exception (exception->map e)} {})
         entry (merge base-entry payload error-entry)
         msg (json/generate-string entry)]
     (case level
       :info (log/info msg)
       :debug (log/debug msg)
       :warn (log/warn msg)
       :error (log/error msg)
       (log/info msg)))))

(def info (partial log-json :info))
(def debug (partial log-json :debug))
(def warn (partial log-json :war))
(def error (partial log-json :error))
