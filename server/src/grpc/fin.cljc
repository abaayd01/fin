;;;----------------------------------------------------------------------------------
;;; Generated by protoc-gen-clojure.  DO NOT EDIT
;;;
;;; Message Implementation of package grpc.fin
;;;----------------------------------------------------------------------------------
(ns grpc.fin
  (:require [protojure.protobuf.protocol :as pb]
            [protojure.protobuf.serdes.core :as serdes.core]
            [protojure.protobuf.serdes.complex :as serdes.complex]
            [protojure.protobuf.serdes.utils :refer [tag-map]]
            [protojure.protobuf.serdes.stream :as serdes.stream]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]))

;;----------------------------------------------------------------------------------
;;----------------------------------------------------------------------------------
;; Forward declarations
;;----------------------------------------------------------------------------------
;;----------------------------------------------------------------------------------

(declare cis->GetTransactionsRequest)
(declare ecis->GetTransactionsRequest)
(declare new-GetTransactionsRequest)
(declare cis->Transaction)
(declare ecis->Transaction)
(declare new-Transaction)
(declare cis->GetTransactionsResponse)
(declare ecis->GetTransactionsResponse)
(declare new-GetTransactionsResponse)


;;----------------------------------------------------------------------------------
;;----------------------------------------------------------------------------------
;; Message Implementations
;;----------------------------------------------------------------------------------
;;----------------------------------------------------------------------------------

;-----------------------------------------------------------------------------
; GetTransactionsRequest
;-----------------------------------------------------------------------------
(defrecord GetTransactionsRequest-record []
  pb/Writer
  (serialize [this os]
)
  pb/TypeReflection
  (gettype [this]
    "grpc.fin.GetTransactionsRequest"))

(s/def ::GetTransactionsRequest-spec (s/keys :opt-un []))
(def GetTransactionsRequest-defaults {})

(defn cis->GetTransactionsRequest
  "CodedInputStream to GetTransactionsRequest"
  [is]
  (->> (tag-map GetTransactionsRequest-defaults
         (fn [tag index]
             (case index
               [index (serdes.core/cis->undefined tag is)]))
         is)
        (map->GetTransactionsRequest-record)))

(defn ecis->GetTransactionsRequest
  "Embedded CodedInputStream to GetTransactionsRequest"
  [is]
  (serdes.core/cis->embedded cis->GetTransactionsRequest is))

(defn new-GetTransactionsRequest
  "Creates a new instance from a map, similar to map->GetTransactionsRequest except that
  it properly accounts for nested messages, when applicable.
  "
  [init]
  {:pre [(if (s/valid? ::GetTransactionsRequest-spec init) true (throw (ex-info "Invalid input" (s/explain-data ::GetTransactionsRequest-spec init))))]}
  (-> (merge GetTransactionsRequest-defaults init)
      (map->GetTransactionsRequest-record)))

(defn pb->GetTransactionsRequest
  "Protobuf to GetTransactionsRequest"
  [input]
  (cis->GetTransactionsRequest (serdes.stream/new-cis input)))

(def ^:protojure.protobuf.any/record GetTransactionsRequest-meta {:type "grpc.fin.GetTransactionsRequest" :decoder pb->GetTransactionsRequest})

;-----------------------------------------------------------------------------
; Transaction
;-----------------------------------------------------------------------------
(defrecord Transaction-record [date description amount source accountNumber]
  pb/Writer
  (serialize [this os]
    (serdes.core/write-String 1  {:optimize true} (:date this) os)
    (serdes.core/write-String 2  {:optimize true} (:description this) os)
    (serdes.core/write-String 3  {:optimize true} (:amount this) os)
    (serdes.core/write-String 4  {:optimize true} (:source this) os)
    (serdes.core/write-String 5  {:optimize true} (:accountNumber this) os))
  pb/TypeReflection
  (gettype [this]
    "grpc.fin.Transaction"))

(s/def :grpc.fin.Transaction/date string?)
(s/def :grpc.fin.Transaction/description string?)
(s/def :grpc.fin.Transaction/amount string?)
(s/def :grpc.fin.Transaction/source string?)
(s/def :grpc.fin.Transaction/accountNumber string?)
(s/def ::Transaction-spec (s/keys :opt-un [:grpc.fin.Transaction/date :grpc.fin.Transaction/description :grpc.fin.Transaction/amount :grpc.fin.Transaction/source :grpc.fin.Transaction/accountNumber ]))
(def Transaction-defaults {:date "" :description "" :amount "" :source "" :accountNumber "" })

(defn cis->Transaction
  "CodedInputStream to Transaction"
  [is]
  (->> (tag-map Transaction-defaults
         (fn [tag index]
             (case index
               1 [:date (serdes.core/cis->String is)]
               2 [:description (serdes.core/cis->String is)]
               3 [:amount (serdes.core/cis->String is)]
               4 [:source (serdes.core/cis->String is)]
               5 [:accountNumber (serdes.core/cis->String is)]

               [index (serdes.core/cis->undefined tag is)]))
         is)
        (map->Transaction-record)))

(defn ecis->Transaction
  "Embedded CodedInputStream to Transaction"
  [is]
  (serdes.core/cis->embedded cis->Transaction is))

(defn new-Transaction
  "Creates a new instance from a map, similar to map->Transaction except that
  it properly accounts for nested messages, when applicable.
  "
  [init]
  {:pre [(if (s/valid? ::Transaction-spec init) true (throw (ex-info "Invalid input" (s/explain-data ::Transaction-spec init))))]}
  (-> (merge Transaction-defaults init)
      (map->Transaction-record)))

(defn pb->Transaction
  "Protobuf to Transaction"
  [input]
  (cis->Transaction (serdes.stream/new-cis input)))

(def ^:protojure.protobuf.any/record Transaction-meta {:type "grpc.fin.Transaction" :decoder pb->Transaction})

;-----------------------------------------------------------------------------
; GetTransactionsResponse
;-----------------------------------------------------------------------------
(defrecord GetTransactionsResponse-record [transactions]
  pb/Writer
  (serialize [this os]
    (serdes.complex/write-repeated serdes.core/write-embedded 1 (:transactions this) os))
  pb/TypeReflection
  (gettype [this]
    "grpc.fin.GetTransactionsResponse"))

(s/def ::GetTransactionsResponse-spec (s/keys :opt-un []))
(def GetTransactionsResponse-defaults {:transactions [] })

(defn cis->GetTransactionsResponse
  "CodedInputStream to GetTransactionsResponse"
  [is]
  (->> (tag-map GetTransactionsResponse-defaults
         (fn [tag index]
             (case index
               1 [:transactions (serdes.complex/cis->repeated ecis->Transaction is)]

               [index (serdes.core/cis->undefined tag is)]))
         is)
        (map->GetTransactionsResponse-record)))

(defn ecis->GetTransactionsResponse
  "Embedded CodedInputStream to GetTransactionsResponse"
  [is]
  (serdes.core/cis->embedded cis->GetTransactionsResponse is))

(defn new-GetTransactionsResponse
  "Creates a new instance from a map, similar to map->GetTransactionsResponse except that
  it properly accounts for nested messages, when applicable.
  "
  [init]
  {:pre [(if (s/valid? ::GetTransactionsResponse-spec init) true (throw (ex-info "Invalid input" (s/explain-data ::GetTransactionsResponse-spec init))))]}
  (-> (merge GetTransactionsResponse-defaults init)
      (cond-> (some? (get init :transactions)) (update :transactions #(map new-Transaction %)))
      (map->GetTransactionsResponse-record)))

(defn pb->GetTransactionsResponse
  "Protobuf to GetTransactionsResponse"
  [input]
  (cis->GetTransactionsResponse (serdes.stream/new-cis input)))

(def ^:protojure.protobuf.any/record GetTransactionsResponse-meta {:type "grpc.fin.GetTransactionsResponse" :decoder pb->GetTransactionsResponse})
