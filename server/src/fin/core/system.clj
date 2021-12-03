(ns fin.core.system
  (:require
    [fin.core.application.api.entry :refer [make-api]]
    [fin.core.application.grpc.server :refer [make-server]]
    [fin.core.domain.application-service :refer [make-application-service]]
    [fin.core.infrastructure.persistence.mysql-repository :refer [make-repository]]
    [fin.core.infrastructure.persistence.db :refer [make-db]]

    [com.stuartsierra.component :as component]
    [next.jdbc.connection :as connection])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defn create-system [db-spec]
  (-> (component/system-map
        :ds (connection/component HikariDataSource db-spec)
        :db (make-db)
        :repository (make-repository)
        :application-service (make-application-service)
        :api (make-api {:port  4000
                        :join? false})
        :grpc-server (make-server {:port 50055}))
      (component/system-using
        {:db                  {:ds :ds}
         :repository          {:db :db}
         :application-service {:repository :repository}
         :api                 {:application-service :application-service}
         :grpc-server         {:application-service :application-service}})))

(def ^:private db-spec {:dbtype "mysql" :dbname "fin" :username "root" :password "password"})

(defonce the-system nil)

(defn init []
  (alter-var-root
    #'the-system
    (constantly
      (create-system db-spec))))

(defn start []
  (alter-var-root #'the-system component/start))

(defn stop []
  (alter-var-root #'the-system #(when % (component/stop %))))

(defn go []
  (init)
  (start))

(comment
  (go)
  (stop)
  (:repository the-system)

  (require 'fin.core.infrastructure.persistence.db)
  (require 'fin.core.infrastructure.persistence.mysql-repository)
  (require 'honey.sql)
  (require 'fin.core.domain.application-service)
  (require 'fin.core.domain.repository)

  (require 'tick.core)
  (require 'sc.api)

  (fin.core.domain.migration-service/update-transaction-if-is-internal!
    (:migration-service the-system)
    192
    )

  (fin.core.domain.application-service/find-transaction-by-id
    (:application-service the-system)
    314
    )

  (fin.core.domain.application-service/create-transaction
    (:application-service the-system)
    {:amount           -7.67M
     :description      "DIGITALOCEAN.COM NEW YORK CITY NYz"
     :transaction-date (tick.core/date "2021-10-01")})


  (fin.core.domain.repository/find-all-transactions
    (:repository the-system)
    )

  (fin.core.domain.application-service/create-transaction
    (:application-service the-system)
    {:description      "Test transaction"
     :amount           -20
     :transaction-date (tick.core/date "2021-10-15")
     :account-number   "1234"
     :source           "test"})

  (fin.core.domain.application-service/get-transaction-summary
    (:application-service the-system)
    (tick.core/date "2021-09-01")
    (tick.core/date "2021-10-15"))

  (fin.core.domain.application-service/find-transactions-between-dates
    (:application-service the-system)
    (tick.core/date "2021-09-01")
    (tick.core/date "2021-10-01"))

  (fin.core.domain.repository/find-transactions-between-dates
    (:repository the-system)
    (tick.core/date "2021-09-14")
    (tick.core/date "2021-09-15"))

  (fin.core.domain.repository/find-matching-transaction
    (:repository the-system)
    {:amount           12M
     :description      "test"
     :transaction-date (tick.core/date "2021-09-15")})

  (def txn-cats
    (fin.core.infrastructure.persistence.db/execute!
      (:db the-system)
      (honey.sql/format
        {:select [:*]
         :from   :transactions_categories
         :join   [:categories [:= :transactions_categories.category_id :categories.id]]
         :where  [:in :transactions_categories.transaction_id [1 314]]})))

  (group-by :transactions_categories/transaction_id txn-cats)

  (fin.core.infrastructure.persistence.db/execute!
    (:db the-system)
    (honey.sql/format
      {:select :*
       :from   :transactions
       :limit  1}))
  )