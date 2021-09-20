(ns fin.schemas
  (:require [malli.core :as m]
            [tick.core :as t]
            [clojure.test.check.generators :as gen]))

(def date?
  (m/-simple-schema
    {:type            :date
     :pred            t/date?
     :type-properties {:gen/gen (gen/return (t/date "2020-01-01"))}}))

(def zoned-date-time-string?
  (m/-simple-schema
    {:type            :zoned-date-time-string
     :pred            (fn [in]
                        (try (let [[_ month day hrs mins secs _]
                                   (->> in
                                        (re-matches #"(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}).(\d{3})Z")
                                        rest
                                        (map #(Integer/parseInt %)))]
                               (and (and (> month 0) (<= month 12))
                                    (and (> day 0) (<= day 31))
                                    (and (>= hrs 0) (<= hrs 24))
                                    (and (>= mins 0) (<= mins 60))
                                    (and (>= secs 0) (<= secs 60))))

                             (catch Exception _ false)))
     :type-properties {:error/message "invalid zoned date time string"
                       :gen/gen       (gen/return "2020-01-01T00:00:00Z")}}))

(defn Response [body-schema]
  [:map
   [:status int?]
   [:body body-schema]])

(def Transaction
  [:map
   [:description string?]
   [:amount decimal?]
   [:transaction_date date?]
   [:source string?]
   [:account_number string?]])

(def Date date?)
(def Transactions [:sequential Transaction])

(def TransactionSummary
  [:map
   [:in decimal?]
   [:out decimal?]])

(comment
  )