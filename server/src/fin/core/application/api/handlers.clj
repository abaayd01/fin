(ns fin.core.application.api.handlers
  (:require
    [fin.core.domain.application-service :as service]
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

(defn get-transaction
  [req]
  (let [application-service (:application-service req)
        transaction-id      (get-in req [:parameters :path :transaction-id])]
    {:status 200
     :body   (service/find-transaction-by-id
               application-service
               transaction-id)}))

(defn tag-transaction
  [req]
  (let [application-service (:application-service req)
        transaction-id      (get-in req [:parameters :path :transaction-id])
        categories          (get-in req [:parameters :body :categories])]
    {:status 200
     :body   (service/tag-transaction-with-categories
               application-service
               transaction-id
               categories)}))
