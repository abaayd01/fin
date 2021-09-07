(ns fin.components.malli-instrumenter
  (:require
    [com.stuartsierra.component :as component]
    [malli.dev :as dev]
    [malli.dev.pretty :as pretty]))

(defrecord MalliInstrumenter []
  component/Lifecycle
  (start [this]
    (dev/start!)
    this)
  (stop [this]
    (dev/stop!)
    this))

(defn make-malli-instrumenter []
  (map->MalliInstrumenter {}))