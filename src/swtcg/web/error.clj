(ns swtcg.web.error)

(deftype NotFoundException [^String message ^clojure.lang.IPersistentMap data]
  Exception
  (getMessage [_] message)
  (getCause [_] nil)

  clojure.lang.IExceptionInfo
  (getData [_] data))

(deftype BadRequestException [^String message ^clojure.lang.IPersistentMap data]
  Exception
  (getMessage [_] message)
  (getCause [_] nil)

  clojure.lang.IExceptionInfo
  (getData [_] data))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; API and middleware

(defn not-found
  "Construct a NotFoundException with a message and optional data."
  ([msg] (NotFoundException. msg nil))
  ([msg data] (NotFoundException. msg data)))

(defn bad-request
  "Construct a BadRequestException with a message and optional data."
  ([msg] (BadRequestException. msg nil))
  ([msg data] (BadRequestException. msg data)))

(defmulti render-exception
  "Convert an exception to an HTTP response map."
  type)

(defmethod render-exception NotFoundException [^NotFoundException e]
  {:status 404
   :body {:error (.getMessage e)
          :data (.getData e)}})

(defmethod render-exception BadRequestException [^BadRequestException e]
  {:status 400
   :body {:error (.getMessage e)
          :data (.getData e)}})

(defmethod render-exception :default [e]
  {:status 500
   :body {:error "Unexpected server error"
          :message (.getMessage e)}})

(defn translate-error-middleware
  "Catches exceptions and renders HTTP responses."
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (render-exception e)))))
