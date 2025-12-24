package marge.syntax

//import marge.syntax.Syntax.{Edge, EdgeMap, Edges, RxGraph}
import marge.syntax.FRTS.{Edge, EdgeMap, Edges, FRTS, Reaction, XFRTS}

object Show:

  def apply(e: Edge): String =
    s"${e._1}>${e._2}${if e._3.n.nonEmpty then s":${e._3}" else ""}"

  def apply(abc: Edges): String =
    abc.map(apply).mkString(", ")

  private def showEdges(abc: EdgeMap): String =
    apply(for (a, bcs) <- abc.toSet; (b, c) <- bcs yield (a, b, c))
  private def showReaction(abc: Reaction): String =
    (for (a, bcs) <- abc.toSet; b <- bcs yield s"${apply(a)}|${apply(b)}")
      .mkString(",")


//  def apply(rx: RxGraph): String =
//    s"[init]  ${rx.inits.mkString(",")}\n[act]   ${apply(rx.act)}\n[edges] ${
//      showEdges(rx.edg)
//    }\n[on]    ${showEdges(rx.on)}\n[off]   ${showEdges(rx.off)}"
//
//  def simple(rx:RxGraph): String =
//    s"[at] ${rx.inits.mkString(",")} [active] ${apply(rx.act)}"

  def apply(rx: XFRTS): String =
    s"${apply(rx.f)}\n[xon] ${rx.xon}\n[xoff] ${rx.xoff}\n[aliases] ${rx.names.mkString(",")}"


  def apply(rx: FRTS): String =
    s"[init]  ${rx.inits}\n[act]   ${apply(rx.act)}\n[edges] ${
      showEdges(rx.edgs)
    }\n[on]    ${showReaction(rx.on)}\n[off]   ${showReaction(rx.off)}"

  def simple(rx:FRTS): String =
    s"[at] ${rx.inits} [active] ${apply(rx.act)}"
