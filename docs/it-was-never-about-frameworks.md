# It Was Never About Frameworks

## The Proof That Made Me Uncomfortable

I built [ty-pocketledger][pocketledger] as a proof of concept. A personal expense tracker that runs on web, desktop, and mobile from the same server-rendered HTML.

The architecture:

```
Server (Clojure or TypeScript) → HTML + SSE → Datastar → Ty → Browser / Tauri
```

Server-Sent Events pushing HTML fragments to mobile devices. The server renders everything. The client just morphs the DOM. No React. No Vue. No client-side framework at all.

**<ins>SSE server-rendered mobile apps shouldn't exist.</ins> It makes me physically uncomfortable.** But it works. The same web components render on web, desktop, iOS, and Android.

If web components work in *that* setup — the most cursed architecture I could imagine — then there's more to web components than anyone realizes.

<p align="center">
<img src="https://raw.githubusercontent.com/gersak/ty-pocketledger/master/docs/ty_demo_hq.gif" alt="Demo">
</p>

## The Actual Insight

It was never about frameworks.

HTMX, Datastar, Replicant, Svelte, Vue, React, Reagent — I don't know what other cool frameworks are out there. They all solve the same problem differently. And each framework is targeting certain audience. Legit! As it should. But **frameworks shouldn't own building blocks!**

The real product is **components**. Components that:

- Know their rendering context
- Adjust visually to the screen
- Keep the model consistent
- Work across any framework that can handle web standards


## The Simple Truth

If you want to go low-level, use Kotlin or Swift. Raw power. Hardware access. Native performance.

If you want to be abstract and reach every platform, use **Web Components**.

That's it. That's the taxonomy.

I'm not talking about games or video editors. I'm talking about the 80% of software that just needs to move web to mobile. CRUD apps. Dashboards. Forms.

React Native still requires [different code for mobile and web][rn-codebase]. Flutter's web story, six years in, is still ["use it for the right thing"][flutter-guide]. No more broken promises.

Why hasn't the industry landed here? I don't know. Maybe Google and Meta keep pushing their solutions. Maybe developers are unfamiliar with web components. Maybe we're all just dazed and confused, chasing the next framework instead of solving the actual problem.

## Why Web Components

I built [Toddler][toddler] as a component library for React/Helix. It worked. Teams used it.

Then chromalchemy in the Clojure community laid it out for me:

<p align="center">
<img src="https://raw.githubusercontent.com/gersak/ty-pocketledger/master/docs/chromalchemy_feedback.png" width="500" alt="chromalchemy feedback">
</p>

That's when it clicked. **The components need to survive the framework churn.**

So I rewrote everything as web components. [**ty(rell) components**][ty].

Web components don't care what framework you use. They're the standard. They work with React, ClojureScript, vanilla JS, Datastar, whatever comes next. Frameworks change, web standards persist.

**The rewrite was easier than expected.** React state management at the leaf node doesn't bring that much leverage. You can do most of it with plain JavaScript.

>[!CAUTION]
> **Your work isn't worthless.**
>
> Wrap it. Wrap your React component as a web component. Wrap your entire page if you need to. Web components are a composition layer — props in, attributes out. It doesn't matter what technology is underneath. React, Vue, vanilla JS, whatever. As long as you know how to work with props and attributes, it all composes.

## Talk To Me

I don't want you to agree with me. I want you to respond. Share your opinion. Tell me where I'm wrong. Tell me what I'm missing.

Because I've been building this stuff for years and I still feel like the industry is chasing shadows.

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
[toddler]: https://gersak.github.io/toddler/
[ty]: https://gersak.github.io/ty/welcome#top
[pocketledger]: https://github.com/gersak/ty-pocketledger
