(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :refer [go, reset set-prep!]]
   [swtcg.config :as config]
   [swtcg.system :as system]))

(def cfg (config/read-config))
(def sys-map (system/config->system-map cfg))

(set-prep! #(ig/expand sys-map))
