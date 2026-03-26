# PocketLedger — TypeScript Backend

TypeScript server implementation using Hono, running on port 3000.

## Prerequisites

- [Node.js](https://nodejs.org/) 20+
- [Rust](https://rustup.rs/) and [Tauri CLI](https://v2.tauri.app/start/prerequisites/) for desktop/mobile

## Getting Started

**Install dependencies and start the server:**

```bash
npm install
npm run dev
```

This starts Hono on `http://0.0.0.0:3000`. Open http://localhost:3000 in a browser.

**Run with Tauri (desktop):**

```bash
# From repo root
cargo tauri dev --config src-tauri/tauri.ts.conf.json
```

## Project Structure

```
ts/
├── src/
│   ├── index.ts      # Entry point, server setup
│   ├── db.ts         # SQLite database operations
│   ├── handlers.ts   # Route handlers
│   ├── views.ts      # HTML rendering functions
│   └── sse.ts        # Server-Sent Events for Datastar
├── package.json
└── tsconfig.json
```

## Development

The server runs with `tsx` for TypeScript execution with hot reload.

```bash
npm run dev    # Development with watch mode
npm run build  # Compile TypeScript
npm start      # Run compiled JavaScript
```

## Android Development

### Prerequisites

- [Android Studio](https://developer.android.com/studio) with SDK and NDK installed
- [ngrok](https://ngrok.com/) (free account required)

### Initial setup

```bash
cargo tauri android init

# Required environment variables
export ANDROID_HOME=$HOME/Android/Sdk
export NDK_HOME=$ANDROID_HOME/ndk/<your-ndk-version>
```

Create an emulator in Android Studio: Tools → Device Manager → Create Virtual Device.

### Running on Android

Android development requires **three terminals**:

**Terminal 1** — Start the TypeScript server:
```bash
npm run dev
```

**Terminal 2** — ngrok tunnel (port 3000):
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

### Why ngrok?

Tauri on Android loads your app through a special `tauri.localhost` origin. Android's WebView uses `shouldInterceptRequest()` to intercept requests — and **Google's API does not expose POST request bodies** ([unfixed bug since 2018](https://issuetracker.google.com/issues/119844519)).

This means POST bodies are silently stripped. Datastar sends signals as JSON in POST bodies, breaking all form submissions.

**The fix**: bypass `tauri.localhost`. When `devUrl` points to a real HTTPS endpoint (ngrok), POST bodies arrive intact. This is dev-only — production apps point to your real HTTPS server.

### Useful commands

```bash
# Force restart app on emulator
adb shell am force-stop dev.gersak.pocketledger
adb shell am start -n dev.gersak.pocketledger/.MainActivity

# View device logs
adb logcat -s "tauri"
```

## iOS Development

### Prerequisites

- macOS with Xcode installed
- Xcode Command Line Tools: `xcode-select --install`
- iOS Rust targets: `rustup target add aarch64-apple-ios aarch64-apple-ios-sim`
- [ngrok](https://ngrok.com/) (free account required)

### Initial setup

```bash
cargo tauri ios init
```

### Running on iOS

**For physical devices:** It's recommended to open the project in Xcode for initial setup and running:
```bash
open src-tauri/gen/apple/app.xcodeproj
```
This makes code signing and device selection easier.

**For simulator or after Xcode setup**, use three terminals:

**Terminal 1** — Start the TypeScript server:
```bash
npm run dev
```

**Terminal 2** — ngrok tunnel (port 3000):
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
cargo tauri ios dev
```

### Code signing (physical device)

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

### Building release app

```bash
cargo tauri ios build
brew install ios-deploy
ios-deploy --bundle src-tauri/gen/apple/build/arm64/PocketLedger.app
```

### Troubleshooting

See [../docs/troubleshooting-ios.md](../docs/troubleshooting-ios.md) for common issues.
