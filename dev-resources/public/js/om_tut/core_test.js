// Compiled by ClojureScript 0.0-2234
goog.provide('om_tut.core_test');
goog.require('cljs.core');
goog.require('om_tut.core');
goog.require('om_tut.core');
goog.require('cemerick.cljs.test');
goog.require('cemerick.cljs.test');
om_tut.core_test.foo_test = (function foo_test(){return cemerick.cljs.test.test_function.call(null,om_tut.core_test.foo_test);
});
om_tut.core_test.foo_test = cljs.core.vary_meta.call(null,om_tut.core_test.foo_test,cljs.core.assoc,new cljs.core.Keyword(null,"name","name",1017277949),new cljs.core.Symbol(null,"foo-test","foo-test",-1310892302,null),new cljs.core.Keyword(null,"test","test",1017460740),(function foo_test_test(){var _STAR_testing_contexts_STAR_5934 = cemerick.cljs.test._STAR_testing_contexts_STAR_;try{cemerick.cljs.test._STAR_testing_contexts_STAR_ = cljs.core.conj.call(null,cemerick.cljs.test._STAR_testing_contexts_STAR_,"I don't do a lot\n");
var _STAR_testing_contexts_STAR_5935 = cemerick.cljs.test._STAR_testing_contexts_STAR_;try{cemerick.cljs.test._STAR_testing_contexts_STAR_ = cljs.core.conj.call(null,cemerick.cljs.test._STAR_testing_contexts_STAR_,"Edge cases\n");
var _STAR_testing_contexts_STAR_5936 = cemerick.cljs.test._STAR_testing_contexts_STAR_;try{cemerick.cljs.test._STAR_testing_contexts_STAR_ = cljs.core.conj.call(null,cemerick.cljs.test._STAR_testing_contexts_STAR_,"(foo str)");
try{var values__5798__auto___5939 = cljs.core._conj.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,om_tut.core.foo.call(null,"")),"ClojureScript!");var result__5799__auto___5940 = cljs.core.apply.call(null,cljs.core._EQ_,values__5798__auto___5939);if(cljs.core.truth_(result__5799__auto___5940))
{cemerick.cljs.test.do_report.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1017479852),new cljs.core.Keyword(null,"pass","pass",1017337731),new cljs.core.Keyword(null,"expected","expected",3373152810),cljs.core.list(new cljs.core.Symbol(null,"=","=",-1640531466,null),"ClojureScript!",cljs.core.list(new cljs.core.Symbol(null,"foo","foo",-1640429953,null),"")),new cljs.core.Keyword(null,"actual","actual",3885931776),cljs.core.cons.call(null,cljs.core._EQ_,values__5798__auto___5939),new cljs.core.Keyword(null,"message","message",1968829305),null], null));
} else
{cemerick.cljs.test.do_report.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1017479852),new cljs.core.Keyword(null,"fail","fail",1017039504),new cljs.core.Keyword(null,"expected","expected",3373152810),cljs.core.list(new cljs.core.Symbol(null,"=","=",-1640531466,null),"ClojureScript!",cljs.core.list(new cljs.core.Symbol(null,"foo","foo",-1640429953,null),"")),new cljs.core.Keyword(null,"actual","actual",3885931776),cljs.core._conj.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.cons.call(null,new cljs.core.Symbol(null,"=","=",-1640531466,null),values__5798__auto___5939)),new cljs.core.Symbol(null,"not","not",-1640422260,null)),new cljs.core.Keyword(null,"message","message",1968829305),null], null));
}
}catch (e5937){if((e5937 instanceof Error))
{var t__5835__auto___5941 = e5937;cemerick.cljs.test.do_report.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1017479852),new cljs.core.Keyword(null,"error","error",1110689146),new cljs.core.Keyword(null,"expected","expected",3373152810),cljs.core.list(new cljs.core.Symbol(null,"=","=",-1640531466,null),"ClojureScript!",cljs.core.list(new cljs.core.Symbol(null,"foo","foo",-1640429953,null),"")),new cljs.core.Keyword(null,"actual","actual",3885931776),t__5835__auto___5941,new cljs.core.Keyword(null,"message","message",1968829305),null], null));
} else
{if(new cljs.core.Keyword(null,"else","else",1017020587))
{throw e5937;
} else
{}
}
}try{var values__5798__auto__ = cljs.core._conj.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,om_tut.core.foo.call(null,null)),"Hello, ClojureScript!");var result__5799__auto__ = cljs.core.apply.call(null,cljs.core._EQ_,values__5798__auto__);if(cljs.core.truth_(result__5799__auto__))
{cemerick.cljs.test.do_report.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1017479852),new cljs.core.Keyword(null,"pass","pass",1017337731),new cljs.core.Keyword(null,"expected","expected",3373152810),cljs.core.list(new cljs.core.Symbol(null,"=","=",-1640531466,null),"Hello, ClojureScript!",cljs.core.list(new cljs.core.Symbol(null,"foo","foo",-1640429953,null),null)),new cljs.core.Keyword(null,"actual","actual",3885931776),cljs.core.cons.call(null,cljs.core._EQ_,values__5798__auto__),new cljs.core.Keyword(null,"message","message",1968829305),null], null));
} else
{cemerick.cljs.test.do_report.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1017479852),new cljs.core.Keyword(null,"fail","fail",1017039504),new cljs.core.Keyword(null,"expected","expected",3373152810),cljs.core.list(new cljs.core.Symbol(null,"=","=",-1640531466,null),"Hello, ClojureScript!",cljs.core.list(new cljs.core.Symbol(null,"foo","foo",-1640429953,null),null)),new cljs.core.Keyword(null,"actual","actual",3885931776),cljs.core._conj.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.cons.call(null,new cljs.core.Symbol(null,"=","=",-1640531466,null),values__5798__auto__)),new cljs.core.Symbol(null,"not","not",-1640422260,null)),new cljs.core.Keyword(null,"message","message",1968829305),null], null));
}
return result__5799__auto__;
}catch (e5938){if((e5938 instanceof Error))
{var t__5835__auto__ = e5938;return cemerick.cljs.test.do_report.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1017479852),new cljs.core.Keyword(null,"error","error",1110689146),new cljs.core.Keyword(null,"expected","expected",3373152810),cljs.core.list(new cljs.core.Symbol(null,"=","=",-1640531466,null),"Hello, ClojureScript!",cljs.core.list(new cljs.core.Symbol(null,"foo","foo",-1640429953,null),null)),new cljs.core.Keyword(null,"actual","actual",3885931776),t__5835__auto__,new cljs.core.Keyword(null,"message","message",1968829305),null], null));
} else
{if(new cljs.core.Keyword(null,"else","else",1017020587))
{throw e5938;
} else
{return null;
}
}
}}finally {cemerick.cljs.test._STAR_testing_contexts_STAR_ = _STAR_testing_contexts_STAR_5936;
}}finally {cemerick.cljs.test._STAR_testing_contexts_STAR_ = _STAR_testing_contexts_STAR_5935;
}}finally {cemerick.cljs.test._STAR_testing_contexts_STAR_ = _STAR_testing_contexts_STAR_5934;
}}));
cemerick.cljs.test.register_test_BANG_.call(null,new cljs.core.Symbol(null,"om-tut.core-test","om-tut.core-test",-169093137,null),om_tut.core_test.foo_test);
