(ns alpha-counter.views.life-counter
  (:require [alpha-counter.data :as data]
            [alpha-counter.views.util :refer [classes]]
            [alpha-counter.abilities :as abilities]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; Takes a properties hash {:text string, :on-click fn}. When first clicked,
;; adds the "activated" class to the button for 2 seconds. If clicked again
;; within this time, executes :on-click.
(defn- two-stage-button [{:keys [text on-click]} owner]
  (reify
    om/IDisplayName
    (display-name [_] "TwoStageButton")
    om/IInitState
    (init-state [_] {:activated false}) ; when true, next click will return to char select
    om/IRenderState
    (render-state [_ {:keys [activated]}]
      (let [activate! (fn []
                        (om/set-state! owner :activated true)
                        (js/setTimeout #(om/set-state! owner :activated false) 1000))
            handle-click #(if activated
                            (on-click)
                            (activate!))]
        (dom/button #js {:className (classes "button small" (when activated "activated"))
                         :onClick handle-click}
          text)))))

(defn- toolbar-view [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "ToolbarView")
    om/IRender
    (render [_]
      (dom/div #js {:className "toolbar-view"}
        (om/build two-stage-button {:text "Character Select" :on-click data/return-to-character-select!})
        (om/build two-stage-button {:text "Rematch" :on-click data/rematch!})
        (dom/button #js {:className "button small" :onClick data/undo!} "Undo")
        (when (data/chosen? "Gwen")
          (dom/div #js {:className "abilities"}
            (dom/button #js {:className "button small" :onClick abilities/shadow-plague!}
              "Shadow Plague")))
        (when (data/chosen? "Gloria")
          (dom/div #js {:className "abilities"}
            (dom/button #js {:className "button small" :onClick abilities/overdose!}
              "Overdose")
            (dom/button #js {:className "button small" :onClick abilities/healing-touch!}
              "Healing Touch")
            (dom/button #js {:className "button small" :onClick abilities/bathed-in-moonlight!}
              "Bathed in Moonlight")))
        (when (data/chosen? "Argagarg")
          (dom/div #js {:className "abilities"}
            (dom/button #js {:className "button small" :onClick abilities/hex-of-murkwood!}
             "Hex of Murkwood")))
        (when (data/chosen? "Jaina")
          (dom/div #js {:className "abilities"}
            (dom/button #js {:className "button small" :onClick abilities/burning-vigor!}
              "Burning Vigor")
            (dom/button #js {:className "button small" :onClick abilities/burning-desperation!}
              "Burning Desperation")))))))

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

;; Health bar view. Expects props {:player p, :current-player-id k,
;; :select-player fn}. Includes character name, current life as a number, and
;; health and damage bars. The health bar shrinks as the player takes damage,
;; progressively revealing the damage bar.
(defn- health-view [props owner]
  (reify
    om/IDisplayName
    (display-name [_] "HealthView")
    om/IRender
    (render [_]
      (let [{:keys [player current-player-id select-player]} props
            ; if this is the left-hand player, the health bar should be aligned
            ; right as it shrinks; I'd prefer to do this in CSS, I just don't
            ; know how. TODO: learn how. Maybe dwab or Rachel can help.
            left (= (:id player) :player-one)
            current (= (:id player) current-player-id)
            health-width ["width" (health-percent player)]
            health-offset (when left ["marginLeft" (damage-percent player)])
            health-style (apply js-obj (concat health-width health-offset))]
        (dom/li #js {:className (classes "health-view" (when current "current"))
                     :onClick select-player}
          (dom/div #js {:className "health-view__name"}
            (str (when (:ex player) "EX ") (:name (:character player))))
          (dom/div #js {:className "health-view__health-bar"}
            (dom/div #js {:className "health-view__damage"} "")
            (dom/div #js {:className "health-view__health" :style health-style} "")
            (dom/div #js {:className "health-view__number"} (:health player))))))))

;; List of damage buttons. Uses no props, but om/build expects at least one, so
;; just supply nil. Automatically sets up buttons to send hits to the
;; appropriate channels.
; TODO: Remove the need to pass nil; maybe define this as a partial?
(defn- damage-buttons-view [_ owner]
  (reify
    om/IDisplayName
    (display-name [_] "DamageButtonsView")
    om/IRender
    (render [_]
      (apply dom/ul #js {:className "damage-buttons"}
             (map (fn [n]
                    (dom/li nil
                      (dom/button
                        #js {:className "small button"
                             :onClick #(data/register-hit n)}
                        (if (pos? n) n (str "+" (Math/abs n))))))
                  data/damage-amounts)))))

;; When a combo is in progress, display the running total; otherwise, displays
;; the "VS" symbol between the two health-view panes.
(defn- combo-view [running-total owner]
  (reify
    om/IDisplayName
    (display-name [_] "ComboView")
    om/IRender
    (render [_]
      (let [idle (#{0 :reset} running-total) ; false if combo is in progress
            is-healing (neg? running-total) ; yes, negative damage restores health
            damage-text (str (when is-healing "+") (Math/abs running-total))
            text (if idle "VS" damage-text)
            active-state-name (cond is-healing "healing", (not idle) "damage")]
            ; active-state-name is nil if idle
        (dom/div #js {:className (classes "combo-view" active-state-name)}
          (dom/div #js {:className "combo-view__text"} text))))))

;; Life counter view. Includes the combo damage readout, a health-view for each
;; player, damage buttons, and a toolbar.
(defn life-counter-view [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "LifeCounterView")
    om/IRender
    (render [_]
      (dom/div #js {:className "life-counter"}
        ; toolbar
        (om/build toolbar-view app)
        ; health bars and combo running total ("VS" button when idle)
        (om/build combo-view (:running-total app))
        (apply dom/ul #js {:className "players"}
          (om/build-all health-view
            (mapv (fn [player] ; build health-view arguments per player
                    {:player player
                     :current-player-id (:current-player-id app)
                     :select-player (partial data/select-player app player)})
                  (:players app))))
        ; damage buttons
        (om/build damage-buttons-view nil)))))

