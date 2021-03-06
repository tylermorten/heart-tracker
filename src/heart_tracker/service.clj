(ns heart-tracker.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.log :refer [info]]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]
            [clojure.java.io :as io]
            [om.next.server :as om]
            [heart-tracker.util :refer [wrap-authorize]]
            [heart-tracker.parser :refer [readf mutate]]))

(defn api
  [request]
  (info :email (-> request :emailAddress))
  (let [payload (:transit-params request)
        resp ((om/parser {:read   readf
                          :mutate mutate}) {:db (:db request)
                                                :emailAddress (:emailAddress request)
                                                       :conn (:connection request)} payload)]
    (info :payload payload)
    {:status 200
     :body resp}))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(defn home-page
  [request]
  (ring-resp/response
    (slurp (io/file
             (io/resource "public/index.html")))))

(defroutes routes
  ;; Defines "/" and "/about" routes with their associated :get handlers.
  ;; The interceptors defined after the verb map (e.g., {:get home-page}
  ;; apply to / and its children (/about).
  [[["/" {:get home-page}
     ^:interceptors [(body-params/body-params) bootstrap/html-body]
     ["/api" ^:interceptors [bootstrap/transit-json-body wrap-authorize] {:any api}]
     ["/about" {:get about-page}]]]])

;; Consumed by heart-tracker.server/create-server
;; See bootstrap/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :jetty
              ;;::bootstrap/host "localhost"
              ::bootstrap/port 8080})

