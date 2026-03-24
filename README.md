# PocketLedger

A personal expense tracker built with **Clojure**, **Datastar**, **Ty** web components, and **Tauri** — demonstrating a single-codebase architecture that runs on web, desktop, and mobile from the same server-rendered HTML.

## Architecture

```
Clojure (http-kit) → HTML + SSE → Datastar (reactivity) → Ty (web components) → Browser / Tauri
```

- **Clojure** handles all business logic, routing, and HTML rendering (Hiccup)
- **Datastar** provides client-side reactivity via SSE — no client-side framework needed
- **Ty** web components (`@gersak/ty`) provide the UI layer — framework-agnostic, works everywhere
- **Tauri** wraps the same web content into native desktop and mobile apps
- **SQLite** stores data locally via next.jdbc

There is no separate API layer. The server renders HTML fragments and pushes them to the client over Server-Sent Events. Datastar morphs the DOM. The entire frontend is server-driven.

## Prerequisites

- Java 21+ (for Clojure)
- [Clojure CLI](https://clojure.org/guides/install_clojure)
- [Rust](https://rustup.rs/) (for Tauri)
- [Tauri CLI](https://v2.tauri.app/start/prerequisites/) — `cargo install tauri-cli`
- For Android: [Android Studio](https://developer.android.com/studio) with SDK and NDK installed
- For iOS: macOS with [Xcode](https://developer.apple.com/xcode/) installed
- For mobile dev: [ngrok](https://ngrok.com/) (free account required — see [why ngrok?](#why-ngrok-for-android-development))

## Getting Started

### 1. Start the Clojure server

```bash
cd examples/pocketledger
clj -M:dev
```

This starts http-kit on `http://0.0.0.0:3000`. Open http://localhost:3000 in a browser — you have a working app.

### 2. Desktop (Tauri)

```bash
cargo tauri dev
```

This opens PocketLedger as a native desktop window. It connects to `http://localhost:3000` — same server, native shell.

## Android Development

### Initial setup

**Install Tauri and Android tooling:**

```bash
# Install Tauri CLI
cargo install tauri-cli

# Initialize Android support in an existing Tauri project
cargo tauri android init
```

**Android SDK** — install via Android Studio (not mise). Required environment:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export NDK_HOME=$ANDROID_HOME/ndk/<your-ndk-version>
```

**Create an emulator** in Android Studio: Tools → Device Manager → Create Virtual Device (e.g. Pixel 9).

### Running on Android

Android development requires **three terminals**:

**Terminal 1** — Clojure server:
```bash
cd examples/pocketledger
clj -M:dev
```

**Terminal 2** — ngrok tunnel:
```bash
ngrok http 3000
```

Copy the HTTPS forwarding URL (e.g. `https://abc123.ngrok-free.dev`).

**Terminal 3** — Update `src-tauri/tauri.android.conf.json` with the ngrok URL, then build:
```json
{
  "build": {
    "devUrl": "https://your-ngrok-url.ngrok-free.dev"
  }
}
```

```bash
cargo tauri android dev --config src-tauri/tauri.android.conf.json
```

The app builds, deploys to the emulator, and connects to your Clojure server through ngrok.

### Why ngrok for Android development?

Tauri on Android loads your app through a special `tauri.localhost` origin. Under the hood, Android's WebView uses `shouldInterceptRequest()` to intercept all requests to this origin — and **Google's API does not expose POST request bodies** (this is an [unfixed Android bug since 2018](https://issuetracker.google.com/issues/119844519)).

This means:
- **GET requests** work fine
- **POST bodies are silently stripped** — the server receives an empty body
- **Custom headers survive** the interception

Datastar sends reactive signals as JSON in POST bodies, so this breaks all form submissions and state updates on Android.

**The fix**: bypass `tauri.localhost` entirely. When `devUrl` points to a real HTTPS endpoint (ngrok), the WebView makes normal network requests without interception. POST bodies arrive intact.

**This is a dev-only problem.** In production, the Tauri app points to your real HTTPS server — POST bodies work natively. ngrok simulates this production topology during development.

### Useful commands

```bash
# Force restart app on emulator
adb shell am force-stop dev.gersak.pocketledger
adb shell am start -n dev.gersak.pocketledger/.MainActivity

# View device logs
adb logcat -s "tauri"

# Kill Gradle daemon (if changing SDK paths)
cd src-tauri/gen/android && ./gradlew --stop

# Stop ngrok
# Ctrl+C in the ngrok terminal
```

## iOS Development

### Prerequisites

- macOS with Xcode installed
- Xcode Command Line Tools: `xcode-select --install`
- iOS Rust targets: `rustup target add aarch64-apple-ios aarch64-apple-ios-sim`

### Initial setup

```bash
# Initialize iOS support
cargo tauri ios init
```

### Running on iOS

iOS development requires **three terminals** (same as Android):

**Terminal 1** — Clojure server:
```bash
clj -M:dev
```

**Terminal 2** — ngrok tunnel:
```bash
ngrok http 3000
```

Copy the HTTPS URL and update `src-tauri/tauri.ios.conf.json`:
```json
{
  "build": {
    "devUrl": "https://your-ngrok-url.ngrok-free.dev"
  }
}
```

**Terminal 3** — Run on device or simulator:
```bash
# Run on simulator
cargo tauri ios dev

# Run on physical device
cargo tauri ios dev --device
```

### Code signing (physical device)

For physical devices, open the Xcode project to configure signing:

```bash
open src-tauri/gen/apple/app.xcodeproj
```

In Xcode:
1. Select **app_iOS** target
2. Go to **Signing & Capabilities**
3. Enable **Automatically manage signing**
4. Select your **Team** (Apple ID)

On first run, trust the developer on your iPhone:
- Settings → General → VPN & Device Management → tap your profile → Trust

### Troubleshooting

See [docs/troubleshooting-ios.md](docs/troubleshooting-ios.md) for common issues:
- Cargo not found in Xcode
- Code signing errors
- Viewport height issues
- Production build "asset not found"

### Building release app (wireless testing)

To install an app that runs without Mac connection:

```bash
# Build the release app
cargo tauri ios build

# Install ios-deploy (first time only)
brew install ios-deploy

# Install to connected device
ios-deploy --bundle src-tauri/gen/apple/build/arm64/PocketLedger.app
```

The release app loads `src-tauri/dist/index.html` which redirects to your server URL.

**To change the server URL**, edit `src-tauri/dist/index.html`:

```html
<script>
  window.location.href = 'https://your-server-url.com';
</script>
```

**Note:** With a free Apple Developer account, apps expire after 7 days.

## Production Deployment

Since this is a server-rendered app, Tauri bundles a minimal `index.html` that redirects to your production server:

```
src-tauri/dist/index.html → redirect → https://your-production-server.com
```

The Clojure server must be accessible over HTTPS. For production:
1. Deploy your Clojure server (VPS, cloud, etc.)
2. Update `src-tauri/dist/index.html` with the production URL
3. Build release: `cargo tauri ios build` / `cargo tauri android build`

For fully offline/local apps: bundle a [GraalVM native-image](https://www.graalvm.org/native-image/) Clojure server as a Tauri sidecar — the server runs on the device itself.
