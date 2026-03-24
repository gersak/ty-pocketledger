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
- For Android dev: [ngrok](https://ngrok.com/) (free account required — see [why ngrok?](#why-ngrok-for-android-development))

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

**Alternatives considered:**
- **Caddy reverse proxy** with self-signed certs — works but requires installing the CA certificate on the Android emulator, which adds friction
- **HTTP to LAN IP** (`http://192.168.x.x:3000`) — Android blocks cleartext in release builds, and some WebView configurations still interfere
- **Tauri HTTP plugin** — uses Rust-side requests instead of WebView fetch, but Datastar uses standard `fetch` internally so this doesn't help
- **Header workaround** — monkey-patch `fetch` to copy the body into a custom header, server reads from header as fallback. Works but is a hack.

ngrok is the simplest: one command, trusted HTTPS cert, zero emulator configuration.

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

## Production Deployment

In production, the architecture simplifies:

```
Tauri APK (bundled HTML/CSS/JS) → HTTPS → Production Clojure Server
```

No ngrok, no tunnels, no workarounds. The Tauri app ships with your static assets and points `devUrl` (or the production equivalent) at your HTTPS server. POST bodies work because requests go over the real network.

For fully offline/local apps: bundle a [GraalVM native-image](https://www.graalvm.org/native-image/) Clojure server as a Tauri sidecar — the server runs on the device itself.
