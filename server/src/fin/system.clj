(ns fin.system
  (:require
    [fin.application.api.entry :refer [make-api]]
    [fin.core.application-service :refer [make-application-service]]
    [fin.infrastructure.persistence.mysql-repository :refer [make-repository]]
    [fin.infrastructure.persistence.db :refer [make-db]]

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
                        :join? false}))
      (component/system-using
        {:db                  {:ds :ds}
         :repository          {:db :db}
         :application-service {:repository :repository}
         :api                 {:application-service :application-service}})))

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
  )