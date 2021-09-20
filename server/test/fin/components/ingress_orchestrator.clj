(ns fin.components.ingress-orchestrator
  (:require
    [clojure.test :refer :all]
    [com.stuartsierra.component :as component]
    [fin.components.ingress-orchestrator :refer [make-ingress-orchestrator]]
    [fin.factories :as f]
    [fin.protocols :as p]
    [fin.schemas :as s]
    [malli.core :as m]
    [malli.generator :as mg]
    [malli.transform :as mt]
    [spy.core :as spy]
    [spy.protocol :as protocol]
    [tick.core :as t]
    [utils.test :refer [it]]))

(deftest min-by-transaction-date-test
  (testing "with transactions"
    #_:clj-kondo/ignore
    (let [fn     #'fin.components.ingress-orchestrator/min-by-transaction-date
          input  [(f/Transaction {:transaction_date (t/date "2021-10-18")})
                  (f/Transaction {:transaction_date (t/date "2020-10-18")})
                  (f/Transaction {:transaction_date (t/date "2022-10-18")})]
          result (fn input)]
      (is (= (input 1) result)))))

(deftest max-by-transaction-date-test
  (testing "with transactions"
    #_:clj-kondo/ignore
    (let [fn     #'fin.components.ingress-orchestrator/max-by-transaction-date
          input  [(f/Transaction {:transaction_date (t/date "2022-10-18")})
                  (f/Transaction {:transaction_date (t/date "2020-10-18")})
                  (f/Transaction {:transaction_date (t/date "2021-10-18")})]
          result (fn input)]
      (is (= (input 0) result)))))

(deftest diff-transactions-test
  #_:clj-kondo/ignore
  (let [fn #'fin.components.ingress-orchestrator/diff-transactions]
    (testing
      "when both sets of transactions are vectors of exact Transactions"

      (testing
        "and when both sets of transactions are the same"

        (let [transactions (vec (mg/sample s/Transaction))
              result       (fn transactions transactions)]

          (it
            "returns an empty vector."
            (= [] result))))

      (testing
        "and when there are transactions in the first set not in the second set"

        (let [transaction-1 (f/Transaction)
              transaction-2 (f/Transaction)

              a             [transaction-1 transaction-2]
              b             [transaction-2]
              result        (fn a b)]

          (it
            "returns a vector of only the transactions in the first set."

            (= [transaction-1]
               result))
          ))

      (testing
        "and when there are transactions in the second set not in the first set"

        (let [transaction-1 (f/Transaction)
              transaction-2 (f/Transaction)

              a             [transaction-2]
              b             [transaction-1 transaction-2]
              result        (fn a b)]

          (it
            "returns a vector of only the transactions in the first set (an empty vector)."
            (= [] result))
          ))

      (testing
        "and when there are duplicates in the first set"

        (let [transaction-1 (f/Transaction)
              transaction-2 (f/Transaction)

              a             [transaction-1 transaction-1 transaction-2]
              b             [transaction-2]
              result        (fn a b)]

          (it
            "returns a vector of transactions only in the first set, allowing duplicates."

            (= [transaction-1 transaction-1]
               result))
          )))

    (testing
      "when both sets of transactions have extra keys, and are non-homogenous"

      (testing
        "and when there are transactions in the first set not in the second set"

        (let [transaction-1  (f/Transaction {:another-key "abc"})
              transaction-2a (f/Transaction {:description "tx2a" :junk "who put this here?"})
              transaction-2b (-> transaction-2a
                                 (dissoc :junk)
                                 (assoc :date :bogus))

              a              [transaction-1 transaction-2a]
              b              [transaction-2b]

              result         (fn a b)]

          (it
            "returns a vector of only the transactions in the first set, with the extra keys stripped."

            (= [(m/decode s/Transaction transaction-1 mt/strip-extra-keys-transformer)]
               result))
          ))
      )))

(deftest ingress-orchestrator-test
  (let
    [t1                      (f/Transaction {:transaction_date (t/date "2020-01-01")})
     t2                      (f/Transaction)
     t3                      (f/Transaction {:transaction_date (t/date "2022-01-01")})

     ingress-txns            [t1 t3]
     db-txns                 [t1 t2]

     transaction-repo-spy    (protocol/spy
                               p/ITransactionRepository
                               (reify p/ITransactionRepository
                                 (find-between-dates [_ _ _] db-txns)
                                 (insert-transaction! [_ _])))

     find-between-dates-spy  (:find-between-dates (protocol/spies transaction-repo-spy))
     insert-transaction!-spy (:insert-transaction! (protocol/spies transaction-repo-spy))

     ingress-service-spy     (protocol/spy
                               p/IIngressService
                               (reify p/IIngressService
                                 (get-transactions [_] ingress-txns)))

     get-transactions-spy    (:get-transactions (protocol/spies ingress-service-spy))

     system                  (component/start
                               (-> (component/system-map
                                     :transaction-repository transaction-repo-spy
                                     :ingress-service ingress-service-spy
                                     :ingress-orchestrator (make-ingress-orchestrator))

                                   (component/system-using
                                     {:ingress-orchestrator
                                      {:transaction-repository :transaction-repository
                                       :ingress-service        :ingress-service}})))]
    (testing
      "run (integration style smoke test)"
      (p/run (:ingress-orchestrator system))

      (it
        "should call get-transactions on the ingress-service once"
        (spy/called-once-with?
          get-transactions-spy
          ingress-service-spy))

      (it
        "should call find-between-dates on the transaction-repo with the min / max date in ingress-txns once"
        (spy/called-once-with?
          find-between-dates-spy
          transaction-repo-spy
          (:transaction_date t1)
          (:transaction_date t3)))

      (it
        "should call insert-transactions! once for transaction 3"
        (spy/called-once-with?
          insert-transaction!-spy
          transaction-repo-spy
          t3))

      (component/stop system))))

(comment
  (run-tests)
  (run-all-tests #"fin.*")
  )