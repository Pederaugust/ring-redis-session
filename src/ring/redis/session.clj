(ns ring.redis.session
  (:require [taoensso.timbre :as log]
            [ring.middleware.session.store :as api]
            [ring.redis.session.util :as util]
            [taoensso.carmine :as carmine]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Method implementations   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-redis-session
  "Read a session from a Redis store."
  [this session-key]
  (let [conn (:redis-conn this)]
    (when session-key
      (log/debug "In read-session ...")
      (log/debug "\tsession-key:" session-key)
      (when-let [data (carmine/wcar conn (carmine/get session-key))]
        (let [read-handler (:read-handler this)]
          (when (and (:expiration this) (:reset-on-read this))
            (carmine/wcar conn (carmine/expire session-key (:expiration this))))
          (read-handler data))))))

(defn write-redis-session
  "Write a session to a Redis store."
  [this old-session-key data]
  (let [conn (:redis-conn this)
        session-key (or old-session-key (util/new-session-key (:prefix this)))
        expiri (:expiration this)]
    (log/debug "In write-redis-session ...")
    (log/debug "\tsession-key:" session-key)
    (log/debug "\tdata:" data)
    (let [write-handler (:write-handler this)]
      (if expiri
        (carmine/wcar conn (carmine/setex session-key expiri (write-handler data)))
        (carmine/wcar conn (carmine/set session-key (write-handler data)))))
    session-key))

(defn delete-redis-session
  "Delete a session in a Redis store."
  [this session-key]
  (carmine/wcar (:redis-conn this) (carmine/del session-key))
  nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Protocol Implementation   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord RedisStore [redis-conn prefix expiration reset-on-read read-handler write-handler])

(def store-behaviour {:read-session read-redis-session
                      :write-session write-redis-session
                      :delete-session delete-redis-session})

(extend RedisStore api/SessionStore store-behaviour)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Constructor   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redis-store
  "Creates a redis-backed session storage engine."
  ([redis-conn]
   (redis-store redis-conn {}))
  ([redis-conn {:keys [prefix expire-secs reset-on-read read-handler write-handler]
                :or {prefix "session"
                     read-handler identity
                     write-handler identity
                     reset-on-read false}}]
   (log/debug "Creating Redis store ...")
   (->RedisStore redis-conn prefix expire-secs reset-on-read read-handler write-handler)))





