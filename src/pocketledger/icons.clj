(ns pocketledger.icons
  "Server-side icon registration. Pulls SVG strings from ty-icons (cljc)
   and generates a <script> tag that batch-registers them via window.tyIcons.register()."
  (:require [clojure.data.json :as json]
            [hiccup2.core :as h]
            [ty.lucide :as lucide]))

;; Icons used in PocketLedger — just a Clojure map of name → SVG string
(def app-icons
  {"wallet"           lucide/wallet
   "layout-dashboard" lucide/layout-dashboard
   "plus-circle"      lucide/circle-plus
   "settings"         lucide/settings
   "plus"             lucide/plus
   "trash"            lucide/trash-2
   "tag"              lucide/tag
   "moon"             lucide/moon
   "sun"              lucide/sun
   "chevron-down"     lucide/chevron-down
   "chevron-up"       lucide/chevron-up
   "chevron-left"     lucide/chevron-left
   "chevron-right"    lucide/chevron-right
   "trending-up"      lucide/trending-up
   "trending-down"    lucide/trending-down
   "check"            lucide/check
   "x"                lucide/x
   ;; Dashboard icons
   "coins"            lucide/coins
   ;; Category icons
   "utensils"         lucide/utensils
   "car"              lucide/car
   "house"            lucide/house
   "film"             lucide/film
   "heart-pulse"      lucide/heart-pulse
   "shopping-bag"     lucide/shopping-bag
   "zap"              lucide/zap})

(defn registration-script
  "Generate a <script> tag that batch-registers all icons.
   Serializes the icon map as JSON, waits for ty.js to load,
   then calls window.tyIcons.register() once."
  []
  (let [icons-json (json/write-str app-icons)]
    [:script
     (h/raw (str
       "(function() {\n"
       "  var icons = " icons-json ";\n"
       "  function register() {\n"
       "    if (!window.tyIcons || !window.tyIcons.register) return false;\n"
       "    window.tyIcons.register(icons);\n"
       "    return true;\n"
       "  }\n"
       "  if (!register()) {\n"
       "    var i = setInterval(function() { if (register()) clearInterval(i); }, 100);\n"
       "    setTimeout(function() { clearInterval(i); }, 10000);\n"
       "  }\n"
       "})();"))]))
