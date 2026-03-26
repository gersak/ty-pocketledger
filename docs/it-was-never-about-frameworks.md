# It Was Never About Frameworks

There were so many attempts to create one codebase for all devices. You know
them better than me. The latest and probably closest is Flutter, powered by
Dart. What seemed so appealing about Flutter was the really good UX on mobile.

But why was it good?

Because of a bunch of components that Google gave you for free. Material.
Cupertino. Copy-paste, done. In the end, if you wanted to work with specific
styling, you'd end up in the same shit as building with web technologies
anyway.

Components are great for onboarding. From my perspective, that's what changed
the game and moved a lot of developers from React Native to Flutter. Not Dart.
Not the framework. The components.

## React Native... Come On

What a BS. It's React, but it's not web React. It's "reactive." Is that it?

You still need to write different code for mobile and web. `<Text>` instead of
`<p>`. `<View>` instead of `<div>`.
[Two codebases to maintain][rn-codebase], which means twice the effort each
time you make an update. Want to convert your React app to React Native?
[You'll need to rewrite your entire codebase][rn-rewrite].

The [honeymoon phase is over][rn-honeymoon]. Performance bottlenecks in
animation-heavy apps. No support for multiprocessing. Big app sizes. No
official testing frameworks. It [relies on third-party libraries][rn-libs]
for many UI elements, unlike Flutter's built-in components.

See? Even the criticism points back to components.

## Flutter's Broken Promise

And Flutter? Last I heard, it [still has issues on web][flutter-issues]. Slow
initial load because it must download and initialize its entire rendering
engine before displaying anything. No server-side rendering, which hurts SEO.
[Struggles with heavy, high-performance applications][flutter-wasm].

Yes, WebAssembly and Impeller have improved things. But after 5-6 years of
promises, we still don't have an end solution. The
[official guidance][flutter-guide] is basically "use it for the right thing"
— which means it's not universal.

So why the fuck is everybody still hooked on these ideas?

## The Simple Truth

If you want to go low-level, use Kotlin or Swift. Raw power. Hardware access.
Native performance.

If you want to be abstract and reach every platform, use **Web Components**.

That's it. That's the taxonomy. Why hasn't the industry landed here? I don't
know. Maybe Google and Meta keep pushing their solutions. Maybe developers
are unfamiliar with web components. Maybe we're all just dazed and confused,
chasing the next framework instead of solving the actual problem.

## Why Web Components

I built [Toddler][toddler] as a component library for React/Helix. It worked.
Teams used it.

Then chromalchemy in the Clojure community laid it out for me. The frontend
landscape right now: Replicant, Datastar, Electric, Squint, Scittle-based
wrappers, Borkweb, Membrane, clojure-dart + flutter, Signalli, Zero, Zodiac,
Caveman, Hazel... and those are just the recent ones. "Even for a UI fanboy
like myself, it's hard to keep up."

Add the chorus of developers wanting off React. The JS bazaar spawning
libraries, tools, frameworks, compiled languages endlessly.

That's when it clicked. **The components need to survive the framework churn.**

So I rewrote everything as web components. [Ty][ty].

Web components don't care what framework you use. They're the standard. They
work with React, ClojureScript, vanilla JS, Datastar, whatever comes next.
Frameworks change, web standards persist.

The rewrite was easier than expected. React state management at the leaf node
doesn't bring that much leverage. You can do most of it with plain JavaScript.

## The Proof That Made Me Uncomfortable

I built [ty-pocketledger][pocketledger] as a proof of concept. A personal
expense tracker that runs on web, desktop, and mobile from the same
server-rendered HTML.

The architecture:

```
Server (Clojure or TypeScript) → HTML + SSE → Datastar → Ty → Browser / Tauri
```

Server-Sent Events pushing HTML fragments to mobile devices. The server
renders everything. The client just morphs the DOM. No React. No Vue. No
client-side framework at all.

SSE server-rendered mobile apps shouldn't exist. It almost makes me physically
uncomfortable. But it works. The same Ty components render beautifully on web,
desktop, iOS, and Android.

If web components work in *that* setup — the most cursed architecture I could
imagine — then there's more to web components than anyone realizes.

![Demo](ty_demo_hq.gif)

## The Actual Insight

Maybe it was never about frameworks.

HTMX, Datastar, Replicant, Svelte, Vue, React, Reagent — I don't know what
other cool frameworks are out there. They all solve the same problem
differently. The framework wars are a distraction.

The real product is **components**. Components that:

- Know their rendering context
- Adjust visually to the screen
- Keep the model consistent
- Work across any framework that can handle web standards

Flutter's dirty secret: developers didn't love Dart. They loved that
`BottomNavigationBar` just worked and looked native.

## I'm Not Talking About Everything

I'm not talking about low-level, high-performance apps that need raw power or
hardware access. Go native for those.

I'm talking about the majority of apps that just need to move web to mobile.
CRUD apps. Dashboards. Forms. The 80% of software that doesn't need a game
engine.

Isn't web good enough for that? Isn't Tauri + web components the obvious
answer?

## Talk To Me

I don't want you to agree with me. I want you to respond. Share your opinion.
Tell me where I'm wrong. Tell me what I'm missing.

Because I've been building this stuff for years and I still feel like the
industry is chasing shadows.

---

**Links:**

- [Ty — Framework-agnostic web components][ty]
- [ty-pocketledger — SSE mobile app PoC][pocketledger]
- [Toddler — The React/Helix predecessor][toddler]

[rn-codebase]: https://www.sitepoint.com/react-vs-react-native-pros-cons-and-key-differences/
[rn-rewrite]: https://www.mobiloud.com/blog/react-vs-react-native
[rn-honeymoon]: https://medium.com/@ManuscriptsTech/the-state-of-react-native-in-2025-still-a-contender-or-time-to-move-on-566e63a6b69f
[rn-libs]: https://pagepro.co/blog/react-native-pros-and-cons/
[flutter-issues]: https://kitrum.com/blog/why-flutter-isnt-ideal-for-cross-platform-development/
[flutter-wasm]: https://amgres.com/blog/flutter-web-webassembly-wasm-2026-guide
[flutter-guide]: https://www.milanmeurrens.com/guides/when-to-use-flutter-for-web-in-2025-a-comprehensive-guide
[toddler]: https://github.com/gersak/toddler
[ty]: https://github.com/gersak/ty
[pocketledger]: https://github.com/gersak/ty-pocketledger
