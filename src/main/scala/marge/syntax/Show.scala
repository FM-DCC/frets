package marge.syntax

//import marge.syntax.Syntax.{Edge, EdgeMap, Edges, RxGraph}
import marge.syntax
import FExp.*
import marge.syntax.RTS
import marge.syntax.RTS.{Edge, EdgeMap, Edges, Reaction}
import marge.syntax.XFRTS

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

  /** Pretty print an FRTS extended with aliases */
  def apply(rx: XFRTS): String =
    s"${apply(rx.f)}\n[xon] ${rx.xon}\n[xoff] ${rx.xoff}\n[aliases] ${rx.names.mkString(",")}"

  /** Pretty print an FRTS */
  def apply(f: FRTS): String =
    s"${apply(f.rts)}\n[FM] ${apply(f.fm)}\n[FCond] ${
      f.pk.map(kv => apply(kv._1) + " -> " + apply(kv._2)).mkString("; ")}\n[Sel] ${
      f.main.mkString("{",",","}")}"
  
  /** Pretty print a feature expression */
  def apply(fe: FExp): String = fe match
    case FTrue => "true"
    case Feat(n) => n
    case FAnd(e1, e2) => s"${applyP(e1)} /\\ ${applyP(e2)}"
    case FOr(e1, e2) => s"${applyP(e1)} \\/ ${applyP(e2)}"
    case FNot(e) => s"¬${applyP(e)}"
    case FImp(e1, e2) => s"${applyP(e1)} -> ${applyP(e2)}"
    case FEq(e1, e2) => s"${applyP(e1)} <-> ${applyP(e2)}"
  private def applyP(fe: FExp): String = fe match
    case e: (FOr | FAnd | FImp | FEq) => s"(${apply(e)})"
    case _ => apply(fe)

  /** Pretty print an DNF formula */
  def showDNF(dnf: Set[Set[Literal]]): String =
    if dnf.isEmpty then "False"
    else if dnf == Set(Set()) then "True"
    else dnf
      .map(con => con.map(l => l.fold(x=>s"¬$x",x=>x))
      .mkString(" & "))
      .mkString("\n")

  /** Pretty print a guarded action */
  def apply(guarded: (Any, FExp)): String =
    if guarded._2==FExp.FTrue then
      guarded._1.toString else
      s"${guarded._1} if ${Show(guarded._2).replaceAll("¬","!")}"

  /** Pretty print an RTS */
  def apply(rx: RTS): String =
    s"[init]  ${rx.inits}\n[act]   ${apply(rx.act)}\n[edges] ${
      showEdges(rx.edgs)
    }\n[on]    ${showReaction(rx.on)}\n[off]   ${showReaction(rx.off)}"

  /** Pretty print an RTS with only the number of active edges */
  def simple(rx:RTS): String =
    if rx.off.isEmpty && rx.on.isEmpty then
      s"${rx.inits}"
    else
      s"[at] ${rx.inits} [active] ${apply(rx.act)}"

  /** Pretty print an RTS with only the number of active edges (more compact) */
  def simpler(rx: RTS): String =
    if rx.off.isEmpty && rx.on.isEmpty then
      s"${rx.inits}"
    else
      s"${rx.inits}[${rx.act.size}]"
