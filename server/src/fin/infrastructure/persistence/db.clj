(ns fin.infrastructure.persistence.db
  (:require
    [com.stuartsierra.component :as component]
    [next.jdbc :as jdbc]
    [next.jdbc.date-time :as dt]))

(defprotocol IDB
  (execute! [this sql]))

(defrecord DB [ds]
  component/Lifecycle
  (start [this]
    (dt/read-as-local)
    this)
  (stop [this]
    this)

  IDB
  (execute! [this sql]
    (jdbc/execute! ((:ds this)) sql)))

(defn make-db []
  (map->DB {}))
