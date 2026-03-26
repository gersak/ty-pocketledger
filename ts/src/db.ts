/**
 * SQLite database layer using better-sqlite3.
 */

import Database from 'better-sqlite3';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dbPath = path.resolve(__dirname, '../../..', 'pocketledger.db');

const db = new Database(dbPath);

// Enable WAL mode for better concurrency
db.pragma('journal_mode = WAL');

export function init(): void {
  db.exec(`
    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    )
  `);

  db.exec(`
    CREATE TABLE IF NOT EXISTS categories (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL UNIQUE,
      icon TEXT DEFAULT 'tag'
    )
  `);

  db.exec(`
    CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      description TEXT NOT NULL,
      amount REAL NOT NULL,
      type TEXT NOT NULL DEFAULT 'expense',
      category_id INTEGER REFERENCES categories(id),
      category_ids TEXT DEFAULT '',
      date TEXT NOT NULL DEFAULT (date('now')),
      created_at TEXT NOT NULL DEFAULT (datetime('now'))
    )
  `);

  // Migration: add category_ids column if missing
  try {
    db.exec(`ALTER TABLE transactions ADD COLUMN category_ids TEXT DEFAULT ''`);
  } catch {
    // Column already exists
  }
}

export function seedDefaults(): void {
  const count = db.prepare('SELECT count(*) as count FROM categories').get() as { count: number };

  if (count.count === 0) {
    const insert = db.prepare('INSERT INTO categories (name, icon) VALUES (?, ?)');
    const defaults: [string, string][] = [
      ['Food', 'utensils'],
      ['Transport', 'car'],
      ['Housing', 'house'],
      ['Entertainment', 'film'],
      ['Health', 'heart-pulse'],
      ['Shopping', 'shopping-bag'],
      ['Utilities', 'zap'],
      ['Other', 'tag']
    ];
    for (const [name, icon] of defaults) {
      insert.run(name, icon);
    }
  }
}

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

export function getSetting(key: string): string | undefined {
  const row = db.prepare('SELECT value FROM settings WHERE key = ?').get(key) as { value: string } | undefined;
  return row?.value;
}

export function setSetting(key: string, value: string): void {
  db.prepare(`
    INSERT INTO settings (key, value) VALUES (?, ?)
    ON CONFLICT(key) DO UPDATE SET value = excluded.value
  `).run(key, value);
}

export function setupComplete(): boolean {
  return getSetting('setup_complete') === 'true';
}

export function getAllSettings(): Record<string, string> {
  const rows = db.prepare('SELECT key, value FROM settings').all() as { key: string; value: string }[];
  return Object.fromEntries(rows.map(r => [r.key, r.value]));
}

// ---------------------------------------------------------------------------
// Categories
// ---------------------------------------------------------------------------

export interface Category {
  id: number;
  name: string;
  icon: string;
}

export function getCategories(): Category[] {
  return db.prepare('SELECT * FROM categories ORDER BY name').all() as Category[];
}

export function addCategory(name: string): void {
  db.prepare('INSERT OR IGNORE INTO categories (name) VALUES (?)').run(name);
}

export function deleteCategory(id: number): void {
  db.prepare('DELETE FROM categories WHERE id = ?').run(id);
}

// ---------------------------------------------------------------------------
// Transactions
// ---------------------------------------------------------------------------

export interface Transaction {
  id: number;
  description: string;
  amount: number;
  type: 'expense' | 'income';
  category_id: number | null;
  category_ids: string;
  date: string;
  created_at: string;
  categories?: Category[];
}

function categoriesById(): Record<number, Category> {
  const cats = getCategories();
  return Object.fromEntries(cats.map(c => [c.id, c]));
}

function resolveCategoryNames(categoryIds: string): Category[] {
  if (!categoryIds || categoryIds === '') return [];
  const cats = categoriesById();
  const ids = categoryIds.split(',').map(Number);
  return ids.map(id => cats[id]).filter(Boolean);
}

export function getTransactions(options: { limit?: number; offset?: number } = {}): Transaction[] {
  const { limit = 50, offset = 0 } = options;
  const txns = db.prepare(`
    SELECT t.*
    FROM transactions t
    ORDER BY t.date DESC, t.created_at DESC
    LIMIT ? OFFSET ?
  `).all(limit, offset) as Transaction[];

  return txns.map(tx => ({
    ...tx,
    categories: resolveCategoryNames(tx.category_ids)
  }));
}

export function addTransaction(data: {
  description: string;
  amount: number;
  type?: string;
  category_ids?: string;
  date?: string;
}): void {
  const date = data.date || new Date().toISOString().split('T')[0];
  db.prepare(`
    INSERT INTO transactions (description, amount, type, category_ids, date)
    VALUES (?, ?, ?, ?, ?)
  `).run(
    data.description,
    data.amount,
    data.type || 'expense',
    data.category_ids || '',
    date
  );
}

export function deleteTransaction(id: number): void {
  db.prepare('DELETE FROM transactions WHERE id = ?').run(id);
}

export function resetAll(): void {
  db.exec('DELETE FROM transactions');
  db.exec('DELETE FROM settings');
  db.exec('DELETE FROM categories');
  seedDefaults();
}

export interface Summary {
  budget: number;
  expenses: number;
  income: number;
  remaining: number;
  count: number;
}

export function getSummary(): Summary {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;
  const start = `${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-01`;
  const nextMonth = month === 12 ? 1 : month + 1;
  const nextYear = month === 12 ? year + 1 : year;
  const end = `${nextYear.toString().padStart(4, '0')}-${nextMonth.toString().padStart(2, '0')}-01`;

  const budget = parseFloat(getSetting('monthly_budget') || '0') || 0;

  const result = db.prepare(`
    SELECT
      COALESCE(SUM(CASE WHEN type='expense' THEN amount ELSE 0 END), 0) as expenses,
      COALESCE(SUM(CASE WHEN type='income' THEN amount ELSE 0 END), 0) as income,
      COUNT(*) as count
    FROM transactions
    WHERE date >= ? AND date < ?
  `).get(start, end) as { expenses: number; income: number; count: number };

  return {
    ...result,
    budget,
    remaining: budget - result.expenses
  };
}
