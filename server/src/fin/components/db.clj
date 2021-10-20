(ns fin.components.db
  (:require
    [fin.protocols :as p]
    [com.stuartsierra.component :as component]
    [next.jdbc :as jdbc]
    [next.jdbc.date-time :as dt]
    [next.jdbc.result-set :as rs]))

(defn- db-exec
  ([ds sql]
   (db-exec ds sql {:builder-fn rs/as-unqualified-maps}))
  ([ds sql opts]
   (jdbc/execute! (ds) sql opts)))

(defrecord DB [ds]
  component/Lifecycle
  (start [this]
    (dt/read-as-local)
    this)
  (stop [this]
    this)

  p/IDB
  (query [this sql]
    (db-exec (:ds this) sql)))

(defn make-db []
  (map->DB {}))
