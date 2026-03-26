/**
 * HTML view templates using tagged template literals.
 */

import * as db from './db.js';
import { registrationScript } from './icons.js';

// Simple HTML tagged template - just concatenates strings and values
export const html = (strings: TemplateStringsArray, ...values: unknown[]): string =>
  strings.reduce((acc, str, i) => acc + str + (values[i] ?? ''), '');

// Escape HTML entities
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// ---------------------------------------------------------------------------
// Layout
// ---------------------------------------------------------------------------

export function layout(body: string): string {
  const head = html`
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
      <title>PocketLedger</title>
      <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@gersak/ty@1.0.0-TC2/css/ty.css">
      <script type="module" src="https://cdn.jsdelivr.net/npm/@gersak/ty@1.0.0-TC2/dist/ty.js"></script>
      <script type="module" src="https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-RC.8/bundles/datastar.js"></script>
      <script src="https://cdn.tailwindcss.com"></script>
      ${registrationScript()}
      <style>
        html, body { margin: 0; padding: 0; overflow: hidden; }
        .app-container { display: flex; flex-direction: column; padding-top: env(safe-area-inset-top); }
        .app-container > ty-resize-observer { flex: 1; display: flex; flex-direction: column; min-height: 0; }
        .main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-height: 0; }
        .app-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-height: 0; }
        nav { flex-shrink: 0; }
        footer { flex-shrink: 0; }
        .dark .theme-sun { display: inline-block; }
        .dark .theme-moon { display: none; }
        .theme-sun { display: none; }
        .theme-moon { display: inline-block; }
      </style>
    </head>`;

  const navbar = html`
    <nav class="ty-elevated border-b ty-border">
      <div class="mx-auto max-w-3xl px-4 py-4 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <ty-icon name="wallet" size="md"></ty-icon>
          <h1 class="text-xl font-bold ty-text++">PocketLedger</h1>
        </div>
        <div class="flex items-center gap-2">
          <ty-button size="sm" plain onclick="document.documentElement.classList.toggle('dark'); localStorage.setItem('theme', document.documentElement.classList.contains('dark') ? 'dark' : 'light')">
            <ty-icon class="theme-moon" name="moon" size="sm"></ty-icon>
            <ty-icon class="theme-sun" name="sun" size="sm"></ty-icon>
          </ty-button>
        </div>
      </div>
    </nav>`;

  const footer = html`
    <footer class="ty-content border-t ty-border">
      <div class="mx-auto max-w-3xl px-4 py-4 ty-text- text-sm text-center">
        Built with Ty, Datastar &amp; TypeScript
      </div>
    </footer>`;

  const themeInit = html`
    <script>
      if (localStorage.getItem('theme') === 'dark')
        document.documentElement.classList.add('dark');
    </script>`;

  const autoHeight = html`
    <script>
      document.addEventListener('DOMContentLoaded', function() {
        function setAppHeight() {
          var container = document.querySelector('.app-container');
          if (container) {
            var isMobile = screen.width < 500;
            var h = (isMobile && screen.height > window.innerHeight) ? screen.height : window.innerHeight;
            container.style.height = h + 'px';
          }
        }
        setAppHeight();
        window.addEventListener('resize', setAppHeight);

        var tabs = document.getElementById('app-tabs');
        if (!tabs) return;
        function fitTabs() {
          var rect = tabs.getBoundingClientRect();
          var footer = document.querySelector('footer');
          var footerH = footer ? footer.offsetHeight : 0;
          var bottomPad = 24;
          tabs.setAttribute('height', Math.floor(window.innerHeight - rect.top - footerH - bottomPad) + 'px');
        }
        fitTabs();
        window.tyResizeObserver && window.tyResizeObserver.onResize('app-layout', fitTabs);
        function fitScroll() {
          var sc = document.getElementById('tx-scroll');
          if (sc) {
            var rect = sc.getBoundingClientRect();
            var footer = document.querySelector('footer');
            var footerH = footer ? footer.offsetHeight : 0;
            sc.setAttribute('max-height', Math.floor(window.innerHeight - rect.top - footerH - 24) + 'px');
          }
        }
        fitScroll();
        tabs.addEventListener('ty-tab-change', function() { setTimeout(fitScroll, 100); });
        window.addEventListener('resize', function() { fitTabs(); fitScroll(); });
      });
    </script>`;

  return html`<!DOCTYPE html>
<html lang="en">
${head}
<body class="ty-canvas antialiased">
  <div class="app-container">
    ${navbar}
    <ty-resize-observer id="app-layout">
      <main class="main-content mx-auto max-w-3xl w-full px-4 py-6">
        ${body}
      </main>
    </ty-resize-observer>
    ${footer}
  </div>
  ${themeInit}
  ${autoHeight}
</body>
</html>`;
}

// ---------------------------------------------------------------------------
// Setup wizard
// ---------------------------------------------------------------------------

export function setupPage(): string {
  return layout(html`
    <div class="app-body">
      <div class="max-w-md mx-auto space-y-8 py-8 my-auto">
        <div class="text-center space-y-2">
          <h2 class="text-2xl font-bold ty-text++">Welcome to PocketLedger</h2>
          <p class="ty-text-">Let's set up your expense tracker</p>
        </div>

        <div class="ty-elevated rounded-xl p-6 space-y-6"
             data-signals="{setupName: '', setupCurrency: 'EUR', setupBudget: ''}">

          <div>
            <ty-input data-bind="setupName" label="Your name" placeholder="How should we call you?"></ty-input>
          </div>

          <div>
            <label class="block text-sm font-medium ty-text+ mb-1">Currency</label>
            <ty-dropdown data-bind="setupCurrency" placeholder="Select currency" value="EUR">
              <ty-option value="EUR">EUR — Euro</ty-option>
              <ty-option value="USD">USD — Dollar</ty-option>
              <ty-option value="GBP">GBP — Pound</ty-option>
              <ty-option value="CHF">CHF — Franc</ty-option>
              <ty-option value="HRK">HRK — Kuna</ty-option>
            </ty-dropdown>
          </div>

          <div>
            <ty-input data-bind="setupBudget" label="Monthly budget" type="number" placeholder="e.g. 2000"></ty-input>
          </div>

          <div class="pt-2">
            <ty-button flavor="primary" wide="true" data-on:click="@post('/api/setup')">
              Start tracking
            </ty-button>
          </div>

          <div id="setup-result"></div>
        </div>
      </div>
    </div>
  `);
}

// ---------------------------------------------------------------------------
// Dashboard fragments
// ---------------------------------------------------------------------------

function currencySymbol(currency: string): string {
  const symbols: Record<string, string> = {
    EUR: '\u20ac',
    USD: '$',
    GBP: '\u00a3',
    CHF: 'CHF',
    HRK: 'kn'
  };
  return symbols[currency] || currency;
}

function summaryCard(
  label: string,
  value: string,
  options: { flavor?: string; icon?: string } = {}
): string {
  const { flavor, icon } = options;

  const bgClass = flavor === 'success' ? 'ty-bg-success-' :
                  flavor === 'danger' ? 'ty-bg-danger-' :
                  flavor === 'primary' ? 'ty-bg-primary-' : 'ty-elevated';

  const textClass = flavor === 'success' ? 'ty-text-success+' :
                    flavor === 'danger' ? 'ty-text-danger+' :
                    flavor === 'primary' ? 'ty-text-primary+' : 'ty-text++';

  const borderClass = flavor === 'success' ? 'ty-border-success' :
                      flavor === 'danger' ? 'ty-border-danger' :
                      flavor === 'primary' ? 'ty-border-primary' : 'ty-border';

  return html`
    <div class="${bgClass} ${borderClass} rounded-lg p-4 text-center border-t-2">
      <div class="flex items-center justify-center gap-1 mb-1">
        ${icon ? html`<ty-icon name="${icon}" size="xs" class="${textClass}"></ty-icon>` : ''}
        <p class="text-sm ${textClass}">${label}</p>
      </div>
      <p class="${textClass} text-2xl font-bold">${value}</p>
    </div>
  `;
}

export function summaryFragment(): string {
  const summary = db.getSummary();
  const settings = db.getAllSettings();
  const cur = currencySymbol(settings.currency || 'EUR');

  return html`
    <div id="summary" class="grid grid-cols-2 gap-4">
      ${summaryCard('Budget', `${cur} ${Math.round(summary.budget)}`, { flavor: 'primary', icon: 'wallet' })}
      ${summaryCard('Spent', `${cur} ${Math.round(summary.expenses)}`, { flavor: 'danger', icon: 'trending-down' })}
      ${summaryCard('Income', `${cur} ${Math.round(summary.income)}`, { flavor: 'success', icon: 'trending-up' })}
      ${summaryCard('Remaining', `${cur} ${Math.round(summary.remaining)}`, {
        flavor: summary.remaining < 0 ? 'danger' : 'success',
        icon: 'coins'
      })}
    </div>
  `;
}

function transactionRow(tx: db.Transaction): string {
  const settings = db.getAllSettings();
  const cur = currencySymbol(settings.currency || 'EUR');
  const isExpense = tx.type === 'expense';

  const categoryTags = (tx.categories || []).map(cat => html`
    <ty-tag size="sm" flavor="neutral">
      <ty-icon name="${cat.icon || 'tag'}" size="xs" slot="start"></ty-icon>
      ${escapeHtml(cat.name)}
    </ty-tag>
  `).join('');

  return html`
    <div class="flex items-center justify-between p-3 ty-elevated rounded-lg">
      <div class="flex items-center gap-3 min-w-0">
        <div class="min-w-0">
          <p class="ty-text+ font-medium truncate">${escapeHtml(tx.description)}</p>
          <div class="flex items-center gap-2 flex-wrap">
            ${categoryTags}
            <span class="ty-text- text-xs">${tx.date}</span>
          </div>
        </div>
      </div>
      <div class="flex items-center gap-3 shrink-0">
        <span class="${isExpense ? 'ty-text-danger+' : 'ty-text-success+'} font-semibold">
          ${isExpense ? '\u2212' : '+'}${cur} ${tx.amount.toFixed(2)}
        </span>
        <ty-button size="xs" flavor="danger" plain data-on:click="@post('/api/transactions/delete?id=${tx.id}')">
          <ty-icon name="trash" size="xs"></ty-icon>
        </ty-button>
      </div>
    </div>
  `;
}

export function transactionListFragment(): string {
  const txns = db.getTransactions({ limit: 20 });

  if (txns.length === 0) {
    return html`
      <div id="transaction-list" class="space-y-2 pb-4">
        <div class="text-center py-8">
          <p class="ty-text- italic">No transactions yet. Add one above!</p>
        </div>
      </div>
    `;
  }

  return html`
    <div id="transaction-list" class="space-y-2 pb-4">
      ${txns.map(transactionRow).join('')}
    </div>
  `;
}

// ---------------------------------------------------------------------------
// Main app page
// ---------------------------------------------------------------------------

export function appPage(): string {
  const settings = db.getAllSettings();
  const name = settings.name || '';
  const currency = settings.currency || 'EUR';
  const categories = db.getCategories();

  const categoryOptions = categories.map(cat => html`
    <ty-tag value="${cat.id}">
      <ty-icon name="${cat.icon || 'tag'}" size="xs" slot="start"></ty-icon>
      ${escapeHtml(cat.name)}
    </ty-tag>
  `).join('');

  const tabLabels = [
    ['dashboard', 'Dashboard', 'layout-dashboard'],
    ['add', 'Add', 'plus-circle'],
    ['settings', 'Settings', 'settings']
  ].map(([id, label, icon]) => html`
    <span slot="label-${id}" class="flex items-center gap-1">
      <ty-icon name="${icon}" size="sm"></ty-icon>
      ${label}
    </span>
  `).join('');

  return layout(html`
    <div class="app-body" data-signals="{txDesc: '', txAmount: '', txType: 'expense', txCategory: '', txDate: ''}">

      <div class="shrink-0 pb-4">
        <h2 class="text-xl font-bold ty-text++">Hey, ${escapeHtml(name)}!</h2>
        <p class="ty-text-">Here's your spending overview</p>
      </div>

      <ty-tabs id="app-tabs" active="dashboard" class="ty-content rounded-lg">
        ${tabLabels}

        <ty-tab id="dashboard" label="Dashboard">
          <div class="px-4 pt-4">
            ${summaryFragment()}
            <h3 class="text-lg font-semibold ty-text++ mt-4">Recent Transactions</h3>
            <ty-scroll-container id="tx-scroll" class="mt-2" max-height="400px">
              ${transactionListFragment()}
            </ty-scroll-container>
          </div>
        </ty-tab>

        <ty-tab id="add" label="Add">
          <div class="p-4">
            <div class="ty-elevated rounded-xl p-6 space-y-4">
              <h3 class="text-lg font-semibold ty-text++">New Transaction</h3>

              <div class="flex gap-2">
                <ty-tag size="md" data-attr:flavor="$txType === 'expense' ? 'danger' : 'neutral'" data-on:click="$txType = 'expense'">
                  <ty-icon name="trending-down" size="xs" slot="start"></ty-icon>
                  Expense
                </ty-tag>
                <ty-tag size="md" data-attr:flavor="$txType === 'income' ? 'success' : 'neutral'" data-on:click="$txType = 'income'">
                  <ty-icon name="trending-up" size="xs" slot="start"></ty-icon>
                  Income
                </ty-tag>
              </div>

              <ty-input data-bind="txDesc" label="Description" placeholder="What was it for?"></ty-input>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <ty-input data-bind="txAmount" label="Amount" type="currency" currency="${currency}" placeholder="0.00"></ty-input>
                <ty-date-picker data-attr:value="$txDate" data-on:change="$txDate = evt.detail.value || ''" label="Date" placeholder="Pick a date"></ty-date-picker>
              </div>

              <div>
                <ty-multiselect data-bind="txCategory" label="Categories" placeholder="Select categories">
                  ${categoryOptions}
                </ty-multiselect>
              </div>

              <ty-button flavor="primary" wide="true" data-on:click="@post('/api/transactions/add')">
                <ty-icon slot="start" name="plus" size="sm"></ty-icon>
                Add Transaction
              </ty-button>

              <div id="add-result"></div>
            </div>
          </div>
        </ty-tab>

        <ty-tab id="settings" label="Settings">
          <div class="p-4" data-signals="{settingsName: '${escapeHtml(settings.name || '')}', settingsCurrency: '${settings.currency || 'EUR'}', settingsBudget: '${settings.monthly_budget || ''}'}">
            <div class="ty-elevated rounded-xl p-6 space-y-4">
              <h3 class="text-lg font-semibold ty-text++">Settings</h3>

              <ty-input data-bind="settingsName" label="Your name"></ty-input>

              <div>
                <label class="block text-sm font-medium ty-text+ mb-1">Currency</label>
                <ty-dropdown data-bind="settingsCurrency" value="${settings.currency || 'EUR'}">
                  <ty-option value="EUR">EUR — Euro</ty-option>
                  <ty-option value="USD">USD — Dollar</ty-option>
                  <ty-option value="GBP">GBP — Pound</ty-option>
                  <ty-option value="CHF">CHF — Franc</ty-option>
                  <ty-option value="HRK">HRK — Kuna</ty-option>
                </ty-dropdown>
              </div>

              <ty-input data-bind="settingsBudget" label="Monthly budget" type="number"></ty-input>

              <ty-button flavor="primary" data-on:click="@post('/api/settings')">
                Save Settings
              </ty-button>

              <div id="settings-result"></div>

              <hr class="ty-border my-4">

              <div>
                <h4 class="text-md font-semibold ty-text++">Danger Zone</h4>
                <ty-button flavor="danger" plain class="mt-2" data-on:click="@post('/api/reset')">
                  <ty-icon slot="start" name="trash" size="sm"></ty-icon>
                  Reset All Data
                </ty-button>
                <div id="reset-result"></div>
              </div>
            </div>
          </div>
        </ty-tab>
      </ty-tabs>
    </div>
  `);
}

// ---------------------------------------------------------------------------
// Router entry point
// ---------------------------------------------------------------------------

export function index(): string {
  if (db.setupComplete()) {
    return appPage();
  }
  return setupPage();
}
