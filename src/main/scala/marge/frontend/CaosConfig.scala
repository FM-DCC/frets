package marge.frontend

import caos.frontend.Configurator.*
import caos.frontend.{Configurator, Documentation}
import caos.sos.{FinAut, SOS}
import caos.view.*
import marge.backend.*
import marge.syntax.FExp.{FNot, Feat}
import marge.syntax.FRTS.toMermaid
import marge.syntax.RTS.{Action, Edge, QName}
import marge.syntax.{FExp, FRTS, Parser, Show, Syntax}
//import marge.syntax.Syntax.RxGraph
import marge.syntax.RTS
import marge.backend.RxSemantics

/** Object used to configure which analysis appear in the browser */
object CaosConfig extends Configurator[FRTS]:
  val name = "FRETS: Animator of Featured Reactive Transition Systems"
  override val languageName: String = "Input Featured Reactive TS"

  val parser: String => FRTS = marge.syntax.Parser.parseProgram

  /** Examples of programs that the user can choose from. The first is the default one. */
  val examples: Seq[Example] = List(
    "Ex.2: simple TS pr1" -> "// TS: Flatenned \"Simple FRTS\"\n// after selecting product 1,\n// with feature f1\ninit s0\ns0a --> s0: a "
      -> "Simple TS, obtained from the simple FRTS example after product 1, selecting feature f1. Presented in Fig. 1 and Example 2 in the companion paper.",
    "Ex.2: simple TS pr2" -> "// TS: Flatenned \"Simple FRTS\"\n// after selecting product 2,\n// with features f1 and f2,\n// and minimising it\ninit s0\ns0 --> sa: a \ns0 --> sb: b \nsa --> sab: b\nsb --> sab: a\nsb --> sb: b\nsab --> sab: b\nsb --> sc: c\nsab --> sc: c"
      -> "Simple TS, obtained from the simple FRTS example after product 2, with features f1 and f2, and minimising it. Presented in Fig. 1 and Example 2 in the companion paper.",
    "Ex.3: vending TS" -> "// Flatenned TS to model a vending machine\n// from the vending FRTS after selecting\n// features S, T, and P.\ninit e4\nx1 --> x2: pay\nx2 --> x3: change\nx3 --> x4: cancel\nx4 --> x1: return\nx3 --> x5: soda\nx3 --> x6: tea\nx5 --> x7: serve\nx6 --> x7: serve\nx7 --> x8: open\nx8 --> x9: take\nx9 --> x1: close\nx1 --> x1: sodaRefill\nx1 --> x1: teaRefill\ny1 --> y2: pay\ny2 --> y3: change\ny3 --> y4: cancel\ny4 --> y1: return\ny3 --> y5: tea\ny5 --> y6: serve\ny6 --> y7: open\ny7 --> y8: take\ny8 --> y1: close\ny1 --> y1: teaRefill\nz1 --> z2: pay\nz2 --> z3: change\nz3 --> z4: cancel\nz4 --> z1: return\nz3 --> z5: soda\nz5 --> z6: serve\nz6 --> z7: open\nz7 --> z8: take\nz8 --> z1: close\nz1 --> z1: sodaRefill\ne1 --> e2: open\ne2 --> e3: take\ne3 --> e4: close\nx5 --> y6: serveSodaGone\nx6 --> z6: serveTeaGone\ny5 --> e1: serveTeaGone\nz5 --> e1: serveSodaGone\ne4 --> y1: teaRefill\ne4 --> z1: sodaRefill\ny1 --> x1: sodaRefill\nz1 --> x1: teaRefill"
      -> "TS obtained from flatenning the FRTS Vending example after the selecting product with features S and P. Presented in Fig. 2a and Example 3 in the companion paper.",
    "Ex.5: perm TS"
      -> "init s\ns --> sa: a\ns --> sb: b\ns --> sc: c\nsa --> sab: b\nsa --> sac: c\nsb --> sab: a\nsb --> sbc: c\nsc --> sac: a\nsc --> sbc: b\nsab --> sabc: c\nsac --> sabc: b\nsbc --> sabc: a"
      -> "TS that accepts all permutations of the actions a,b,c and their prefixes.",
    "Ex.6: simple FTS" -> "// FTS: Flatenned \"Simple FRTS\"\n// without selecting any product\ninit s0\ns0  --> sa:  a if f1\ns0  --> sb:  b if f2\nsa  --> sab: b if f2\nsb  --> sab: a if f1\nsb  --> sb:  b if f2\nsab --> sab: b if f2\nsb  --> sc:  c if f2\nsab --> sc:  c if f2\n\nfm f1\nselect f1,f2; // try also just \"f1\""
      -> "Simple FTS, obtained from the simple FRTS example without selecting any product. Presented in Fig. 1 and Example 6 in the companion paper.",
    "Ex.7: vending FTS" -> "init e4\nx1 --> x2: pay if P\nx2 --> x3: change\nx3 --> x4: cancel\nx4 --> x1: return\nx3 --> x5: soda\nx3 --> x6: tea\nx5 --> x7: serve\nx6 --> x7: serve\nx7 --> x8: open\nx8 --> x9: take\nx9 --> x1: close\nx1 --> x6: tea if !P\nx1 --> x5: soda if !P\nx1 --> x1: sodaRefill\nx1 --> x1: teaRefill\ny1 --> y2: pay if P\ny2 --> y3: change\ny3 --> y4: cancel\ny4 --> y1: return\ny3 --> y5: tea\ny5 --> y6: serve\ny6 --> y7: open\ny7 --> y8: take\ny8 --> y1: close\ny1 --> y5: tea if !P\ny1 --> y1: teaRefill\nz1 --> z2: pay if P\nz2 --> z3: change\nz3 --> z4: cancel\nz4 --> z1: return\nz3 --> z5: soda\nz5 --> z6: serve\nz6 --> z7: open\nz7 --> z8: take\nz8 --> z1: close\nz1 --> z5: soda if !P\nz1 --> z1: sodaRefill\ne1 --> e2: open\ne2 --> e3: take\ne3 --> e4: close\nx5 --> y6: serveSodaGone\nx6 --> z6: serveTeaGone\ny5 --> e1: serveTeaGone\nz5 --> e1: serveSodaGone\ne4 --> y1: teaRefill if T\ne4 --> z1: sodaRefill if S\ny1 --> x1: sodaRefill if S\nz1 --> x1: teaRefill if T\nfm S || T\nselect S,T,P;"
      -> "FTS obtained from flatenning the FRTS Vending example before selecting any product. Presented in Fig. 2a and Example 7 in the companion paper.",
    "Ex.8: perm FTS"
      -> "// Action-permutation FTS, which \n// recognises the language given\n// by all perumations of {a,b,c}\n// and their prefixes. Feature selection\n// restrict the length of the TSs.\ninit s\ns --> sa: a     if f1\ns --> sb: b     if f1\ns --> sc: c     if f1\nsa --> sab: b   if f2\nsa --> sac: c   if f2\nsb --> sab: a   if f2\nsb --> sbc: c   if f2\nsc --> sac: a   if f2\nsc --> sbc: b   if f2\nsab --> sabc: c if f3\nsac --> sabc: b if f3\nsbc --> sabc: a if f3\nfm (f3 -> f2) && (f2 -> f1)\nselect f1,f2,f3;"
      -> "FTS that accepts all permutations of the actions a,b,c and their prefixes. Feature selection restricts the length of the TSs. Presented in Example 8 in the companion paper.",
    "Ex.9: simple RTS pr1" -> "// RTS: Simple FRTS after\n// selecting product 1, with\n// feature f1\ninit s0\ns0 --> s0: a \na --x a"
      -> "Simple RTS, obtained from the simple FRTS example after product 1, selecting feature f1. Presented in Fig. 1 and Example 9 in the companion paper.",
    "Ex.9: simple RTS pr2" -> "// RTS: Simple FRTS after\n// selecting product 2, with\n// features f1 and f2\ninit s0\ns0 --> s0: a \ns0 --> s0: b \ns0 --> s1: c disabled\na --x a\nb ->> c"
      -> "Simple RTS, obtained from the simple FRTS example after selecting product 2, with features f1 and f2. Presented in Fig. 1 and Example 9 in the companion paper.",
    "Ex.11: vending RTS" -> "init s1\ns1 --> s1: sodaRefill\ns1 --> s1: teaRefill\ns1 --> s2: pay\ns4 --> s1: return\ns2 --> s3: change\ns3 --> s4: cancel\ns3 --> s5: soda\ns3 --> s6: tea\ns5 --> s7: serve\ns5 --> s7: serveSodaGone\ns6 --> s7: serve\ns6 --> s7: serveTeaGone\ns7 --> s8: open\ns8 --> s9: take\ns9 --> s1: close\nsodaRefill ->> soda\nteaRefill ->> tea\nserveSodaGone --x soda\nserveTeaGone --x tea"
      -> "Vending machine, implemented using an RTS. Presented in Fig. 3a and Example 11 in the companion paper.",
    "Ex.12: perm RTS"
      -> "// Action-permutation RTS, which \n// recognises the language given\n// by all perumations of {a,b,c}\n// and their subsets. Variation that\n// exploits reactivity to forbid\n// multiple occurrences of each action\ninit s\ns --> s: a\ns --> s: b\ns --> s: c\na --x a\nb --x b\nc --x c"
      -> "RTS that accepts all permutations of the actions a,b,c and their prefixes. Reactivity is used to disable multiple occurrences of each action. Presented in Example 12 in the companion paper.",
    "Ex.13 simple FRTS" -> "init s0\ns0 --> s0: a if f1\ns0 --> s0: b if f2\ns0 --> s1: c if f2 disabled\na --x a\nb ->> c\n\nfm f1\nselect f1,f2; // try also just \"f1\""
      -> "Simple illustrative example of an FRTS, used to motivate the core ideas. Presented in Fig. 1 and Example 13 in the companion paper.",
    "Ex.15: vending FRTTS" -> "init s1\ns1 --> s1: sodaRefill if S\ns1 --> s1: teaRefill if T\ns1 --> s2: pay if P\ns4 --> s1: return\ns2 --> s3: change\ns3 --> s4: cancel\ns3 --> s5: soda if S\ns3 --> s6: tea  if T\ns1 --> s5: soda if !P\ns1 --> s6: tea  if !P\ns5 --> s7: serve\ns5 --> s7: serveSodaGone\ns6 --> s7: serve\ns6 --> s7: serveTeaGone\ns7 --> s8: open\ns8 --> s9: take\ns9 --> s1: close\nsodaRefill ->> soda\nteaRefill ->> tea\nserveSodaGone --x soda\nserveTeaGone --x tea\nfm S || T\nselect S,T,P;"
      -> "FRTS version of the vending machine example, presented in Fig. 3b and Example 15 in the companion paper.",
    // "Simple FRTS 2" -> "init s0\ns0 --> s1: a\ns1 --> s0: b\na  --! a"
    //   -> "Simpler variation of the simple FRTS example, without features",
    // "Simple FRTS 3" -> "init s0\ns0 --> s0: a if f1\ns0 --> s0: b if f2\na --x a\nb --x b\n\nfm f1\nselect f1,f2; // try also just \"f1\""
    //   -> "Another variation of the simple FRTS example",
    // "Simple FRTS 4" -> "init s0\ns0 --> s0: a if f1\ns0 --> s0: b if f2\ns0 --> s1: c if f2\na --x a\nb --x b\na --x c\nb ->> c\n\nfm f1\nselect f1,f2; // try also just \"f1\""
    //   -> "Forth (slightly larger) illustrative example of an FRTS, used to motivate the core ideas",
    "Simple FRTS (w/o shortcuts)" -> "init s0\n[e1] s0 --> s0: a if f1\n[e2] s0 --> s0: b if f2\n[e3] s0 --> s1: b if f2 disabled\n\ne1 --x e1\ne2 ->> e3\n\nfm f1\nselect f1,f2; // try also just \"f1\""
      -> "Variation of the simple FRTS example, using aliases for edges in the reaction definitions",
    "FM experiment" -> "init s0\ns0 --> s1: a if sec\ns1 --> s0: b if !sec\na  --! a\n\nfm fa -> fb && (!fa || fb)\nselect sec,fb;"
      -> "Experimenting with FM solutions",
//    "Counter" -> "init s0\ns0 --> s0 : act\nact --! act : offAct disabled\nact ->> offAct : on1 disabled\nact ->> on1"
//      -> "turns off a transition after 3 times.",
//    "Penguim" -> "init Son_of_Tweetie\nSon_of_Tweetie --> Special_Penguin\nSpecial_Penguin --> Penguin : Penguim\nPenguin --> Bird : Bird\nBird --> Does_Fly: Fly\n\nBird --! Fly : noFly\nPenguim --! noFly"
//      -> "Figure 7.4 in Dov M Gabbay, Cognitive Technologies Reactive Kripke Semantics",
//    "Vending (max 1eur)" -> "init Insert\nInsert --> Coffee : 50ct\nInsert --> Chocolate : 1eur\nCoffee --> Insert : Get_coffee\nChocolate --> Insert : Get_choc\n\n1eur --! 50ct\n1eur --! 1eur\n50ct --! 50ct : last50ct disabled\n50ct --! 1eur\n50ct ->> last50ct"
//      -> "Example of a vending machine, presented in a recently accepted companion paper at FACS 2024. There is a total of 1eur to be spent, and some transitions are deactivated when there is not enough money.",
//    "Vending (max 3prod)" -> "init pay\npay --> select : insert_coin\nselect --> soda : ask_soda\nselect --> beer : ask_beer\nsoda --> pay : get_soda\nbeer --> pay : get_beer\n\nask_soda --! ask_soda : noSoda disabled\nask_beer --! ask_beer : noBeer\nask_soda ->> noSoda"
//      -> "Variation of an example of a vending machine, presented in a recently accepted companion paper at FACS 2024. There is a total of 1 beer and 2 sodas available.",
    "Intrusive product" -> "aut s {\n  init 0\n  0 --> 1 : a\n  1 --> 2 : b\n  2 --> 0 : d disabled\n  a --! b\n}\naut w {\n  init 0\n  0 --> 1 : a\n  1 --> 0 : c\n  a --! a \n}\n// intrusion\nw.c ->> s.b",
    "Conflict" -> "init 0\n     0 --> 1: a\n[e2] 1 --> 2: b\n[e3] 2 --> 3: b disabled\n\na ->> b\na --! e3"
      -> "Possible conflict detected in the analysis.",
//    "Higher-edge" -> "init 0\n0 --> 1: a\n1 --> 2: b disabled\n2 --> 3: c disabled\na ->> b: on\non ->> c: off"
//      -> "Example of a hyper-edge from a higher level (from another hyper-edge).",
    // "Dependencies" -> "aut A {\n  init 0\n  0 --> 1: look\n  1 --> 0: restart\n}\n\naut B {\n  init 0\n  0 --> 1: on\n  1 --> 2: goLeft disabled\n  1 --> 2: goRight disabled\n  goLeft --#-- goRight\n  2 --> 0: off\n}\n\n// dependencies\nA.look ----> B.goLeft\nA.look ----> B.goRight"
    //   -> "Experimental syntax to describe dependencies, currently only as syntactic sugar.",
    "Dynamic SPL" -> "init setup\nsetup --> setup : Safe\nsetup --> setup : Unsafe\nsetup --> setup : Encrypt\nsetup --> setup : Dencrypt\nsetup --> ready\nready --> setup\nready --> received : Receive\nreceived --> routed_safe : ERoute  disabled\nreceived --> routed_unsafe : Route\nrouted_safe --> sent : ESend       disabled\nrouted_unsafe --> sent : Send\nrouted_unsafe --> sent_encrypt : ESend disabled\nsent_encrypt --> ready : Ready\nsent --> ready : Ready\n\nSafe ->> ERoute\nSafe --! Route\nUnsafe --! ERoute\nUnsafe ->> Route\nEncrypt --! Send\nEncrypt ->> ESend\nDencrypt ->> Send\nDencrypt --! ESend"
      -> "Example of a Dynamic Software Product Line, borrowed from Fig 1 in Maxime Cordy et al. <em>Model Checking Adaptive Software with Featured Transition Systems</em>",
    "NFA-DFA 1" -> "init 0\n0 --> 1: 0\n1 --> 0: 0\n1 --> 3: 1\n2 --> 1: 0\n2 --> 3: 1\n4 --> 3: 0\n4 --> 3: 1\n0 --> 3: 1\n3 --> 5: 0\n3 --> 5: 1\n5 --> 5: 0\n5 --> 5: 1"
      -> "Experimenting with determinisatoin and minimisation of automata",
    "NFA-DFA 2" -> "init q0\nq0 --> q0: a\nq0 --> q0: b\nq0 --> q1: a\nq1 --> q2: b"
      -> "Simple example of an NFA that could be determinised",
    "Min 1" -> "init q0\nq0 --> q1: a\nq1 --> q1: a" -> "Experiment to minimise automata",
    "Min 2" -> "init q0\nq0 --> q1: a\nq0 --> q2: b" -> "Experiment to minimise automata",
    "Parallel" -> "aut a {\n  init 0\n  0 --> 1 : a disabled\n}\naut b {\n  init 0\n  0 --> 1 : b0\n  1 --> 0 : b disabled\n}\naut c {\n  init 0\n  0 --> 1 : c0\n  1 --> 0 : c disabled\n}\n// intrusion\nb.b  ->> a.a\nc.c  ->> a.a\nb.b0 ->> c.c\nc.c0 ->> b.b\nb.b0 --#-- c.c0"
      -> "Experiments with multiple components.",
    // "Vending (FRTS)" -> "init s1\ns1 --> s1: sodaRefill\ns1 --> s1: teaRefill\ns1 --> s2: pay\ns4 --> s1: return\ns2 --> s3: change\ns3 --> s4: cancel\ns3 --> s5: soda\ns3 --> s6: tea\ns5 --> s7: serve\ns5 --> s7: serveSodaGone\ns6 --> s7: serve\ns6 --> s7: serveTeaGone\ns7 --> s8: open\ns8 --> s9: take\ns9 --> s1: close\n\nsodaRefill ->> soda\nteaRefill ->> tea\nserveSodaGone --x soda\nserveTeaGone --x tea"
    //   -> "Experiment from the ongoing paper",
  )

//   val a = Feat("a")
//   val b = Feat("b")
//   val c = Feat("c")
//   val d = Feat("d")
////   val test1 = List((a && b) || c, a && (b || c), a || b, a-->b)
//   val test1 = List(a, a&&b, a||b)
//   val test = test1 ++ test1.map(x => FNot(x))

   /** Description of the widgets that appear in the dashboard. */
   val widgets = List(
//     "View State (DB)" -> view[FRTS](_.toString, Text).expand,
     htmlLeft[FRTS]("""
            |<button class="tgBtn" id="frtsBtn">FRTS</button>
            |<button class="tgBtn" id="rtsBtn">RTS</button>
            |<button class="tgBtn" id="ftsBtn">FTS</button>
            |<button class="tgBtn" id="tsBtn">TS</button>
            |""".stripMargin),
     "View FRTS" -> view[FRTS](Show.apply, Text).moveTo(1),
     "View RTS variant" -> view[FRTS](x => Show(x.getRTS), Text).moveTo(1),
// <script>
//   const button = document.getElementById("tttoggleBtn");

//   button.addEventListener("click", () => {
//     const div = document.getElementById("id-344744587");
//     div.classList.toggle("hidden");
//     button.classList.toggle("offBt")
//   });
// </script>"""),
     html("<h2>Main functionalities</h2>"),
     // "View debug (simpler)" -> view[RxGraph](RxGraph.toMermaidPlain, Text).expand,
     // "View debug (complx)" -> view[RxGraph](RxGraph.toMermaid, Text).expand,
//     "experiment" -> view[FRTS](x => test.map(_.dnf).mkString("\n"), Text).expand,
//     "experiment2" -> view[FRTS](x => test.map(_.products(Set("a","b"))).mkString("\n"), Text).expand,
     "FRTS: draw" -> view[FRTS](g => toMermaid(g), Mermaid).expand,
     "RTS variant: Step-by-step" -> steps((e:FRTS)=>e.getRTS, RTSSemantics, RTS.toMermaid, _.show, Mermaid),
     "TS variant: flattened" -> lts((e:FRTS)=>e.getRTS,
       RTSSemantics,
       x => Show.simpler(x),//x.inits.toString,
       _.toString),
     "FTS: flattened" ->
       ltsCustom((e:FRTS)=> (
         Set(e.getRTS),
         RTSSemantics.asFTS(e.pk),
         x => Show.simpler(x), //x.inits.toString,
         (ae:(Action,FExp)) =>
           if ae._2==FExp.FTrue
           then ae._1.toString
           else s"${ae._1} if ${Show(ae._2)}")),
     "Products (feature combinations)" -> view[FRTS](x =>
                  val sel = x.main.toList.sorted.mkString(", ")
//                  "== FM to DNF ==\n" +
//                  Show.showDNF(x.fm.dnf) +
                  "== All features ==\n" +
                  x.feats.mkString(", ") +
                  "\n== Products ==\n" +
                  x.products
                    .toList.sortWith(_.size < _.size)
                    .zipWithIndex
                    .map((p,i)=>s" ${i+1}. ${p.toList.sorted.mkString(", ")}${
                      if p==x.main then " [selected]" else ""}")
                    .mkString("\n"), Text),
     "Possible problems of the RTS variant" -> view[FRTS](r=>AnalyseLTS.randomWalk(r.getRTS)._4 match
        case Nil => "No deadlocks, unreachable states/edges, nor inconsistencies"
        case m => m.mkString("\n")
       , Text), //.expand,
     "Number of states and edges"
       -> view((frts:FRTS) => {
       val rts = frts.getRTS
       val (st,eds,done) = SOS.traverse(RTSSemantics,rts,2000)
      //  val (stD, edsD, doneD) = SOS.traverse(caos.sos.FinAut.detSOS(RTSSemantics), Set(rts), 2000)
       val (iniMin,sosMin,doneMin1) = caos.sos.FinAut.minSOS(RTSSemantics, Set(rts), 2000)
       val (stMin, edsMin, doneMin2) = SOS.traverse(sosMin, iniMin, 2000)
       val doneMin = doneMin1 && doneMin2
       val rstates = rts.states.size
       val simpleEdges = (for (_,dests) <- rts.edgs yield dests.size).sum
       val reactions = (for (_,dests) <- rts.on yield dests.size).sum +
         (for (_,dests) <- rts.off yield dests.size).sum
       val frstates = frts.rts.states.size
       val fsimpleEdges = (for (_,dests) <- frts.rts.edgs yield dests.size).sum
       val freactions = (for (_,dests) <- frts.rts.on yield dests.size).sum +
         (for (_,dests) <- frts.rts.off yield dests.size).sum
       s"== FRTS (size: ${
         frstates + fsimpleEdges + freactions
       }) ==\nstates: ${
         frstates
       }\nsimple edges: ${
         fsimpleEdges
       }\nhyper edges: ${
         freactions
       }\n== RTS variant (size: ${
         rstates + simpleEdges + reactions
       }) ==\nstates: ${
         rstates
       }\nsimple edges: ${
         simpleEdges
       }\nhyper edges: ${
         reactions
       }\n== Flattened TS variant (size: ${
         if !done then ">2000" else st.size + eds
       }) ==\n" +
         (if !done then s"Stopped after traversing 2000 states"
         else s"States: ${st.size}\nEdges: $eds") +
         s"\n== Flattened TS variant as minimal DFA (size: ${
           if !doneMin then ">2000" else stMin.size + edsMin
         }) ==\n" +
         (if !doneMin then s"Stopped after traversing 2000 states"
         else s"States: ${stMin.size}\nEdges: $edsMin")
     },
       Text),
     html("<h2>Other functionalities</h2>"),
     "RTS variant: Step-by-step (simpler)" -> steps((e:FRTS)=>e.getRTS, RTSSemantics, RTS.toMermaidPlain, _.show, Mermaid),
     //     "Step-by-step DB" -> steps((e:FRTS)=>e, FRTSSemantics, FRTS.toMermaid, _.show, Text).expand,
     //     "Step-by-step DB (simpler)" -> steps((e:FRTS)=>e, FRTSSemantics, FRTS.toMermaidPlain, _.show, Text).expand,
     "RTS variant: Step-by-step (txt)" -> steps((e:FRTS)=>e.getRTS, RTSSemantics, Show.apply, _.show, Text),
     ////     "Step-by-step (debug)" -> steps((e:RxGraph)=>e, Program2.RxSemantics, RxGraph.toMermaid, _.show, Text),
     "TS variant: flattened (verbose)" -> lts((e:FRTS)=>e.getRTS,
       RTSSemantics,
       x => Show.simple(x),//x.inits.toString,
       _.toString),
     "TS variant: as mCRL2" ->
       view((e:FRTS)=>
           var seed = 0;
           var rtsid = Map[RTS,Int]()
           def fresh(rts:RTS): String = rtsid.get(rts) match
             case Some(value) => s"s$value"
             case None =>
               rtsid += rts -> seed
               seed += 1
               s"s${seed-1}"
           def clean(s:String) = s.replaceAll("/","_")
           val rts = e.getRTS
           val init = fresh(rts)
           val (nfa,done) = caos.sos.FinAut.sosToNFA(RTSSemantics,Set(rts))
           val emap = nfa.e.groupBy(_._1)
           val procs = for (src,edgs) <- emap yield
             s"  ${fresh(src)} = ${edgs.map(e =>
               val rest = if emap.contains(e._3) then s". ${fresh(e._3)}" else ""
               s"${clean(e._2.toString)} $rest"
             ).mkString(" + ")};"
           s"init $init;\n"+
             s"act\n  ${e.getRTS.edgs.flatMap(x=>x._2.map(y => clean(y._2.toString))).mkString(",")};\n" +
             s"proc\n${procs.toSet.mkString("\n")}"
         ,Text),
     "TS variant: flattened (DFA)" -> lts((e:FRTS)=>
       Set(e.getRTS), FinAut.detSOS(RTSSemantics),
       x => x.map(_.inits.toString).mkString(","),
       _.toString),
     "TS variant: flatenned (trace-equivalence minimal DFA)" -> ltsCustom(
       (e:FRTS)=>
         val (i,s,_) = FinAut.minSOS(RTSSemantics,Set(e.getRTS))
         (i,s, x => x.map(_.inits.toString).mkString(","), _.toString)),
     "TS variant: trace-equivalent states" -> view(e =>
       val p = FinAut.partitionNFA( FinAut.sosToNFA(RTSSemantics,Set(e.getRTS))._1)
       p.map(r => r.map(x => x.inits.toString).mkString(",")).mkString(" - ")
       , Text),
//     "1. NFA -> DFA (DFA)" -> lts((e:FRTS)=>Set(e.getRTS), FinAut.detSOS(RTSSemantics), x => x.map(_.inits.toString).mkString(","), _.toString),
//     "2. DFA -> revNFA)" -> lts2( //[FRTS,QName,Set[Set[RTS]]](
//       (e:FRTS)=>
//         val dfaLazy = FinAut.detSOS(RTSSemantics)
//         val dfa = FinAut.sosToNFA(dfaLazy,Set(Set(e.getRTS)))._1
//         val nfa = FinAut.revNFA(dfa)
//         val (sos,ini) = FinAut.nfaToSOS(nfa)
//         (ini,sos),
//       xx => xx.map(x => x.inits.toString).mkString(" - "), //x.map(_.map(_.inits).mkString(";")).mkString(","),
//       _.toString),
//     "3. revNDA -> revDFA" -> lts2( //[FRTS,QName,Set[Set[RTS]]](
//       (e:FRTS)=>
//         val dfaLazy = FinAut.detSOS(RTSSemantics)
//         val dfa = FinAut.sosToNFA(dfaLazy, Set(Set(e.getRTS)))._1
//         val rnfa = FinAut.revNFA(dfa)
//         val (rnfaSOS, ini) = FinAut.nfaToSOS(rnfa) // initial states of the revNFA
//         val rdfaSOS = FinAut.detSOS(rnfaSOS) // DFA - we do not know its initial states!
////         val rdfa = FinAut.sosToNFA(rdfaSOS)
//         (ini.map(Set(_)), rdfaSOS),
//       xxx => xxx.map(xx => xx.map( x => x.inits.toString).mkString(";")).mkString(","), //x.map(_.map(_.inits).mkString(";")).mkString(","),
//       _.toString),
//     "4. revDFA -> rev^2NFA -> rev^2DFA" -> lts2[FRTS,QName,Set[Set[RTS]]](
//       (e:FRTS)=>
//         val (sss,init,done) = caos.sos.FinAut.minSOS(RTSSemantics,e.getRTS)
//         (init,sss),
//       x => x.map(_.map(_.inits).mkString(";")).mkString(","),
//       _.toString),
//     "All steps (min-DFA)" -> lts2[FRTS,QName,Set[Set[RTS]]]((e:FRTS)=>Set(Set(e.getRTS)),
//         s => caos.sos.FinAut.minSOS(RTSSemantics,s.getRTS)._1,
//         x => x.map(_.map(_.inits).mkString(";")).mkString(","),
//         _.toString)
//     ,     ////     "All steps (Min DFA)" -> lts((e:RxGraph)=>Set(e), caos.sos.ToDFA.minLTS(RxSemantics), x => x.map(_.inits.mkString(",")).mkString("-"), _.toString),

//     "mCRL2 experiments"
//     -> view(MCRL2.apply, Text),

   )

  override val toggles: Map[String, Set[String]] = Map(
    "frtsBtn" -> widgets.map(ex => ex._1).toSet.filter(_.startsWith("FRTS")),
    "rtsBtn"  -> widgets.map(ex => ex._1).toSet.filter(_.startsWith("RTS")),
    "ftsBtn"  -> widgets.map(ex => ex._1).toSet.filter(_.startsWith("FTS")),
    "tsBtn"   -> widgets.map(ex => ex._1).toSet.filter(_.startsWith("TS")),
  )

  //// Documentation below

  override val footer: String =
    """Source code at: <a target="_blank"
      | href="https://github.com/fm-dcc/frets">
      | https://github.com/fm-dcc/frets</a>. This is a companion tool for
      | a paper submitted to VARS 2026, using <a target="_blank"
      | href="https://github.com/arcalab/CAOS">
      | CAOS</a> backend. Click the (?) on the headers of the widgets for more information.""".stripMargin

  private val sosRules: String =
    """ """.stripMargin

  override val documentation: Documentation = List(
    languageName -> "More information on the syntax of Reactive Graph" -> (
      "A Feature Reactive Transition System is a transition system where transitions can be " +
        "<ul><li>enabled or disabled at compile time based on feature expressions." +
        "<li>enabled or disabled at runtime based reactions.</li></ul>" +
        "The syntax for defining an FRTS is illustrated by the following example: " +
        "<pre>" +
        "init &lt;initial-state&gt;\n" +
        "// Add two lablelled transitions with a feature expression (optional)\n" +
        "&lt;source-state&gt;  --&gt; &lt;target-state&gt; by &lt;action&gt; if &lt;feature-expression&gt;\n" +
        "&lt;source-state&gt;  --&gt; &lt;target-state&gt; by &lt;action&gt; disabled // starts disabled\n" +
        "// (add more transitions)\n" +
        "\n// Enable an transition action2 when action1 is performed\n" +
        "&lt;action1&gt; -&gt; &lt;action2&gt;\n" +
        "\n// Disable an transition action2 when action1 is performed\n" +
        "&lt;action1&gt; --x &lt;action2&gt;\n" +
        "// Define a feature model as a constraint over feature names\n" +
        "fm &lt;feature-expression&gt;\n" +
        "// Select a set of features to be used in the analsyses\n" +
        "select &lt;feature-names*&gt;;" +
        "</pre>" +
        "<p> where <code>feature_expression</code> is a boolean expression over features, and " +
        "<code>feature-names*</code> is a comma-separated list of features chosen for the current product.</p>"),
    "TS: flattened" -> "More information on the TS visualization" ->
      """<p>This widget depicts the flattened variant for the selected product of the given FRTS.</p>
        |
        |<p>The names of the states include both the original name in the given FRTS and a number
        |indicating the number of active transitions. E.g., <code>s0[2]</code> represents
        |the state <code>s0</code> in the FRTS with 2 active transitions. Note that this name is not
        |unique. To see the list of all active transitions, which provides unique names,
        |please use the widget "TS: flattened (verbose)." </p>
        |""".stripMargin,
    "TS: flatenned (trace-equivalence minimal DFA)" -> "More information on how to mininmise the TS)"
      ->
      """We use Hopcroft's algorithm to find and merge indistinguishable states
        |(<a href="https://en.wikipedia.org/wiki/DFA_minimization#Hopcroft's_algorithm">https://en.wikipedia.org/wiki/DFA_minimization</a>),
        |based on partition refinement of the underlying equivalence class.
        |This notion of indistinguishable relies on trace-equivalence and not on bisimilarity.""".stripMargin,
    "TS: trace-equivalent states" -> "More information on how to mininmise the TS)"
      ->
      """We use Hopcroft's algorithm to find indistinguishable states
      |(<a href="https://en.wikipedia.org/wiki/DFA_minimization#Hopcroft's_algorithm">https://en.wikipedia.org/wiki/DFA_minimization</a>),
      |based on partition refinement of the underlying equivalence class.
      |This notion of indistinguishable relies on trace-equivalence and not on bisimilarity.""".stripMargin,
    "TS variant: as mCRL2" -> "More information on the mCRL2 syntax" ->
      """<p>This widget translates the RTS variant of the given FRTS into an equivalent
        |mCRL2 specification.</p>
        |
        |<p>For more information on the mCRL2 language, please visit
        |<a target="_blank" href="https://www.mcrl2.org/web/user_manual/language_reference/mcrl2.html">
        |https://www.mcrl2.org/web/user_manual/language_reference/mcrl2.html</a></p>
        |
        |<p> This translation is not modular, i.e., the RTS variant if flatenned into a single transition system
        |before being translated into mCRL2. We are investigating a modular approach, encoding the activation/deactivation
        |of transitions by parallel processes.</p>
        |
        |<p> To use this specification with the mCRL2 toolset, please start a new project in
        |mcrl2ide, copy-paste the output of this widget into the main specification file, and
        |then use the mCRL2 toolset to analyse it (e.g., generate the LTS, minimise it (using
        |trace equivalence, bisimilarity, or other equivanlences), check properties, etc).</p>
        |""".stripMargin,
  )
//      """|A program <code>RG</code> is a Reactive Graph with a syntax that follows the following template:
//         |<pre>
//         |init = Initial State;
//         |l0 = {
//         |    State from  --> State to by action,
//         |    };
//         |ln = {
//         |    (HE from, HE to, active, function),
//         |    }
//         |</pre>
//         |
//         |where:
//         |</p><code>init</code> is the initial state; </p>
//         |</p><code>l0</code> is a set of level 0 edges (E); use <code>--></code> to represent an enabled edge and <code>-.-></code> a disable edge; </p>
//         |</p><code>ln</code> is a set of hyper edges (GE); these can start and end in either E or another HE.
//         | An HE is defined recursively, i.e., both the "from" and the "to" fields can be another HE, or a simpler E in the base case;</p>
//         |</p><code>action</code> is a string that labels an E; it can have only letters in lower or upper case,  digits, and the symbols <code>_</code>, <code><</code>, <code>></code>, <code>.</code>, <code>-</code>, <code>€</code>, and <code>$</code>; </p>
//         |</p><code>funtion</code> is either <code>ON</code> or <code>OFF</code>; representing whether the HE enables or disables the target edge, respectively.</p>
//       """.stripMargin,
//    //"Build LTS" -> "More information on the operational rules used here" -> sosRules,
//    //"Build LTS (explore)" -> "More information on the operational rules used here" -> sosRules,
//    //"Run semantics" -> "More information on the operational rules used here" -> sosRules,
//    "Find strong bisimulation (given a program \"A ~ B\")" -> "More information on this widget" ->
//      ("<p>When the input consists of the comparison of 2 programs separated by <code>~</code>, this widget " +
//        "searches for a (strong) bisimulation between these 2 programs, providing either a " +
//        "concrete bisimulation or an explanation of where it failed.</p>" +
//        "<p>When only a program is provided, it compares it against the empty process <code>0</code>.</p>"),
//  )


