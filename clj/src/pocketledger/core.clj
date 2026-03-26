(ns pocketledger.core
  (:require [org.httpkit.server :as http]
            [reitit.ring :as reitit]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [pocketledger.db :as db]
            [pocketledger.views :as views]
            [pocketledger.handlers :as handlers])
  (:gen-class))

(defn wrap-slurp-body
  "Pre-read the request body into a string so it survives wrap-params.
   Datastar sends signals as application/json in the POST body."
  [handler]
  (fn [request]
    (let [body (:body request)
          body-str (when body
                     (if (string? body)
                       body
                       (let [s (slurp body)]
                         (when-not (empty? s) s))))]
      (handler (assoc request :body body-str)))))

(defn make-handler []
  (reitit/ring-handler
   (reitit/router
    [["/" {:get {:handler #'views/index}}]

       ;; Setup
     ["/api/setup" {:post {:handler #'handlers/setup}}]

       ;; Dashboard
     ["/api/dashboard" {:get {:handler #'handlers/dashboard}}]

       ;; Transactions
     ["/api/transactions/add" {:post {:handler #'handlers/add-transaction}}]
     ["/api/transactions/delete" {:post {:handler #'handlers/delete-transaction}}]

       ;; Settings
     ["/api/settings" {:post {:handler #'handlers/save-settings}}]
     ["/api/reset" {:post {:handler #'handlers/reset-data}}]])

   (reitit/routes
    (reitit/create-resource-handler {:path "/"})
    (reitit/create-default-handler))

   {:middleware [wrap-slurp-body
                 wrap-params
                 wrap-keyword-params]}))

(def app
  (-> (make-handler)
      (wrap-reload {:dirs ["src"]})))

(defonce !server (atom nil))

(defn start! [& {:keys [port] :or {port 3000}}]
  (when @!server
    (@!server)
    (reset! !server nil))
  (db/init!)
  (db/seed-defaults!)
  (reset! !server (http/run-server #'app {:ip "0.0.0.0" :port port})))

(defn stop! []
  (when-let [s @!server]
    (s)
    (reset! !server nil)))

(defn -main [& _args]
  (start!))

(comment
  (start!)
  (stop!))
