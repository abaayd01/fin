(ns fin.infrastructure.persistence.migrations.clean-transaction-descriptions
  (:require
    [fin.infrastructure.persistence.db :refer [make-db] :as d]

    [clojure.string :as string]
    [com.stuartsierra.component :as component]
    [next.jdbc.connection :as connection]
    [honey.sql :as sql]
    [tick.core :as t])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defn- clean-description-str [str]
  (-> str
      string/trim
      (string/replace #"\s+" " ")
      (string/replace #"\"" "")
      string/upper-case))

(defn- create-system [{:keys [db-spec]}]
  (-> (component/system-map
        :ds (connection/component HikariDataSource db-spec)
        :db (make-db))
      (component/system-using
        {:db {:ds :ds}})))

(defn migrate-up [{:keys [db]}]
  (let [db-spec      (dissoc (assoc db :username (:user db)) :user)
        system       (component/start-system (create-system {:db-spec db-spec}))
        transactions (d/execute!
                       (:db system)
                       (sql/format
                         {:select :*
                          :from   :transactions}))]
    (doseq [{:keys [description] :as transaction} transactions]
      (let [updated-description (clean-description-str description)
            updated-transaction (assoc transaction :description updated-description)]
        (d/execute!
          (:db system)
          {:update :transactions
           :set    (-> updated-transaction
                       (dissoc :id)
                       (assoc :updated_at (t/date-time)))
           :where  [:= :id (:id updated-transaction)]})))
    (component/stop system)))

(defn migrate-down [_]
  (throw (Exception. "irreversible migration")))
