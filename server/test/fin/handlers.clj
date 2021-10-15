(ns fin.handlers
  (:require [clojure.test :refer :all]
            [fin.handlers :refer [get-transaction-summary]]
            [fin.factories :as f]
            [fin.protocols :as p]
            [pjstadig.humane-test-output :as humane]
            [spy.core :as spy]
            [spy.protocol :as protocol]
            [tick.core :as t]
            [utils.test :refer [it]]))

(def internal-transactions
  [(f/Transaction {:amount 1M :transaction_date (t/date "2021-08-20")}) ;; should be internal
   (f/Transaction {:amount -1M :transaction_date (t/date "2021-08-20")}) ;; should be internal
   (f/Transaction {:description      "ONLINE SERVICE CENTRE ACCOUNT PAYMENT" ;; should be internal
                   :amount           20M
                   :transaction_date (t/date "2021-08-20")})])

(def external-transactions
  [(f/Transaction {:amount -2M :transaction_date (t/date "2021-08-21")})
   (f/Transaction {:amount 2M :transaction_date (t/date "2021-08-20")})
   (f/Transaction {:amount -4M :transaction_date (t/date "2021-08-20")})])

(def stub-transactions (concat internal-transactions external-transactions))

(deftest get-transaction-summary-test
  (let [transaction-repository (protocol/spy
                                 p/ITransactionRepository
                                 (reify p/ITransactionRepository
                                   (find-between-dates [_ _ _]
                                     stub-transactions)))

        find-between-dates-spy (:find-between-dates (protocol/spies transaction-repository))

        req                    {:params {:from "2021-08-19T00:00:00Z"
                                         :to   "2021-09-19T00:00:00Z"}
                                :repo-registry
                                        {:transaction-repository transaction-repository}}

        result                 (get-transaction-summary req)]

    (testing "with a valid request"
      (it "returns the correct response body"
          (= {:status 200
              :body   {:stats        {:in      23M
                                      :out     7M
                                      :in-ext  2M
                                      :out-ext 6M
                                      :delta   -4M}
                       :transactions external-transactions}}
             result))

      (it "calls the find-between-dates with the correct date range"
          (spy/called-once-with?
            find-between-dates-spy
            transaction-repository
            (t/zoned-date-time "2021-08-19T00:00:00Z")
            (t/zoned-date-time "2021-09-19T00:00:00Z"))))))

(comment
  (humane/activate!)
  (run-tests)
  )