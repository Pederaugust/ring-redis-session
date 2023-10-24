(ns ring.redis.session.util
  "Redis session storage."
  (:require [clojure.pprint :as pprint])
  (:import java.util.UUID))

(defn new-session-key [prefix]
  (str prefix ":" (str (UUID/randomUUID))))

