(ns pocketledger.handlers
  (:require [pocketledger.db :as db]
            [pocketledger.sse :as sse]
            [pocketledger.views :as views]
            [clojure.string :as str]))

(defn- parse-date
  "Extract YYYY-MM-DD from a UTC ISO string or date string."
  [s]
  (when (and s (not (str/blank? s)))
    (first (str/split s #"T"))))

;; ---------------------------------------------------------------------------
;; Setup
;; ---------------------------------------------------------------------------

(defn setup [request]
  (let [signals (sse/parse-signals request)
        {:keys [setupName setupCurrency setupBudget]} signals]
    (if (or (empty? setupName) (empty? setupBudget))
      (sse/sse-response
        (sse/patch-elements
          [:div#setup-result.ty-bg-danger-.p-3.rounded-lg.border.ty-border-danger.mt-2
           [:p.ty-text-danger+.text-sm "Please fill in all fields."]]))
      (do
        (db/set-setting! "name" setupName)
        (db/set-setting! "currency" setupCurrency)
        (db/set-setting! "monthly_budget" setupBudget)
        (db/set-setting! "setup_complete" "true")
        ;; Redirect to app via Datastar
        (sse/sse-response
          (sse/sse-event "datastar-execute-script"
                         ["script window.location.href = '/';"]))))))

;; ---------------------------------------------------------------------------
;; Dashboard
;; ---------------------------------------------------------------------------

(defn dashboard [_request]
  (sse/sse-response
    (sse/patch-elements
      [:div#dashboard.space-y-6
       (views/summary-fragment)
       [:h3.text-lg.font-semibold.ty-text++.mt-4 "Recent Transactions"]
       (views/transaction-list-fragment)])))

;; ---------------------------------------------------------------------------
;; Transactions
;; ---------------------------------------------------------------------------

(defn add-transaction [request]
  (let [signals (sse/parse-signals request)
        {:keys [txDesc txAmount txType txCategory txDate]} signals
        amount (some-> txAmount str parse-double)]
    (if (or (empty? txDesc) (nil? amount) (zero? amount))
      (sse/sse-response
        (sse/patch-elements
          [:div#add-result.ty-bg-danger-.p-3.rounded-lg.border.ty-border-danger.mt-2
           [:p.ty-text-danger+.text-sm "Please enter a description and amount."]]))
      (do
        (db/add-transaction!
          {:description txDesc
           :amount amount
           :type (or txType "expense")
           :category_ids (when (and txCategory (not= txCategory ""))
                           txCategory)
           :date (parse-date txDate)})
        (sse/sse-response
          (sse/patch-elements
            [:div#add-result.ty-bg-success-.p-3.rounded-lg.border.ty-border-success.mt-2
             [:p.ty-text-success+.text-sm "Transaction added!"]])
          (sse/patch-elements (views/summary-fragment))
          (sse/patch-elements (views/transaction-list-fragment))
          (sse/patch-signals {:txDesc "" :txAmount "" :txCategory "" :txDate ""}))))))

(defn delete-transaction [request]
  (let [id (some-> (get-in request [:params :id]) parse-long)]
    (when id (db/delete-transaction! id))
    (sse/sse-response
      (sse/patch-elements (views/summary-fragment))
      (sse/patch-elements (views/transaction-list-fragment)))))

;; ---------------------------------------------------------------------------
;; Settings
;; ---------------------------------------------------------------------------

(defn save-settings [request]
  (let [signals (sse/parse-signals request)
        {:keys [settingsName settingsCurrency settingsBudget]} signals]
    (when settingsName (db/set-setting! "name" settingsName))
    (when settingsCurrency (db/set-setting! "currency" settingsCurrency))
    (when settingsBudget (db/set-setting! "monthly_budget" settingsBudget))
    (sse/sse-response
      (sse/patch-elements
        [:div#settings-result.ty-bg-success-.p-3.rounded-lg.border.ty-border-success.mt-2
         [:p.ty-text-success+.text-sm "Settings saved!"]]))))

(defn reset-data [_request]
  (db/reset-all!)
  (sse/sse-response
    (sse/sse-event "datastar-execute-script"
                   ["script window.location.href = '/';"])))
