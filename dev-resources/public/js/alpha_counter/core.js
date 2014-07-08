// Compiled by ClojureScript 0.0-2234
goog.provide('alpha_counter.core');
goog.require('cljs.core');
goog.require('om.dom');
goog.require('om.dom');
goog.require('om.core');
goog.require('om.core');
cljs.core.enable_console_print_BANG_.call(null);
alpha_counter.core.characters = new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1017277949),"Grave",new cljs.core.Keyword(null,"health","health",4087608782),90], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1017277949),"Jaina",new cljs.core.Keyword(null,"health","health",4087608782),85], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1017277949),"Rook",new cljs.core.Keyword(null,"health","health",4087608782),100], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1017277949),"Midori",new cljs.core.Keyword(null,"health","health",4087608782),95], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1017277949),"Setsuki",new cljs.core.Keyword(null,"health","health",4087608782),70], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1017277949),"Valerie",new cljs.core.Keyword(null,"health","health",4087608782),85], null)], null);
alpha_counter.core.app_state = cljs.core.atom.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ready","ready",1122290965),false,new cljs.core.Keyword(null,"current-player","current-player",2351550759),null,new cljs.core.Keyword(null,"players","players",520336676),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentArrayMap.EMPTY,cljs.core.PersistentArrayMap.EMPTY], null)], null));
alpha_counter.core.select_character = (function select_character(player,character){return om.core.transact_BANG_.call(null,player,(function (p1__7534_SHARP_){return cljs.core.assoc.call(null,p1__7534_SHARP_,new cljs.core.Keyword(null,"character","character",2578099867),character,new cljs.core.Keyword(null,"health","health",4087608782),new cljs.core.Keyword(null,"health","health",4087608782).cljs$core$IFn$_invoke$arity$1(character),new cljs.core.Keyword(null,"history","history",1940838406),cljs.core.PersistentVector.EMPTY);
}));
});
alpha_counter.core.ready = (function ready(app){return om.core.transact_BANG_.call(null,app,(function (p1__7535_SHARP_){return cljs.core.assoc.call(null,p1__7535_SHARP_,new cljs.core.Keyword(null,"ready","ready",1122290965),true);
}));
});
alpha_counter.core.character_select_view = (function character_select_view(app,owner){if(typeof alpha_counter.core.t7539 !== 'undefined')
{} else
{
/**
* @constructor
*/
alpha_counter.core.t7539 = (function (owner,app,character_select_view,meta7540){
this.owner = owner;
this.app = app;
this.character_select_view = character_select_view;
this.meta7540 = meta7540;
this.cljs$lang$protocol_mask$partition1$ = 0;
this.cljs$lang$protocol_mask$partition0$ = 393216;
})
alpha_counter.core.t7539.cljs$lang$type = true;
alpha_counter.core.t7539.cljs$lang$ctorStr = "alpha-counter.core/t7539";
alpha_counter.core.t7539.cljs$lang$ctorPrWriter = (function (this__4105__auto__,writer__4106__auto__,opt__4107__auto__){return cljs.core._write.call(null,writer__4106__auto__,"alpha-counter.core/t7539");
});
alpha_counter.core.t7539.prototype.om$core$IRender$ = true;
alpha_counter.core.t7539.prototype.om$core$IRender$render$arity$1 = (function (_){var self__ = this;
var ___$1 = this;var p1 = cljs.core.first.call(null,new cljs.core.Keyword(null,"players","players",520336676).cljs$core$IFn$_invoke$arity$1(self__.app));var p2 = cljs.core.second.call(null,new cljs.core.Keyword(null,"players","players",520336676).cljs$core$IFn$_invoke$arity$1(self__.app));var icons = ((function (p1,p2,___$1){
return (function (player){return cljs.core.map.call(null,((function (p1,p2,___$1){
return (function (c){return React.DOM.button({"onClick": ((function (p1,p2,___$1){
return (function (){return alpha_counter.core.select_character.call(null,player,c);
});})(p1,p2,___$1))
},new cljs.core.Keyword(null,"name","name",1017277949).cljs$core$IFn$_invoke$arity$1(c));
});})(p1,p2,___$1))
,alpha_counter.core.characters);
});})(p1,p2,___$1))
;return React.DOM.div(null,React.DOM.h1(null,"Character Select"),React.DOM.h2(null,"Player One"),cljs.core.apply.call(null,om.dom.div,null,icons.call(null,p1)),React.DOM.h2(null,"Player Two"),cljs.core.apply.call(null,om.dom.div,null,icons.call(null,p2)),React.DOM.button({"disabled": cljs.core.some.call(null,cljs.core.empty_QMARK_,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [p1,p2], null)), "onClick": ((function (p1,p2,icons,___$1){
return (function (){return alpha_counter.core.ready.call(null,self__.app);
});})(p1,p2,icons,___$1))
},"Start!"));
});
alpha_counter.core.t7539.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_7541){var self__ = this;
var _7541__$1 = this;return self__.meta7540;
});
alpha_counter.core.t7539.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_7541,meta7540__$1){var self__ = this;
var _7541__$1 = this;return (new alpha_counter.core.t7539(self__.owner,self__.app,self__.character_select_view,meta7540__$1));
});
alpha_counter.core.__GT_t7539 = (function __GT_t7539(owner__$1,app__$1,character_select_view__$1,meta7540){return (new alpha_counter.core.t7539(owner__$1,app__$1,character_select_view__$1,meta7540));
});
}
return (new alpha_counter.core.t7539(owner,app,character_select_view,null));
});
alpha_counter.core.life_counter_view = (function life_counter_view(player,owner){return React.DOM.h1(null,"Coming soon!");
});
alpha_counter.core.main_view = (function (){var method_table__4404__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);var prefer_table__4405__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);var method_cache__4406__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);var cached_hierarchy__4407__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);var hierarchy__4408__auto__ = cljs.core.get.call(null,cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"hierarchy","hierarchy",3129050535),cljs.core.get_global_hierarchy.call(null));return (new cljs.core.MultiFn("main-view",((function (method_table__4404__auto__,prefer_table__4405__auto__,method_cache__4406__auto__,cached_hierarchy__4407__auto__,hierarchy__4408__auto__){
return (function (app,_){return new cljs.core.Keyword(null,"ready","ready",1122290965).cljs$core$IFn$_invoke$arity$1(app);
});})(method_table__4404__auto__,prefer_table__4405__auto__,method_cache__4406__auto__,cached_hierarchy__4407__auto__,hierarchy__4408__auto__))
,new cljs.core.Keyword(null,"default","default",2558708147),hierarchy__4408__auto__,method_table__4404__auto__,prefer_table__4405__auto__,method_cache__4406__auto__,cached_hierarchy__4407__auto__));
})();
cljs.core._add_method.call(null,alpha_counter.core.main_view,false,(function (app,owner){return alpha_counter.core.character_select_view.call(null,app,owner);
}));
cljs.core._add_method.call(null,alpha_counter.core.main_view,true,(function (app,owner){return alpha_counter.core.life_counter_view.call(null,new cljs.core.Keyword(null,"current-player","current-player",2351550759).cljs$core$IFn$_invoke$arity$1(app),owner);
}));
om.core.root.call(null,alpha_counter.core.main_view,alpha_counter.core.app_state,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"target","target",4427965699),document.getElementById("main")], null));
