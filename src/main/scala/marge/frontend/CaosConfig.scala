package marge.frontend

import caos.frontend.Configurator.*
import caos.frontend.{Configurator, Documentation}
import caos.sos.SOS
import caos.view.*
import marge.backend.*
import marge.syntax.{Parser, Show, Syntax}
//import marge.syntax.Syntax.RxGraph
import marge.syntax.RTS
import marge.backend.RxSemantics

/** Object used to configure which analysis appear in the browser */
object CaosConfig extends Configurator[RTS]:
  val name = "Animator of Labelled Reactive Graphs"
  override val languageName: String = "Input Reactive Graphs"

  val parser = marge.syntax.Parser.parseProgram

  /** Examples of programs that the user can choose from. The first is the default one. */
  val examples = List(
    "Simple" -> "init s0\ns0 --> s1: a\ns1 --> s0: b\na  --! a"
      -> "Basic example",
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
    "Dependencies" -> "aut A {\n  init 0\n  0 --> 1: look\n  1 --> 0: restart\n}\n\naut B {\n  init 0\n  0 --> 1: on\n  1 --> 2: goLeft disabled\n  1 --> 2: goRight disabled\n  goLeft --#-- goRight\n  2 --> 0: off\n}\n\n// dependencies\nA.look ----> B.goLeft\nA.look ----> B.goRight"
      -> "Experimental syntax to describe dependencies, currently only as syntactic sugar.",
    "Dynamic SPL" -> "init setup\nsetup --> setup : Safe\nsetup --> setup : Unsafe\nsetup --> setup : Encrypt\nsetup --> setup : Dencrypt\nsetup --> ready\nready --> setup\nready --> received : Receive\nreceived --> routed_safe : ERoute  disabled\nreceived --> routed_unsafe : Route\nrouted_safe --> sent : ESend       disabled\nrouted_unsafe --> sent : Send\nrouted_unsafe --> sent_encrypt : ESend disabled\nsent_encrypt --> ready : Ready\nsent --> ready : Ready\n\nSafe ->> ERoute\nSafe --! Route\nUnsafe --! ERoute\nUnsafe ->> Route\nEncrypt --! Send\nEncrypt ->> ESend\nDencrypt ->> Send\nDencrypt --! ESend"
      -> "Example of a Dynamic Software Product Line, borrowed from Fig 1 in Maxime Cordy et al. <em>Model Checking Adaptive Software with Featured Transition Systems</em>",
    "NFA-DFA 1" -> "init 0\n0 --> 1: 0\n1 --> 0: 0\n1 --> 3: 1\n2 --> 1: 0\n2 --> 3: 1\n4 --> 3: 0\n4 --> 3: 1\n0 --> 3: 1\n3 --> 5: 0\n3 --> 5: 1\n5 --> 5: 0\n5 --> 5: 1"
      -> "Experimenting with determinisatoin and minimisation of automata",
    "NFA-DFA 2" -> "init q0\nq0 --> q0: a\nq0 --> q0: b\nq0 --> q1: a\nq1 --> q2: b"
      -> "Simple example of an NFA that could be determinised",
    "Parallel" -> "aut a {\n  init 0\n  0 --> 1 : a disabled\n}\naut b {\n  init 0\n  0 --> 1 : b0\n  1 --> 0 : b disabled\n}\naut c {\n  init 0\n  0 --> 1 : c0\n  1 --> 0 : c disabled\n}\n// intrusion\nb.b  ->> a.a\nc.c  ->> a.a\nb.b0 ->> c.c\nc.c0 ->> b.b\nb.b0 --#-- c.c0"
      -> "Experiments with multiple components.",
    "Vending" -> "init s1\ns1 --> s1: sodaRefill\ns1 --> s1: teaRefill\ns1 --> s2: pay\ns4 --> s1: return\ns2 --> s3: change\ns3 --> s4: cancel\ns3 --> s5: soda\ns3 --> s6: tea\ns5 --> s7: serve\ns5 --> s7: serveSodaGone\ns6 --> s7: serve\ns6 --> s7: serveTeaGone\ns7 --> s8: open\ns8 --> s9: take\ns9 --> s1: close\n\nsodaRefill ->> soda\nteaRefill ->> tea\nserveSodaGone --x soda\nserveTeaGone --x tea"
      -> "Experiment from the ongoing paper"
  )

   /** Description of the widgets that appear in the dashboard. */
   val widgets = List(
//     "View State" -> view[FRTS](_.toString, Text).expand,
//     "View State" -> view[FRTS](Show.apply, Text).expand,
     // "View debug (simpler)" -> view[RxGraph](RxGraph.toMermaidPlain, Text).expand,
     // "View debug (complx)" -> view[RxGraph](RxGraph.toMermaid, Text).expand,
     "Step-by-step" -> steps((e:RTS)=>e, RTSSemantics, RTS.toMermaid, _.show, Mermaid).expand,
     "Step-by-step (simpler)" -> steps((e:RTS)=>e, RTSSemantics, RTS.toMermaidPlain, _.show, Mermaid),
//     "Step-by-step DB" -> steps((e:FRTS)=>e, FRTSSemantics, FRTS.toMermaid, _.show, Text).expand,
//     "Step-by-step DB (simpler)" -> steps((e:FRTS)=>e, FRTSSemantics, FRTS.toMermaidPlain, _.show, Text).expand,
     "Step-by-step (txt)" -> steps((e:RTS)=>e, RTSSemantics, Show.apply, _.show, Text),
////     "Step-by-step (debug)" -> steps((e:RxGraph)=>e, Program2.RxSemantics, RxGraph.toMermaid, _.show, Text),
     "All steps" -> lts((e:RTS)=>e, RTSSemantics, x => x.inits.toString, _.toString),
     "All steps (DFA)" -> lts((e:RTS)=>Set(e), caos.sos.ToDFA(RTSSemantics), x => x.map(_.inits.toString).mkString(","), _.toString),
////     "All steps (Min DFA)" -> lts((e:RxGraph)=>Set(e), caos.sos.ToDFA.minLTS(RxSemantics), x => x.map(_.inits.mkString(",")).mkString("-"), _.toString),
     "Possible problems" -> view[RTS](r=>AnalyseLTS.randomWalk(r)._4 match
        case Nil => "No deadlocks, unreachable states/edges, nor inconsistencies"
        case m => m.mkString("\n")
       , Text).expand,
     "Number of states and edges"
      -> view((frts:RTS) => {
          val (st,eds,done) = SOS.traverse(RTSSemantics,frts,2000)
          val (stD, edsD, doneD) = SOS.traverse(caos.sos.ToDFA(RTSSemantics), Set(frts), 2000)
          val rstates = frts.states.size
          val simpleEdges = (for (_,dests) <- frts.edgs yield dests.size).sum
          val reactions = (for (_,dests) <- frts.on yield dests.size).sum +
                          (for (_,dests) <- frts.off yield dests.size).sum
          s"== Reactive Graph (size: ${
            rstates + simpleEdges + reactions
          }) ==\nstates: ${
            rstates
          }\nsimple edges: ${
           simpleEdges
          }\nhyper edges: ${
            reactions
          }\n== Encoded LTS (size: ${
            if !done then ">2000" else st.size + eds
          }) ==\n" +
          (if !done then s"Stopped after traversing 2000 states"
           else s"States: ${st.size}\nEdges: $eds") +
          s"\n== Encoded DFA (size: ${
           if !done then ">2000" else stD.size + edsD
          }) ==\n" +
            (if !doneD then s"Stopped after traversing 2000 states"
            else s"States: ${stD.size}\nEdges: $edsD")
        },
        Text),
//     "mCRL2 experiments"
//     -> view(MCRL2.apply, Text),

   )

  //// Documentation below

  override val footer: String =
    """Source code at: <a target="_blank"
      | href="https://github.com/fm-dcc/marge">
      | https://github.com/fm-dcc/marge</a>. This is a companion tool for
      | a paper accepted at FACS 2024, based on <a target="_blank"
      | href="https://github.com/arcalab/CAOS">
      | CAOS</a>. The original version used for FACS can be found at <a target="_blank"
      | href="https://fm-dcc.github.io/MARGe/marge-0.1.html">
      | https://fm-dcc.github.io/MARGe/marge-0.1.html</a>.""".stripMargin
  // Simple animator of Labelled Reactive Graphs, meant to exemplify the
  // | CAOS libraries, used to generate this website.""".stripMargin
  // Source code available online:
  // | <a target="_blank" href="https://github.com/arcalab/CAOS">
  // | https://github.com/arcalab/CAOS</a> (CAOS).""".stripMargin

  private val sosRules: String =
    """ """.stripMargin

//  override val documentation: Documentation = List(
//    languageName -> "More information on the syntax of Reactive Graph" ->
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


