(ns alpha-counter.views.life-counter
  (:require [alpha-counter.data :as data]
            [alpha-counter.views.util :refer [classes html-container]]
            [alpha-counter.abilities :as abilities]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

;; Takes a properties hash {:text string, :on-click fn}. When first clicked,
;; adds the "activated" class to the button for ~1s. If clicked again
;; within this time, executes :on-click.
(defn- two-stage-button [{:keys [text on-click]} owner]
  (reify
    om/IDisplayName
    (display-name [_] "TwoStageButton")
    om/IInitState
    (init-state [_] {:activated false})
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

(defn- ability-button [[text f]]
  (html [:button {:class "button small ability" :key text :on-click f} text]))

(defn- toolbar [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "ToolbarView")
    om/IRender
    (render [_]
      (html
        [:div {:class "toolbar"}
         (om/build two-stage-button {:text "Character Select" :on-click data/return-to-character-select!})
         (om/build two-stage-button {:text "Rematch" :on-click data/rematch!})
         [:button {:class "button small ghost" :on-click data/undo!} "Undo"]
         (html-container
           [:div {:class "abilities"}]
           (mapv ability-button (abilities/active)))]))))

;; Returns the player's health as a float ratio, between 1.0 and 0.0.
(defn- health-ratio [player]
  (let [max-health (data/max-health player)
        health (:health player)]
    (if (zero? max-health) 0 (/ health max-health))))

;; Returns the player's health as a percentage string ending in "%", rounded
;; up; e.g. a player at 50/80 health would return "63%". Used for the
;; .health__health inline width.
(defn- health-percent [player]
  (-> (health-ratio player) (* 100) Math/ceil (str "%")))

;; Health bar row. Expects props {:player p, :current-player-id k,
;; :select-player fn, :side "left"|"right"}. The CSS handles left/right
;; anchoring of the fill (via .health--left / .health--right), so we no
;; longer set marginLeft.
(defn- health [{:keys [player current-player-id select-player side]}]
  (let [current (= (:id player) current-player-id)
        ratio (max 0 (health-ratio player))
        raw-hp (:health player)
        ko? (and (some? raw-hp) (not (pos? raw-hp)))
        hp (if ko? 0 raw-hp)
        max-hp (data/max-health player)
        danger? (and (pos? hp) (<= ratio 0.25))
        character-name (:name (:character player))
        cls (classes "health"
                     (str "health--" side)
                     (when current "current")
                     (when danger? "danger")
                     (when ko? "ko"))]
    (html
      [:li {:class cls
            :key (name (:id player))
            :on-click select-player}
           ;; Name — locked SVG fit-to-width in portrait, plain text in
           ;; landscape. Both render; a container query on .life-counter
           ;; toggles which is visible (see .health__name--svg in the CSS).
           [:div {:class "health__name health__name--svg"}
            [:svg {:class "health__name-svg name-portrait"
                   :view-box "0 0 220 34"
                   :preserve-aspect-ratio "none"
                   :aria-label character-name}
             [:text {:x 0 :y 27
                     :text-length 220
                     :length-adjust "spacingAndGlyphs"}
              character-name]]
            [:span {:class "health__name-text name-landscape"
                    :aria-hidden "true"}
             character-name]]
           ;; Bar rail wrapped in .health__bar-row so the current-player
           ;; triangle indicator (CSS ::before) can anchor to the bar's
           ;; vertical center and sit just outside it in the padding gutter.
           [:div {:class "health__bar-row"}
            [:div {:class "health__health-bar"}
             [:div {:class "health__damage"}]
             [:div {:class "health__health"
                    :style (js-obj "width" (health-percent player))}]
             ;; React 0.12 doesn't pass through CSS custom properties in the
             ;; style prop, so compute the edge's `left` percent inline (the
             ;; CSS otherwise uses calc(var(--ratio) * 100%)).
             [:div {:class "health__edge"
                    :style (js-obj "left" (str (* (if (= side "left")
                                                    (- 1 ratio)
                                                    ratio)
                                                  100) "%"))}]
             (when ko?
               [:div {:class "health__ko"} "K.O."])]]
           ;; HP readout — split into current + /max
           [:div {:class "health__number"}
            [:span {:class "health__number-cur"} hp]
            [:span {:class "health__number-max"} (str "/" max-hp)]]])))

;; When a combo is in progress, display the running total with an explicit
;; sign; otherwise display "VS" (or "KO" once a fighter has been knocked out).
;; A "N HITS" subtitle hangs below the disc while a combo is in progress.
(defn- combo [{:keys [running-total ko? hits]} owner]
  (reify
    om/IDisplayName
    (display-name [_] "ComboView")
    om/IRender
    (render [_]
      (let [idle (#{0 :reset} running-total)
            healing (and (not idle) (neg? running-total))
            magnitude (Math/abs running-total)
            text (cond
                   (and idle ko?) "KO"
                   idle "VS"
                   healing (str "+" magnitude)
                   :else (str "−" magnitude))
            active-state (cond healing "healing"
                               (not idle) "damage")
            show-hits? (and (not idle) (pos? (or hits 0)))]
        (html [:div {:class (classes "combo" active-state)}
               [:div {:class "combo__text"} text]
               (when show-hits?
                 [:div {:class "combo__hits"}
                  (str hits " HIT" (when (not= hits 1) "S"))])])))))

;; A damage/heal button: negative amounts get the .heal modifier and a
;; "HEAL" tag.
(defn- damage-button [n]
  (let [heal? (neg? n)
        label (if heal? (str "+" (Math/abs n)) (str n))
        cls (classes "button small" (when heal? "heal"))]
    (html
      [:li {:key (str n)}
       [:button {:class cls
                 :on-click #(data/register-hit n)}
        label
        (when heal?
          [:span {:class "heal-tag"} "HEAL"])]])))

;; Life counter view.
(defn main [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "LifeCounterView")
    om/IRender
    (render [_]
      (let [players (:players app)]
        (html
          [:div {:class "life-counter name-mode--svg"}
           (om/build toolbar app)
           ;; .arena scopes the combo disc's absolute positioning to the
           ;; bar area, so a wrapped (multi-row) toolbar doesn't leave the
           ;; disc stranded in the toolbar zone.
           [:div {:class "arena"}
            (om/build combo {:running-total (:running-total app)
                             :hits (:hits app)
                             :ko? (boolean
                                    (some (fn [{:keys [health]}]
                                            (and health (not (pos? health))))
                                          (:players app)))})
            (html-container
              [:ul {:class "players"}]
              (mapv (fn [player side]
                      (health {:player player
                               :current-player-id (:current-player-id app)
                               :select-player (partial data/select-player app player)
                               :side side}))
                    players
                    ["left" "right"]))]
           (html-container
             [:ul {:class "damage-buttons"}]
             (mapv damage-button data/damage-amounts))])))))
