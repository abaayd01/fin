(ns fin.infrastructure.persistence.migrations.seed-categories-table
  (:require
    [fin.infrastructure.persistence.db :refer [make-db] :as d]

    [com.stuartsierra.component :as component]
    [honey.sql :as sql]
    [next.jdbc.connection :as connection])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defn- create-system [{:keys [db-spec]}]
  (-> (component/system-map
        :ds (connection/component HikariDataSource db-spec)
        :db (make-db))
      (component/system-using
        {:db {:ds :ds}})))

(defn migrate-up [{:keys [db]}]
  (let [db-spec (dissoc (assoc db :username (:user db)) :user)
        system  (component/start-system (create-system {:db-spec db-spec}))]
    (d/execute!
      (:db system)
      (sql/format {:insert-into [:categories]
                   :values      [{:name "Food"}
                                 {:name "Entertainment"}
                                 {:name "Bills"}
                                 {:name "Salary"}]}))
    (component/stop system)))

(defn migrate-down [{:keys [db]}]
  (let [db-spec (dissoc (assoc db :username (:user db)) :user)
        system  (component/start-system (create-system {:db-spec db-spec}))]
    (d/execute! (:db system) (sql/format {:truncate :categories}))
    (component/stop system)))

(comment
  )