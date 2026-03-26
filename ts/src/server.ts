/**
 * Hono server for PocketLedger TypeScript backend.
 */

import { serve } from "@hono/node-server";
import { Hono } from "hono";
import { logger } from "hono/logger";

import * as db from "./db.js";
import * as sse from "./sse.js";
import * as views from "./views.js";

const app = new Hono();

// Middleware
app.use("*", logger());

// ---------------------------------------------------------------------------
// Pages
// ---------------------------------------------------------------------------

app.get("/", (c) => {
  return c.html(views.index());
});

// ---------------------------------------------------------------------------
// API: Setup
// ---------------------------------------------------------------------------

app.post("/api/setup", async (c) => {
  const signals = await sse.parseSignals(c.req.raw);
  const { setupName, setupCurrency, setupBudget } = signals as {
    setupName?: string;
    setupCurrency?: string;
    setupBudget?: string;
  };

  if (!setupName || !setupBudget) {
    return sse.sseResponse(
      sse.patchElements(
        `<div id="setup-result" class="ty-bg-danger- p-3 rounded-lg border ty-border-danger mt-2">
          <p class="ty-text-danger+ text-sm">Please fill in all fields.</p>
        </div>`,
      ),
    );
  }

  db.setSetting("name", setupName);
  db.setSetting("currency", setupCurrency || "EUR");
  db.setSetting("monthly_budget", setupBudget);
  db.setSetting("setup_complete", "true");

  return sse.sseResponse(sse.redirect("/"));
});

// ---------------------------------------------------------------------------
// API: Dashboard
// ---------------------------------------------------------------------------

app.get("/api/dashboard", () => {
  return sse.sseResponse(
    sse.patchElements(
      `<div id="dashboard" class="space-y-6">
        ${views.summaryFragment()}
        <h3 class="text-lg font-semibold ty-text++ mt-4">Recent Transactions</h3>
        ${views.transactionListFragment()}
      </div>`,
    ),
  );
});

// ---------------------------------------------------------------------------
// API: Transactions
// ---------------------------------------------------------------------------

function parseDate(s: string | undefined): string | undefined {
  if (!s || s.trim() === "") return undefined;
  return s.split("T")[0];
}

app.post("/api/transactions/add", async (c) => {
  const signals = await sse.parseSignals(c.req.raw);
  const { txDesc, txAmount, txType, txCategory, txDate } = signals as {
    txDesc?: string;
    txAmount?: string | number;
    txType?: string;
    txCategory?: string;
    txDate?: string;
  };

  const amount =
    typeof txAmount === "number"
      ? txAmount
      : parseFloat(String(txAmount || "0"));

  if (!txDesc || !amount || amount === 0) {
    return sse.sseResponse(
      sse.patchElements(
        `<div id="add-result" class="ty-bg-danger- p-3 rounded-lg border ty-border-danger mt-2">
          <p class="ty-text-danger+ text-sm">Please enter a description and amount.</p>
        </div>`,
      ),
    );
  }

  db.addTransaction({
    description: txDesc,
    amount,
    type: txType || "expense",
    category_ids: txCategory && txCategory !== "" ? txCategory : undefined,
    date: parseDate(txDate),
  });

  return sse.sseResponse(
    sse.patchElements(
      `<div id="add-result" class="ty-bg-success- p-3 rounded-lg border ty-border-success mt-2">
        <p class="ty-text-success+ text-sm">Transaction added!</p>
      </div>`,
    ),
    sse.patchElements(views.summaryFragment()),
    sse.patchElements(views.transactionListFragment()),
    sse.patchSignals({
      txDesc: "",
      txAmount: "",
      txCategory: "",
      txDate: "",
    }),
  );
});

app.post("/api/transactions/delete", async (c) => {
  const url = new URL(c.req.url);
  const id = parseInt(url.searchParams.get("id") || "0", 10);

  if (id) {
    db.deleteTransaction(id);
  }

  return sse.sseResponse(
    sse.patchElements(views.summaryFragment()),
    sse.patchElements(views.transactionListFragment()),
  );
});

// ---------------------------------------------------------------------------
// API: Settings
// ---------------------------------------------------------------------------

app.post("/api/settings", async (c) => {
  const signals = await sse.parseSignals(c.req.raw);
  const { settingsName, settingsCurrency, settingsBudget } = signals as {
    settingsName?: string;
    settingsCurrency?: string;
    settingsBudget?: string;
  };

  if (settingsName) db.setSetting("name", settingsName);
  if (settingsCurrency) db.setSetting("currency", settingsCurrency);
  if (settingsBudget) db.setSetting("monthly_budget", settingsBudget);

  return sse.sseResponse(
    sse.patchElements(
      `<div id="settings-result" class="ty-bg-success- p-3 rounded-lg border ty-border-success mt-2">
        <p class="ty-text-success+ text-sm">Settings saved!</p>
      </div>`,
    ),
  );
});

app.post("/api/reset", () => {
  db.resetAll();
  return sse.sseResponse(sse.redirect("/"));
});

// ---------------------------------------------------------------------------
// Start server
// ---------------------------------------------------------------------------

const PORT = parseInt(process.env.PORT || "3000", 10);

// Initialize database
db.init();
db.seedDefaults();

console.log(
  `PocketLedger TypeScript server starting on http://localhost:${PORT}`,
);

serve({
  fetch: app.fetch,
  port: PORT,
  hostname: "0.0.0.0",
});
