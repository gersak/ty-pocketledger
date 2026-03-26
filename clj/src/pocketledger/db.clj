(ns pocketledger.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def db-spec {:dbtype "sqlite" :dbname "../pocketledger.db"})

(defonce ds (jdbc/get-datasource db-spec))

(defn init!
  "Create tables if they don't exist."
  []
  (jdbc/execute! ds
    ["CREATE TABLE IF NOT EXISTS settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL)"])
  (jdbc/execute! ds
    ["CREATE TABLE IF NOT EXISTS categories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE,
        icon TEXT DEFAULT 'tag')"])
  (jdbc/execute! ds
    ["CREATE TABLE IF NOT EXISTS transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        description TEXT NOT NULL,
        amount REAL NOT NULL,
        type TEXT NOT NULL DEFAULT 'expense',
        category_id INTEGER REFERENCES categories(id),
        category_ids TEXT DEFAULT '',
        date TEXT NOT NULL DEFAULT (date('now')),
        created_at TEXT NOT NULL DEFAULT (datetime('now')))"])
  ;; Migration: add category_ids column if missing
  (try
    (jdbc/execute! ds ["ALTER TABLE transactions ADD COLUMN category_ids TEXT DEFAULT ''"])
    (catch Exception _))
)

(defn seed-defaults!
  "Seed default categories if none exist."
  []
  (when (zero? (:count (jdbc/execute-one! ds
                         ["SELECT count(*) as count FROM categories"]
                         {:builder-fn rs/as-unqualified-lower-maps})))
    (doseq [[cat icon] [["Food" "utensils"]
                        ["Transport" "car"]
                        ["Housing" "house"]
                        ["Entertainment" "film"]
                        ["Health" "heart-pulse"]
                        ["Shopping" "shopping-bag"]
                        ["Utilities" "zap"]
                        ["Other" "tag"]]]
      (jdbc/execute! ds ["INSERT INTO categories (name, icon) VALUES (?, ?)" cat icon]))
))

;; ---------------------------------------------------------------------------
;; Settings
;; ---------------------------------------------------------------------------

(defn get-setting [key]
  (some-> (jdbc/execute-one! ds
            ["SELECT value FROM settings WHERE key = ?" key]
            {:builder-fn rs/as-unqualified-lower-maps})
          :value))

(defn set-setting! [key value]
  (jdbc/execute! ds
    ["INSERT INTO settings (key, value) VALUES (?, ?)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value"
     key value]))

(defn setup-complete? []
  (= "true" (get-setting "setup_complete")))

(defn get-all-settings []
  (into {}
    (map (juxt :key :value))
    (jdbc/execute! ds
      ["SELECT key, value FROM settings"]
      {:builder-fn rs/as-unqualified-lower-maps})))

;; ---------------------------------------------------------------------------
;; Categories
;; ---------------------------------------------------------------------------

(defn get-categories []
  (jdbc/execute! ds
    ["SELECT * FROM categories ORDER BY name"]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn add-category! [name]
  (jdbc/execute! ds
    ["INSERT OR IGNORE INTO categories (name) VALUES (?)" name]))

(defn delete-category! [id]
  (jdbc/execute! ds
    ["DELETE FROM categories WHERE id = ?" id]))

;; ---------------------------------------------------------------------------
;; Transactions
;; ---------------------------------------------------------------------------

(defn- categories-by-id
  "Return a map of category id -> {:name ... :icon ...}."
  []
  (into {}
    (map (fn [{:keys [id] :as cat}] [id cat]))
    (get-categories)))

(defn- resolve-category-names
  "Given a comma-separated string of category IDs, return category maps."
  [category-ids]
  (when (and category-ids (not= category-ids ""))
    (let [cats (categories-by-id)
          ids (map parse-long (.split category-ids ","))]
      (keep cats ids))))

(defn get-transactions
  ([] (get-transactions {}))
  ([{:keys [limit offset] :or {limit 50 offset 0}}]
   (let [txns (jdbc/execute! ds
                ["SELECT t.*
                  FROM transactions t
                  ORDER BY t.date DESC, t.created_at DESC
                  LIMIT ? OFFSET ?"
                 limit offset]
                {:builder-fn rs/as-unqualified-lower-maps})]
     (mapv #(assoc % :categories (resolve-category-names (:category_ids %))) txns))))

(defn get-transactions-for-month [year month]
  (let [start (format "%04d-%02d-01" year month)
        end (format "%04d-%02d-01" year (if (= month 12) 1 (inc month)))
        end-year (if (= month 12) (inc year) year)]
    (jdbc/execute! ds
      ["SELECT t.*, c.name as category_name
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.id
        WHERE t.date >= ? AND t.date < ?
        ORDER BY t.date DESC"
       start (format "%04d-%02d-01" end-year (if (= month 12) 1 (inc month)))]
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn add-transaction! [{:keys [description amount type category_ids date]}]
  (jdbc/execute! ds
    ["INSERT INTO transactions (description, amount, type, category_ids, date)
      VALUES (?, ?, ?, ?, ?)"
     description amount (or type "expense") (or category_ids "") (or date (.toString (java.time.LocalDate/now)))]))

(defn delete-transaction! [id]
  (jdbc/execute! ds
    ["DELETE FROM transactions WHERE id = ?" id]))

(defn reset-all!
  "Delete all data and re-seed defaults."
  []
  (jdbc/execute! ds ["DELETE FROM transactions"])
  (jdbc/execute! ds ["DELETE FROM settings"])
  (jdbc/execute! ds ["DELETE FROM categories"])
  (seed-defaults!))

(defn get-summary
  "Get spending summary for the current month."
  []
  (let [now (java.time.LocalDate/now)
        year (.getYear now)
        month (.getMonthValue now)
        start (format "%04d-%02d-01" year month)
        end (format "%04d-%02d-01"
              (if (= month 12) (inc year) year)
              (if (= month 12) 1 (inc month)))
        budget (or (some-> (get-setting "monthly_budget") parse-double) 0.0)
        result (jdbc/execute-one! ds
                 ["SELECT
                     COALESCE(SUM(CASE WHEN type='expense' THEN amount ELSE 0 END), 0) as expenses,
                     COALESCE(SUM(CASE WHEN type='income' THEN amount ELSE 0 END), 0) as income,
                     COUNT(*) as count
                   FROM transactions
                   WHERE date >= ? AND date < ?"
                  start end]
                 {:builder-fn rs/as-unqualified-lower-maps})]
    (merge result
           {:budget budget
            :remaining (- budget (:expenses result 0))})))
