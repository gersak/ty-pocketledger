(ns pocketledger.views
  (:require [hiccup.page :as page]
            [hiccup2.core :as h]
            [pocketledger.db :as db]
            [pocketledger.icons :as icons]))

;; ---------------------------------------------------------------------------
;; Layout
;; ---------------------------------------------------------------------------

(defn layout [& body]
  (let [head
        [:head
         [:meta {:charset "UTF-8"}]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover"}]
         [:title "PocketLedger"]
         [:link {:rel "stylesheet"
                 :href "https://cdn.jsdelivr.net/npm/@gersak/ty@1.0.0-rc.1/css/ty.css"}]
         [:script {:type "module"
                   :src "https://cdn.jsdelivr.net/npm/@gersak/ty@1.0.0-rc.1/dist/ty.js"}]
         [:script {:type "module"
                   :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-RC.8/bundles/datastar.js"}]
         [:script {:src "https://cdn.tailwindcss.com"}]
         (icons/registration-script)
         [:style
          (h/raw
            "html, body { margin: 0; padding: 0; overflow: hidden; }
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
")]]

        navbar
        [:nav.ty-elevated.border-b.ty-border
         [:div.mx-auto.max-w-3xl.px-4.py-4.flex.items-center.justify-between
          [:div.flex.items-center.gap-2
           [:ty-icon {:name "wallet"
                      :size "md"}]
           [:h1.text-xl.font-bold.ty-text++ "PocketLedger"]
           [:a {:href "/test"
                :class "text-xs underline ty-text-"} "dbg"]]
          [:div.flex.items-center.gap-2
           [:ty-button {:size "sm"
                        :plain true
                        :onclick "document.documentElement.classList.toggle('dark');
                                  localStorage.setItem('theme',
                                    document.documentElement.classList.contains('dark') ? 'dark' : 'light')"}
            [:ty-icon.theme-moon {:name "moon"
                                  :size "sm"}]
            [:ty-icon.theme-sun {:name "sun"
                                 :size "sm"}]]]]]

        footer
        [:footer.ty-content.border-t.ty-border
         [:div.mx-auto.max-w-3xl.px-4.py-4.ty-text-.text-sm.text-center
          "Built with Ty, Datastar & Clojure"
          " — " [:a {:href "/test"
                     :class "underline"} "debug"]]]

        theme-init
        [:script
         (h/raw "if (localStorage.getItem('theme') === 'dark')
                    document.documentElement.classList.add('dark');")]

        auto-height
        [:script
         (h/raw
           "document.addEventListener('DOMContentLoaded', function() {
             // Set app container height from actual window dimensions
             function setAppHeight() {
               var container = document.querySelector('.app-container');
               if (container) {
                 var h = window.innerHeight;
                 // On iOS Tauri, window.innerHeight is incorrect - use screen.height
                 // Detect mobile Tauri: has __TAURI__ and is touch device
                 var isTauriMobile = window.__TAURI__ && ('ontouchstart' in window);
                 if (isTauriMobile && screen.height > h) {
                   h = screen.height;
                 }
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
               var bottomPad = 24; /* py-6 = 1.5rem = 24px */
               tabs.setAttribute('height', Math.floor(window.innerHeight - rect.top - footerH - bottomPad) + 'px');
             }
             fitTabs();
             // Use ty-resize-observer to refit tabs when layout changes
             window.tyResizeObserver && window.tyResizeObserver.onResize('app-layout', fitTabs);
             // Fit transaction scroll container when dashboard tab is visible
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
           });")]]

    (str
      (h/html
        (page/doctype :html5)
        [:html {:lang "en"}
         head
         [:body.ty-canvas.antialiased
          [:div.app-container
           navbar
           [:ty-resize-observer#app-layout
            [:main.main-content.mx-auto.max-w-3xl.w-full.px-4.py-6
             body]]
           footer]
          theme-init
          auto-height]]))))

;; ---------------------------------------------------------------------------
;; Setup wizard
;; ---------------------------------------------------------------------------

(defn setup-page [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body
   (layout
     [:div.app-body.flex.items-center.justify-center
      [:div.max-w-md.mx-auto.space-y-8.py-8
       [:div.text-center.space-y-2
        [:h2.text-2xl.font-bold.ty-text++ "Welcome to PocketLedger"]
        [:p.ty-text- "Let's set up your expense tracker"]]

       [:div.ty-elevated.rounded-xl.p-6.space-y-6
        {"data-signals" "{setupName: '', setupCurrency: 'EUR', setupBudget: ''}"}

        ;; Name
        [:div
         [:ty-input
          {"data-bind" "setupName"
           :label "Your name"
           :placeholder "How should we call you?"}]]

        ;; Currency
        [:div
         [:label.block.text-sm.font-medium.ty-text+.mb-1 "Currency"]
         [:ty-dropdown
          {"data-bind" "setupCurrency"
           :placeholder "Select currency"
           :value "EUR"}
          [:ty-option {:value "EUR"} "EUR — Euro"]
          [:ty-option {:value "USD"} "USD — Dollar"]
          [:ty-option {:value "GBP"} "GBP — Pound"]
          [:ty-option {:value "CHF"} "CHF — Franc"]
          [:ty-option {:value "HRK"} "HRK — Kuna"]]]

        ;; Monthly budget
        [:div
         [:ty-input
          {"data-bind" "setupBudget"
           :label "Monthly budget"
           :type "number"
           :placeholder "e.g. 2000"}]]

        ;; Submit
        [:div.pt-2
         [:ty-button
          {:flavor "primary"
           :class "w-full"
           "data-on:click" "@post('/api/setup')"}
          "Start tracking"]]

        ;; Result area
        [:div#setup-result]]]])})

;; ---------------------------------------------------------------------------
;; Dashboard fragments
;; ---------------------------------------------------------------------------

(defn currency-symbol [currency]
  (case currency
    "EUR" "€" "USD" "$" "GBP" "£" "CHF" "CHF" "HRK" "kn"
    currency))

(defn summary-card [label value & [{:keys [flavor icon]}]]
  (let [bg-class (case flavor
                   "success" "ty-bg-success-"
                   "danger" "ty-bg-danger-"
                   "primary" "ty-bg-primary-"
                   "ty-elevated")
        text-class (case flavor
                     "success" "ty-text-success+"
                     "danger" "ty-text-danger+"
                     "primary" "ty-text-primary+"
                     "ty-text++")
        border-class (case flavor
                       "success" "ty-border-success"
                       "danger" "ty-border-danger"
                       "primary" "ty-border-primary"
                       "ty-border")]
    [:div {:class [bg-class border-class
                   "rounded-lg p-4 text-center border-t-2"]}
     [:div.flex.items-center.justify-center.gap-1.mb-1
      (when icon [:ty-icon {:name icon
                            :size "xs"
                            :class text-class}])
      [:p.text-sm {:class text-class} label]]
     [:p {:class [text-class "text-2xl font-bold"]}
      value]]))

(defn summary-fragment []
  (let [{:keys [budget expenses income remaining]} (db/get-summary)
        settings (db/get-all-settings)
        cur (currency-symbol (get settings "currency" "€"))]
    [:div#summary.grid.grid-cols-2.gap-4
     (summary-card "Budget" (str cur " " (format "%.0f" (double budget)))
                   {:flavor "primary"
                    :icon "wallet"})
     (summary-card "Spent" (str cur " " (format "%.0f" (double expenses)))
                   {:flavor "danger"
                    :icon "trending-down"})
     (summary-card "Income" (str cur " " (format "%.0f" (double income)))
                   {:flavor "success"
                    :icon "trending-up"})
     (summary-card "Remaining" (str cur " " (format "%.0f" (double remaining)))
                   {:flavor (if (neg? remaining) "danger" "success")
                    :icon "coins"})]))

(defn transaction-row [{:keys [id description amount type categories date]}]
  (let [settings (db/get-all-settings)
        cur (currency-symbol (get settings "currency" "€"))
        expense? (= type "expense")]
    [:div.flex.items-center.justify-between.p-3.ty-elevated.rounded-lg
     [:div.flex.items-center.gap-3.min-w-0
      [:div.min-w-0
       [:p.ty-text+.font-medium.truncate description]
       [:div.flex.items-center.gap-2.flex-wrap
        (for [{:keys [name icon]} categories]
          [:ty-tag {:size "sm"
                    :flavor "neutral"}
           [:ty-icon {:name (or icon "tag")
                      :size "xs"
                      :slot "start"}]
           name])
        [:span.ty-text-.text-xs date]]]]
     [:div.flex.items-center.gap-3.shrink-0
      [:span {:class [(if expense? "ty-text-danger+" "ty-text-success+")
                      "font-semibold"]}
       (str (if expense? "−" "+") cur " " (format "%.2f" amount))]
      [:ty-button
       {:size "xs"
        :flavor "danger"
        :plain true
        "data-on:click" (str "@post('/api/transactions/delete?id=" id "')")}
       [:ty-icon {:name "trash"
                  :size "xs"}]]]]))

(defn transaction-list-fragment []
  (let [txns (db/get-transactions {:limit 20})]
    [:div#transaction-list.space-y-2
     (if (empty? txns)
       [:div.text-center.py-8
        [:p.ty-text-.italic "No transactions yet. Add one above!"]]
       (for [tx txns]
         (transaction-row tx)))]))

;; ---------------------------------------------------------------------------
;; Main app page
;; ---------------------------------------------------------------------------

(defn app-page [_request]
  (let [settings (db/get-all-settings)
        name (get settings "name" "")
        currency (get settings "currency" "EUR")]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body
     (layout
       [:div.app-body
        {"data-signals" "{txDesc: '', txAmount: '', txType: 'expense', txCategory: '', txDate: ''}"}

        ;; Greeting
        [:div.shrink-0.pb-4
         [:h2.text-xl.font-bold.ty-text++ (str "Hey, " name "!")]
         [:p.ty-text- "Here's your spending overview"]]

        ;; Tabs — height set dynamically by ResizeObserver below
        [:ty-tabs#app-tabs {:active "dashboard"}

         ;; Rich labels with icons
         (for [[id label icon] [["dashboard" "Dashboard" "layout-dashboard"]
                                ["add" "Add" "plus-circle"]
                                ["settings" "Settings" "settings"]]]
           [:span {:slot (str "label-" id)
                   :class "flex items-center gap-1"}
            [:ty-icon {:name icon
                       :size "sm"}]
            label])

         ;; Dashboard tab
         [:ty-tab {:id "dashboard"
                   :label "Dashboard"}
          [:div.p-4
          ;; Summary cards — always visible
           (summary-fragment)
          ;; Transactions — scrollable via ty-scroll-container
           [:h3.text-lg.font-semibold.ty-text++.mt-4 "Recent Transactions"]
           [:ty-scroll-container#tx-scroll.mt-2 {:max-height "400px"}
            (transaction-list-fragment)]]]

         ;; Add transaction tab
         [:ty-tab {:id "add"
                   :label "Add"}
          [:div.p-4
           [:div.ty-elevated.rounded-xl.p-6.space-y-4
            [:h3.text-lg.font-semibold.ty-text++ "New Transaction"]

            ;; Type toggle
            [:div.flex.gap-2
             [:ty-tag
              {:size "md"
               "data-attr:flavor" "$txType === 'expense' ? 'danger' : 'neutral'"
               "data-on:click" "$txType = 'expense'"}
              [:ty-icon {:name "trending-down"
                         :size "xs"
                         :slot "start"}]
              "Expense"]
             [:ty-tag
              {:size "md"
               "data-attr:flavor" "$txType === 'income' ? 'success' : 'neutral'"
               "data-on:click" "$txType = 'income'"}
              [:ty-icon {:name "trending-up"
                         :size "xs"
                         :slot "start"}]
              "Income"]]

            [:ty-input
             {"data-bind" "txDesc"
              :label "Description"
              :placeholder "What was it for?"}]

            [:div.grid.grid-cols-1.sm:grid-cols-2.gap-4
             [:ty-input
              {"data-bind" "txAmount"
               :label "Amount"
               :type "currency"
               :currency currency
               :placeholder "0.00"}]
             [:ty-date-picker
              {"data-bind" "txDate"
               :label "Date"
               :placeholder "Pick a date"}]]

            [:div
             [:ty-multiselect
              {"data-bind" "txCategory"
               :label "Categories"
               :placeholder "Select categories"}
              (for [{:keys [id name icon]} (db/get-categories)]
                [:ty-tag {:value (str id)}
                 [:ty-icon {:name (or icon "tag")
                            :size "xs"
                            :slot "start"}]
                 name])]]

            [:ty-button
             {:flavor "primary"
              :class "w-full"
              "data-on:click" "@post('/api/transactions/add')"}
             [:ty-icon {:slot "start"
                        :name "plus"
                        :size "sm"}]
             "Add Transaction"]

            [:div#add-result]]]]

         ;; Settings tab
         [:ty-tab {:id "settings"
                   :label "Settings"}
          [:div.p-4
           {"data-signals" (str "{settingsName: '" (get settings "name" "") "'"
                                ", settingsCurrency: '" (get settings "currency" "EUR") "'"
                                ", settingsBudget: '" (get settings "monthly_budget" "") "'}")}
           [:div.ty-elevated.rounded-xl.p-6.space-y-4
            [:h3.text-lg.font-semibold.ty-text++ "Settings"]

            [:ty-input
             {"data-bind" "settingsName"
              :label "Your name"}]

            [:div
             [:label.block.text-sm.font-medium.ty-text+.mb-1 "Currency"]
             [:ty-dropdown
              {"data-bind" "settingsCurrency"
               :value (get settings "currency" "EUR")}
              [:ty-option {:value "EUR"} "EUR — Euro"]
              [:ty-option {:value "USD"} "USD — Dollar"]
              [:ty-option {:value "GBP"} "GBP — Pound"]
              [:ty-option {:value "CHF"} "CHF — Franc"]
              [:ty-option {:value "HRK"} "HRK — Kuna"]]]

            [:ty-input
             {"data-bind" "settingsBudget"
              :label "Monthly budget"
              :type "number"}]

            [:ty-button
             {:flavor "primary"
              "data-on:click" "@post('/api/settings')"}
             "Save Settings"]

            [:div#settings-result]

            [:hr.ty-border.my-4]

            [:div
             [:h4.text-md.font-semibold.ty-text++ "Danger Zone"]
             [:ty-button
              {:flavor "danger"
               :plain true
               :class "mt-2"
               "data-on:click" "@post('/api/reset')"}
              [:ty-icon {:slot "start"
                         :name "trash"
                         :size "sm"}]
              "Reset All Data"]
             [:div#reset-result]]]]]]])}))


;; ---------------------------------------------------------------------------
;; Router entry point — redirect to setup or app
;; ---------------------------------------------------------------------------

(defn index [request]
  (if (db/setup-complete?)
    (app-page request)
    (setup-page request)))
