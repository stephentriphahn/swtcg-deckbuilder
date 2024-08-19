(ns swtcg.data.protocols)

(defprotocol Repo
  (list-all [_ opts])
  (get-by-id [_ id])
  (update-by-id [_ id body])
  (delete-by-id [_ id]))
