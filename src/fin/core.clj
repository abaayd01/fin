(ns fin.core
  (:gen-class)
  (:require
    [fin.components.db :refer [make-db]]
    [fin.components.ingress-orchestrator :refer [make-ingress-orchestrator]]
    [fin.components.ingress-service :refer [make-ingress-service]]
    [fin.components.malli-instrumenter :refer [make-malli-instrumenter]]
    [fin.components.transaction-repository :refer [make-transaction-repository]]

    [fin.protocols :as p]

    [clojure.tools.namespace.repl :refer [refresh refresh-all]]
    [com.stuartsierra.component :as component]
    [next.jdbc.connection :as connection]
    )
  (:import (com.zaxxer.hikari HikariDataSource)))

(def ^:private db-spec {:dbtype "mysql" :dbname "fin" :username "root" :password "password"})

(defn create-system [{:keys [db-spec]}]
  (-> (component/system-map
        :ds (connection/component HikariDataSource db-spec)
        :db (make-db)
        :transaction-repository (make-transaction-repository :transactions)
        :ingress-service (make-ingress-service)
        :ingress-orchestrator (make-ingress-orchestrator)
        :malli-instrumenter (make-malli-instrumenter))
      (component/system-using
        {:db                     {:ds :ds}
         :transaction-repository {:db :db}
         :ingress-orchestrator   {:transaction-repository :transaction-repository
                                  :ingress-service        :ingress-service}})))

(defonce the-system nil)

(defn init []
  (alter-var-root #'the-system (constantly (create-system {:db-spec db-spec}))))

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
  (refresh-all)
  (p/run (:ingress-orchestrator the-system))
  (p/get-transactions (:ingress-service the-system))

  (def err *e)

  (Throwable->map err)
  )

(comment
  "
  TODO
  2. add source col to table / transaction model
  "
  )