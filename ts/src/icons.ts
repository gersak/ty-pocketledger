/**
 * Server-side icon registration.
 * Pulls SVG strings from @gersak/ty/icons/lucide and generates a <script>
 * that batch-registers them via window.tyIcons.register().
 */

import {
  wallet,
  layoutDashboard,
  circlePlus,
  settings,
  plus,
  trash2,
  tag,
  moon,
  sun,
  chevronDown,
  chevronUp,
  chevronLeft,
  chevronRight,
  trendingUp,
  trendingDown,
  check,
  x,
  coins,
  utensils,
  car,
  house,
  film,
  heartPulse,
  shoppingBag,
  zap,
} from "@gersak/ty/icons/lucide";

// Icons used in PocketLedger — map name → SVG string
const appIcons: Record<string, string> = {
  wallet,
  "layout-dashboard": layoutDashboard,
  "plus-circle": circlePlus,
  settings,
  plus,
  trash: trash2,
  tag,
  moon,
  sun,
  "chevron-down": chevronDown,
  "chevron-up": chevronUp,
  "chevron-left": chevronLeft,
  "chevron-right": chevronRight,
  "trending-up": trendingUp,
  "trending-down": trendingDown,
  check,
  x,
  coins,
  utensils,
  car,
  house,
  film,
  "heart-pulse": heartPulse,
  "shopping-bag": shoppingBag,
  zap,
};

/**
 * Generate a <script> tag that batch-registers all icons.
 * Waits for ty.js to load, then calls window.tyIcons.register() once.
 */
export function registrationScript(): string {
  const iconsJson = JSON.stringify(appIcons);
  return `<script>
(function() {
  var icons = ${iconsJson};
  function register() {
    if (!window.tyIcons || !window.tyIcons.register) return false;
    window.tyIcons.register(icons);
    return true;
  }
  if (!register()) {
    var i = setInterval(function() { if (register()) clearInterval(i); }, 100);
    setTimeout(function() { clearInterval(i); }, 10000);
  }
})();
</script>`;
}
