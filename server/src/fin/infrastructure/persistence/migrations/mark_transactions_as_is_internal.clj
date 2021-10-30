(ns fin.infrastructure.persistence.migrations.mark-transactions-as-is-internal
  (:require
    [fin.core.migration-service :refer [make-mysql-migration-service] :as ms]
    [fin.infrastructure.persistence.db :refer [make-db]]
    [fin.infrastructure.persistence.mysql-repository :refer [make-repository]]

    [com.stuartsierra.component :as component]
    [next.jdbc.connection :as connection])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defn- create-system [{:keys [db-spec]}]
  (-> (component/system-map
        :ds (connection/component HikariDataSource db-spec)
        :db (make-db)
        :repository (make-repository)
        :migration-service (make-mysql-migration-service))
      (component/system-using
        {:db                {:ds :ds}
         :repository        {:db :db}
         :migration-service {:repository :repository}})))

(defn migrate-up [{:keys [db]}]
  (let [db-spec           (dissoc (assoc db :username (:user db)) :user)
        system            (component/start-system (create-system {:db-spec db-spec}))
        migration-service (:migration-service system)
        transactions      (ms/find-all-transactions migration-service)]
    (doseq [transaction transactions]
      (ms/update-transaction-if-is-internal! migration-service (:id transaction)))
    (component/stop system)))

(defn migrate-down [_]
  (throw (Exception. "irreversible migration")))

(comment
  )
