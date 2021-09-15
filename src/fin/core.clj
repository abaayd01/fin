(ns fin.core
  (:gen-class)
  (:require
    [clojure.tools.namespace.repl :refer [refresh refresh-all]]
    [com.stuartsierra.component :as component]
    [fin.components.db :refer [make-db]]
    [fin.components.malli-instrumenter :refer [make-malli-instrumenter]]
    [fin.components.transaction-repository :refer [make-transaction-repository]]
    [fin.middleware :as middleware]
    [next.jdbc.connection :as connection]
    [reitit.ring :as ring]
    [ring.adapter.jetty :as jetty])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defn handler [req]
  {:status 200, :body {:hello "world!"}})

(defn post-handler [req]
  {:status 200, :body {:hello "world!"}})

(def muuntaja-instance
  (muuntaja.core/create))

(def app (ring/ring-handler
           (ring/router
             ["/api"
              {:muuntaja   muuntaja-instance
               :middleware middleware/pipeline}
              ["/ping" {:get  handler
                        :post post-handler
                        :name ::ping}]])
           (ring/routes
             (ring/create-default-handler))))

(defrecord Server [handler options]
  component/Lifecycle
  (start [this]
    (assoc this :server (jetty/run-jetty handler options)))

  (stop [this]
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-server [handler options]
  (->Server handler options))

(def ^:private db-spec {:dbtype "mysql" :dbname "fin" :username "root" :password "password"})

(defn create-system [{:keys [db-spec server-options handler]}]
  (-> (component/system-map
        :ds (connection/component HikariDataSource db-spec)
        :db (make-db)
        :transaction-repository (make-transaction-repository :transactions)
        :malli-instrumenter (make-malli-instrumenter)
        :server (make-server handler server-options))
      (component/system-using
        {:db                     {:ds :ds}
         :transaction-repository {:db :db}})))

(defonce the-system nil)

(defn init []
  (alter-var-root #'the-system (constantly
                                 (create-system {:db-spec        db-spec
                                                 :server-options {:port  3000
                                                                  :join? false}
                                                 :handler        app}))))

(defn start []
  (alter-var-root #'the-system component/start))

(defn stop []
  (alter-var-root #'the-system #(when % (component/stop %))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'fin.core/go))

(comment
  (start)
  (stop)
  (go)
  (reset)
  )