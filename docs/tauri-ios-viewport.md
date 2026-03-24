# Tauri iOS Viewport Height Fix

## The Problem

On Tauri iOS, the standard CSS viewport units (`100vh`, `100dvh`, `100%`, `-webkit-fill-available`) do not correctly report the full screen height. The WebView's viewport calculations are approximately 80-100px short of the actual screen height.

This causes:
- Footer not reaching the bottom of the screen
- Visible gap between app content and screen edge
- Inconsistent layout between iOS and other platforms

## Measurements

On an iPhone, we observed these values:

| Method | Value | Correct? |
|--------|-------|----------|
| `window.innerHeight` | 839px | No |
| `visualViewport.height` | 839px | No |
| `document.documentElement.clientHeight` | 839px | No |
| `screen.height` | 932px | Yes |

## The Solution

We use JavaScript to set the `.app-container` height directly, choosing the correct measurement based on the platform:

```javascript
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
```

### Detection Logic

1. **Desktop browsers**: Use `window.innerHeight` (works correctly)
2. **Desktop Tauri**: Use `window.innerHeight` (no touch events)
3. **Mobile Tauri (iOS/Android)**: Use `screen.height` when larger than `window.innerHeight`

The detection uses:
- `window.__TAURI__` - Present when running in Tauri
- `'ontouchstart' in window` - Present on touch devices (mobile)

## CSS Structure

The app uses this CSS structure:

```css
html, body {
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.app-container {
  display: flex;
  flex-direction: column;
  padding-top: env(safe-area-inset-top);
  /* height set by JavaScript */
}

.app-container > ty-resize-observer {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}
```

## Safe Areas

- **Top safe area**: Handled via `padding-top: env(safe-area-inset-top)` for status bar
- **Bottom safe area**: Not needed when using `screen.height` as it accounts for the full screen
- **Viewport meta**: Must include `viewport-fit=cover` to enable edge-to-edge rendering

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
```

## Related Tauri Issues

- [#6961 - Ignore safe area in mobile tauri](https://github.com/tauri-apps/tauri/issues/6961)
- [#11475 - IOS/Android SafeArea control](https://github.com/tauri-apps/tauri/issues/11475)

## File Location

The viewport fix is implemented in:
- `src/pocketledger/views.clj` - `layout` function, `auto-height` script
