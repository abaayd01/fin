(ns fin.infrastructure.persistence.migrations.seed-transaction-category-patterns-table
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

(defn- find-bill-category [db]
  (first (d/execute!
           db
           (sql/format {:select :*
                        :from   :categories
                        :where  [:= :name "Bills"]}))))

(defn- find-food-category [db]
  (first (d/execute!
           db
           (sql/format {:select :*
                        :from   :categories
                        :where  [:= :name "Food"]}))))

(defn migrate-up [{:keys [db]}]
  (let [db-spec       (dissoc (assoc db :username (:user db)) :user)
        system        (component/start-system (create-system {:db-spec db-spec}))
        bill-category (find-bill-category (:db system))
        food-category (find-food-category (:db system))]
    (d/execute!
      (:db system)
      (sql/format {:insert-into :transaction_category_patterns
                   :values      [{:category_id (:id bill-category)
                                  :pattern     "ONLINE SERVICE"}
                                 {:category_id (:id food-category)
                                  :pattern     "food"}]}))
    (component/stop system)))

(defn migrate-down [{:keys [db]}]
  (let [db-spec (dissoc (assoc db :username (:user db)) :user)
        system  (component/start-system (create-system {:db-spec db-spec}))]
    (d/execute! (:db system) (sql/format {:truncate :transaction_category_patterns}))
    (component/stop system)))

(comment
  )