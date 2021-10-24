(ns fin.application.api.handlers
  (:require
    [fin.core.application-service :as service]
    [tick.core :as t]
    ))

(defn get-categories
  [{:keys [application-service]}]
  {:status 200
   :body   (service/find-all-categories application-service)})

(defn get-transaction-summary
  [req]
  (let [application-service (:application-service req)
        {:keys [from to]} (->> (-> req :params (select-keys [:from :to]))
                               (into
                                 {}
                                 (map
                                   (fn [[k v]]
                                     (hash-map k (t/date (t/offset-date-time v)))))))]
    {:status 200
     :body   {:transaction-summary (service/get-transaction-summary
                                     application-service
                                     from
                                     to)
              :transactions        (service/find-transactions-between-dates
                                     application-service
                                     from
                                     to)}}))
