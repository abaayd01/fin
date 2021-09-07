(ns fin.schemas
  (:require [malli.core :as m]
            [tick.core :as t]
            [clojure.test.check.generators :as gen]
            [malli.generator :as mg]))

(def date?
  (m/-simple-schema
    {:type            :date
     :pred            t/date?
     :type-properties {:gen/gen (gen/return (t/date "2020-01-01"))}}))

(def Transaction
  [:map
   [:description string?]
   [:amount decimal?]
   [:transaction_date date?]
   [:source string?]])

(def Date date?)
(def Transactions [:sequential Transaction])
