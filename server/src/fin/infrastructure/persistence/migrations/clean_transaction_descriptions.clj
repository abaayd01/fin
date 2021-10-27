(ns fin.infrastructure.persistence.migrations.clean-transaction-descriptions
  (:require
    [fin.components.db :refer [make-db]]
    [fin.components.transaction-repository :refer [make-transaction-repository]]
    [fin.protocols :as p]

    [clojure.string :as string]
    [com.stuartsierra.component :as component]
    [next.jdbc.connection :as connection])
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
        :db (make-db)
        :transaction-repository (make-transaction-repository :transactions))
      (component/system-using
        {:db                     {:ds :ds}
         :transaction-repository {:db :db}})))

(defn migrate-up [{:keys [db]}]
  (let [db-spec      (dissoc (assoc db :username (:user db)) :user)
        system       (component/start-system (create-system {:db-spec db-spec}))
        transactions (p/find-where (:transaction-repository system) [])]
    (doseq [{:keys [description] :as transaction} transactions]
      (let [updated-description (clean-description-str description)]
        (p/update!
          (:transaction-repository system)
          (assoc transaction :description updated-description))))
    (component/stop system)))

(defn migrate-down [_]
  (throw (Exception. "irreversible migration")))
