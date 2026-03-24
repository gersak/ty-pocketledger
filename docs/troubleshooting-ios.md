# iOS Troubleshooting

Common issues when building PocketLedger for iOS with Tauri.

## Build Errors

### "cargo: command not found" in Xcode

**Symptom:** Build fails with "Command PhaseScriptExecution failed" and "cargo: command not found"

**Cause:** Xcode doesn't inherit your shell's PATH where cargo is installed.

**Solution:** Don't build from Xcode directly. Use the terminal:

```bash
cargo tauri ios dev --device
```

If you must use Xcode, create a symlink:
```bash
sudo ln -s ~/.cargo/bin/cargo /usr/local/bin/cargo
```

---

### Missing Rust target: "aarch64-apple-ios"

**Symptom:** Build fails with target-related errors

**Solution:**
```bash
rustup target add aarch64-apple-ios aarch64-apple-ios-sim
```

---

### CocoaPods "Unknown object version (77)"

**Symptom:** `pod install` fails with xcodeproj errors

**Cause:** Outdated CocoaPods version

**Solution:**
```bash
brew install cocoapods
# or
sudo gem install cocoapods
```

Note: This project doesn't actually require CocoaPods (empty Podfile), so you can ignore this if using `cargo tauri ios dev`.

---

### "Signing for app_iOS requires a development team"

**Symptom:** Build fails in Xcode with signing error

**Solution:**
1. Open `src-tauri/gen/apple/app.xcodeproj`
2. Select **app_iOS** under TARGETS (not the project)
3. Go to **Signing & Capabilities** tab
4. Check **Automatically manage signing**
5. Select your **Team** (your Apple ID)

---

## Device Issues

### App won't launch on iPhone

**Symptom:** App installs but crashes immediately or shows "Untrusted Developer"

**Solution:** Trust the developer profile on your iPhone:
1. Settings → General → VPN & Device Management
2. Tap your developer profile
3. Tap **Trust**

---

### App expires after 7 days

**Cause:** Free Apple Developer accounts have 7-day provisioning profiles

**Solution:**
- Reinstall the app every 7 days, or
- Pay for Apple Developer Program ($99/year) for 1-year profiles

---

## Layout Issues

### Footer not at bottom / 80px gap on screen

**Symptom:** Content doesn't fill the full screen height on iOS

**Cause:** Tauri iOS WebView's `window.innerHeight` returns incorrect value (~80px less than actual)

**Solution:** This app uses `screen.height` on mobile Tauri. See [tauri-ios-viewport.md](tauri-ios-viewport.md) for the full fix.

**Quick check values:**
| Method | iOS Tauri | Correct? |
|--------|-----------|----------|
| `window.innerHeight` | 839px | No |
| `screen.height` | 932px | Yes |

---

### Input zoom on iOS

**Symptom:** Page zooms in when focusing input fields

**Solution:** Add to viewport meta tag:
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
```

---

## Production Build Issues

### "asset not found: index.html"

**Symptom:** Release build shows white screen with this error

**Cause:** Tauri production builds expect static assets, but this is a server-rendered app

**Solution:** Create a redirect page in `src-tauri/dist/index.html`:

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <script>
    window.location.href = 'https://your-server-url.ngrok-free.dev';
  </script>
</head>
<body>
  <p>Redirecting...</p>
</body>
</html>
```

And set in `tauri.conf.json`:
```json
{
  "build": {
    "frontendDist": "./dist"
  }
}
```

---

## Useful Debug Commands

```bash
# Check installed iOS targets
rustup target list --installed | grep ios

# Open Xcode project directly
open src-tauri/gen/apple/app.xcodeproj

# Reinitialize iOS project (if corrupted)
rm -rf src-tauri/gen/apple
cargo tauri ios init

# Check if device is connected
xcrun xctrace list devices
```
