(ns pocketledger.sse
  "Datastar SSE response helpers.

   Datastar expects text/event-stream responses with specific event types:
   - datastar-patch-elements  : morph/replace DOM elements
   - datastar-patch-signals   : update reactive signals"
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [hiccup2.core :as h]))

(defn sse-event
  "Build a single SSE event string."
  [event-type data-lines]
  (str "event: " event-type "\n"
       (str/join "\n" (map #(str "data: " %) data-lines))
       "\n\n"))

(defn patch-elements
  "Create a datastar-patch-elements SSE event."
  ([hiccup-fragment]
   (patch-elements {} hiccup-fragment))
  ([{:keys [selector mode]} hiccup-fragment]
   (let [html-str (str (h/html hiccup-fragment))
         lines (cond-> []
                 selector (conj (str "selector " selector))
                 mode     (conj (str "mode " (name mode)))
                 true     (conj (str "elements " html-str)))]
     (sse-event "datastar-patch-elements" lines))))

(defn patch-signals
  "Create a datastar-patch-signals SSE event."
  [signals-map]
  (sse-event "datastar-patch-signals"
             [(str "signals " (json/write-str signals-map))]))

(defn sse-response
  "Return an SSE response with one or more events concatenated."
  [& event-strings]
  {:status 200
   :headers {"Content-Type" "text/event-stream"
             "Cache-Control" "no-cache"
             "Connection" "keep-alive"}
   :body (str/join event-strings)})

(defn parse-signals
  "Extract Datastar signals from the request.
   GET requests send signals as a query param, POST as the body."
  [request]
  (let [raw (or (get-in request [:params :datastar])
                (when-let [body (:body request)]
                  (if (string? body) body (slurp body))))]
    (when (and raw (not (str/blank? raw)))
      (json/read-str raw :key-fn keyword))))
