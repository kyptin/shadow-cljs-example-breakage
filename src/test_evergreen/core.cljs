(ns test-evergreen.core
  (:require
    ["react" :as react]
    ["react-dom" :as reactDOM]
    ["evergreen-ui" :as eg]))

(defn ^:dev/after-load mount-root []
  (let [mount (js/document.getElementById "app")
        doc (react/createElement eg/Button nil "Hello")]
    (reactDOM/render doc mount)))

(defn init []
  (enable-console-print!)
  (mount-root))
