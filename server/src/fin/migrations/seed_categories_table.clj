(ns fin.migrations.seed-categories-table
  (:require
    [fin.components.db :refer [make-db]]
    [fin.protocols :as p]
    [fin.queries :as queries]

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
    (queries/insert!
      (:db system)
      :categories
      {:name "Food"}
      {:name "Entertainment"}
      {:name "Bills"}
      {:name "Salary"})
    (component/stop system)))

(defn migrate-down [{:keys [db]}]
  (let [db-spec (dissoc (assoc db :username (:user db)) :user)
        system  (component/start-system (create-system {:db-spec db-spec}))]
    (p/query (:db system) (sql/format {:truncate :categories}))
    (component/stop system)))

(comment
  )