(ns alpha-counter.views.character-select
  (:require [alpha-counter.data :as data]
            [sablono.core :as html :refer-macros [html]]
            [alpha-counter.views.util :refer [classes html-container]]
            [om.core :as om :include-macros true]))

;; State of the picks at any time:
;;   :empty        – no characters chosen yet
;;   :p1-selected  – P1 has chosen, P2 has not
;;   :ready        – both have chosen
(defn- pick-state [app]
  (let [[p1 p2] (:players app)]
    (cond
      (nil? (:character p1)) :empty
      (nil? (:character p2)) :p1-selected
      :else :ready)))

;; A single character chip. Highlights itself based on which (if any) player
;; owns it; dims itself when the current state-machine state can't act on it.
(defn- character-chip [app state character]
  (let [[p1 p2] (:players app)
        c1 (:character p1)
        c2 (:character p2)
        for-p1? (= c1 character)
        for-p2? (= c2 character)
        ex? (or (and for-p1? (:ex p1))
                (and for-p2? (:ex p2)))
        ;; In :ready, only the two selected chips are actionable.
        disabled? (and (= state :ready) (not for-p1?) (not for-p2?))
        cls (classes "button small"
                     (when (or for-p1? for-p2?) "selected")
                     (when for-p1? "p1")
                     (when for-p2? "p2")
                     (when ex? "ex")
                     (when disabled? "taken"))
        hp-text (str (:health character)
                     (when (:ex-health character)
                       (str "/" (:ex-health character))))]
    (html
      [:li {:key (:name character)}
       [:button {:class cls
                 :disabled disabled?
                 :on-click #(data/select-character! app character)}
        [:span {:class "char-name"} (:name character)]
        [:span {:class "char-hp"} hp-text]]])))

;; A single pick summary slot (P1 or P2). When the player has chosen, shows
;; name + (max) HP and an optional EX badge; otherwise an empty placeholder.
(defn- pick-summary [slot player]
  (let [picked? (some? (:character player))
        ex? (and picked? (:ex player))]
    (html
      [:div {:class (str "cs-pick cs-pick--p" slot)
             :key slot}
       [:span {:class "cs-pick__slot"} (str "P" slot)]
       (if picked?
         [:span {:class "cs-pick__body"}
          (when ex?
            [:em {:class "cs-pick__ex"} "EX"])
          [:span {:class "cs-pick__name"} (-> player :character :name)]
          [:span {:class "cs-pick__hp"}
           (str (data/max-health player) " HP")]]
         [:span {:class "cs-pick__empty"} "— no fighter —"])])))

;; Top-level character-select view. One chip grid; the click handler is
;; state-machine-driven (data/select-character!). Footer has Reset (enabled
;; once anything is picked) and Fight (enabled only when both are picked).
(defn main [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "CharacterSelectView")
    om/IRender
    (render [_]
      (let [[p1 p2] (:players app)
            state (pick-state app)
            anything-picked? (not= state :empty)
            ready? (= state :ready)]
        (html
          [:div {:class "character-select"}
           [:header {:class "cs-header"}
            [:span {:class "cs-brand-mark" :aria-hidden "true"} "▮▮"]
            [:h1 "Character Select"]
            [:p {:class "cs-tag"} "Pick your fighter."]]
           [:div {:class "cs-picks"}
            (pick-summary "1" p1)
            (pick-summary "2" p2)]
           (html-container
             [:ul {:class "list"}]
             (mapv (partial character-chip app state) data/characters))
           [:div {:class "cs-actions"}
            [:button {:class "button cs-reset"
                      :on-click #(when anything-picked? (data/reset-picks! app))
                      :disabled (not anything-picked?)}
             "Reset"]
            [:button {:class (classes "button cs-start" (when ready? "ready"))
                      :on-click #(when ready? (data/ready! app))
                      :disabled (not ready?)}
             (if ready? "Fight!" "Pick both fighters")]]])))))
