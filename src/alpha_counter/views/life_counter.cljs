(ns alpha-counter.views.life-counter
  (:require [alpha-counter.data :as data]
            [alpha-counter.views.util :refer [classes html-container]]
            [alpha-counter.abilities :as abilities]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

;; Takes a properties hash {:text string, :on-click fn}. When first clicked,
;; adds the "activated" class to the button for 2 seconds. If clicked again
;; within this time, executes :on-click.
(defn- two-stage-button [{:keys [text on-click]} owner]
  (reify
    om/IDisplayName
    (display-name [_] "TwoStageButton")
    om/IInitState
    (init-state [_] {:activated false}) ; when true, next click will execute on-click
    om/IRenderState
    (render-state [_ {:keys [activated]}]
      (let [activate! (fn []
                        (om/set-state! owner :activated true)
                        (js/setTimeout #(om/set-state! owner :activated false) 1000))
            handle-click #(if activated
                            (on-click)
                            (activate!))]
        (html [:button {:class (classes "button small" (when activated "activated"))
                        :on-click handle-click}
               text])))))

(defn- ability-button [[text f] owner]
  (reify
    om/IDisplayName
    (display-name [_] "AbilityButton")
    om/IRender
    (render [_]
      (html [:button {:class "button small" :on-click f} text]))))

(defn- toolbar [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "Toolbar")
    om/IRender
    (render [_]
      (html
        [:div {:class "toolbar"}
         (om/build two-stage-button {:text "Character Select" :on-click data/return-to-character-select!})
         (om/build two-stage-button {:text "Rematch" :on-click data/rematch!})
         [:button {:class "button small" :on-click data/undo!} "Undo"]
         (html-container
           [:div {:class "abilities"}]
           (om/build-all ability-button (abilities/active)))]))))

;; Returns the player's health as a float ratio, between 1.0 and 0.0.
(defn- health-ratio [player]
  (let [max-health (data/max-health player)
        health (:health player)]
    (/ health max-health)))

;; Returns the player's health as a percentage string ending in "%", rounded
;; up; e.g. a player at 50/80 health would return "63%". Useful when applying
;; damage bar width in CSS.
(defn- health-percent [player]
  (-> (health-ratio player) (* 100) Math/ceil (str "%")))

;; Returns the damage the player has taken as a percentage string ending in
;; "%", rounded down; e.g. a player at 50/80 health would return "37%". Useful
;; when applying damage bar margin for right alignment in CSS.
(defn- damage-percent [player]
  (-> (- 1 (health-ratio player)) (* 100) Math/floor (str "%")))

;; Health bar component for a single player. Includes character name, current
;; life as a number, and health and damage bars. The health bar shrinks as the
;; player takes damage, progressively revealing the damage bar.
(defn- player-health [player owner]
  (reify
    om/IDisplayName
    (display-name [_] "PlayerHealth")
    om/IRender
    (render [_]
      (let [app (om/observe owner (data/app-cursor))
            team (data/team-of player)
            current (and (= (:id player) (:current-player-id team))
                         (= (:id team) (:current-team-id app)))
            health-width ["width" (health-percent player)]
            health-style #js {"width" (health-percent player)}]
        (html
          [:div {:class (classes "health" (when current "current"))}
           [:div {:class "health__name"}
            (str (when (:ex player) "EX ") (:name (:character player)))]
           [:div {:class "health__health-bar"}
            [:div {:class "health__damage"} ""]
            [:div {:class "health__health" :style health-style} ""]]
           [:div {:class "health__number"} (:health player)]])))))

;; Health bar component for a team. If there are two players on the team, the
;; benched player's health bar will have the "bench" class. If one player is a
;; Dramatic Battle boss, the player's health bar will have the "boss" class.
; TODO: make this true
(defn- team-health [team owner]
  (reify
    om/IDisplayName
    (display-name [_] "TeamHealth")
    om/IRender
    (render [_]
      (let [app (om/observe owner (data/app-cursor))
            current (= (:id team) (:current-team-id app))
            right (= (:id team) :team-two)]
        (html-container
          [:div {:class (classes "team-health" (when right "right"))
                 :on-click (if current
                             (partial data/tag! team)
                             (partial data/select-team! team))}]
          (om/build-all player-health (:players team)))))))

;; A button which registers a hit for the supplied damage amount when clicked.
(defn- damage-button [n owner]
  (reify
    om/IDisplayName
    (display-name [_] "DamageButton")
    om/IRender
    (render [_]
      (let [text (if (pos? n) n (str "+" (Math/abs n)))]
      (html
        [:li
         [:button {:class "small button"
                   :on-click #(data/register-hit! n)}
          text]])))))

;; When a combo is in progress, display the running total; otherwise, displays
;; the "VS" symbol between the two health panes.
(defn- combo [running-total owner]
  (reify
    om/IDisplayName
    (display-name [_] "Combo")
    om/IRender
    (render [_]
      (let [idle (#{0 :reset} running-total) ; false if combo is in progress
            is-healing (neg? running-total) ; yes, negative damage restores health
            damage-text (str (when is-healing "+") (Math/abs running-total))
            text (if idle "VS" damage-text)
            active-state-name (cond is-healing "healing", (not idle) "damage")]
        ; active-state-name is nil if idle
        (html [:div {:class (classes "combo" active-state-name)}
               [:div {:class "combo__text"} text]])))))

;; Life counter component. Includes the combo damage readout, a health
;; component for each player, damage buttons, and a toolbar.
(defn main [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "LifeCounter")
    om/IRender
    (render [_]
      (html
        [:div {:class "life-counter"}
         (om/build toolbar app)
         [:div {:class "counters"}
          (om/build team-health (first (:teams app)))
          (om/build combo (:running-total app))
          (om/build team-health (second (:teams app)))]
         (html-container
           [:ul {:class "damage-buttons"}]
           (om/build-all damage-button data/damage-amounts))]))))
