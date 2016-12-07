(ns expertus.auth
  (:require [expertus.state :refer [app-state]]
            [expertus.routes :refer [reset-hash]]))

;; Проверка состояния identity
(defn auth? []
  (let [identity (:identity @app-state)]
    ;; Проверка наличия токена авторизации
    (if (and identity (:auth-token identity))
      true false)))

;; Удаление состояния пользователя
(defn logout []
  (swap! app-state assoc :identity nil)
  (swap! app-state assoc :account nil)
  (swap! app-state assoc :profile nil)
  (swap! app-state assoc :alert nil)
  (reset-hash "/"))
