(ns fin.core.domain.transaction-service)

(def ^:private custom-internal-transaction-rules
  [#"ONLINE SERVICE CENTRE ACCOUNT PAYMENT.*" #"DIRECT DEBIT 28 DEGREES.*"])

(defn- does-match-custom-internal-transaction-rules [transaction]
  (some #(re-find % (:description transaction)) custom-internal-transaction-rules))

(defn is-internal?
  [transaction matched-transaction]
  (boolean (or matched-transaction
               (does-match-custom-internal-transaction-rules transaction))))
