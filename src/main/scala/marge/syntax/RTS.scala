package marge.syntax

import caos.common.Multiset
import marge.backend.Rel
import Rel.*
import marge.syntax.RTS.{EdgeMap, Edge, Edges, Reaction, State, Action}


/**
 * Featured Reactive Transition System
 * Each edge is indentified by a triple (from,to,lbl).
 * Labels activate/deactivate other labels
 * From, to, lbl, aut - are all (qualified) names
 *
 * @param edgs - edges s1 -> s2: lbl
 * @param on   - activations lbl1 ->> lbl2: lbl3
 * @param off  - deactivations lbl1 --x lbl2: lbl3
 * @param act  - active edges {e1,e2,...}
 */
case class RTS(edgs: EdgeMap,
               on: Reaction, off: Reaction,
               inits: Multiset[State],
               act: Edges):
  override def toString: String = Show.simpler(this)

  /** Collect all states */
  lazy val states =
    for (src, dests) <- edgs.toSet; (d, _) <- dests; st <- Set(src, d) yield st
  /** Collect all actions and their corresponding edges */
  lazy val lbls: Rel[Action, Edge] =
    (for (src, dests) <- edgs.toSet; (d, l) <- dests
      yield (src, d, l))
      .groupBy(_._3)
  lazy val lblsRev: Rel[(State, State), Edge] =
    (for (src, dests) <- edgs.toSet; (d, l) <- dests
      yield (src, d, l))
      .groupBy(e => e._1 -> e._2)
  lazy val rxEdges: Edges =
    for (e1, upd) <- (on.toSet ++ off.toSet); e2 <- upd; s <- Set(e1, e2) yield s

  def ++(rts:RTS): RTS =
    RTS(join(rts.edgs, edgs),
      join(rts.on, on),
      join(rts.off, off),
      rts.inits ++ inits,
      rts.act ++ act)

object RTS:

  type State = QName
  type Action = QName
  type Reaction = Rel[Edge,Edge] // maps edges to their act/deactated edges
  type Edge = (State, State, Action) //from,to,by
  type Edges = Set[Edge]
  type EdgeMap = Rel[State,(State,Action)] // optimised structure for set of edges

  case class QName(n:List[String]):
    override def toString = n.mkString("/")
    def show = if n.isEmpty then "-" else toString
    def /(other:QName) = if other.n.isEmpty then other else QName(n:::other.n)
    def /(other:String) = QName(n:::List(other))
    def /(e:Edge): Edge = (this/e._1,this/e._2,this/e._3)
    def /(e:EdgeMap):EdgeMap =
      e.map(kv=>(this/(kv._1) -> kv._2.map((x,y)=>(this/x,this/y))))
    def /(es:Edges): Edges =
      es.map((x,y,z)=>(this/x,this/y,this/z))
    def add(rx:Reaction): Reaction =
      rx.map(kv=>(this/(kv._1) -> kv._2.map(this/_)))
    def /-(ns:Multiset[QName]): Multiset[QName] =
      Multiset(ns.data.map(kv => (this/(kv._1) -> kv._2)))
    //    def /-(lbls:Map[QName,Edges]): Map[QName,Edges] =
    //      lbls.map(kv=>(this/(kv._1) -> this/kv._2))
    //    def /-(ns:Set[QName]): Set[QName] =
    //      ns.map(n => this/n)
//    def /-(ns:List[QName]): List[QName] =
//      ns.map(n => this/n)
    def /(rx: FRTS): FRTS =
      rx.copy(rts = this/rx.rts,
              equivs = rx.equivs.map( (s1,s2,b) => (this/s1, this/s2,b)))
    def /(rx: XFRTS): XFRTS =
      XFRTS(this / rx.f,
        rx.xon.map( kv => this/kv._1 -> kv._2.map(this / _)),
        rx.xoff.map(kv => this/kv._1 -> kv._2.map(this / _)),
        rx.names.map(kv => this/kv._1 -> this / kv._2))
    def /(rx: RTS): RTS =
        RTS(this/rx.edgs, this add rx.on, this add rx.off,
          this/-rx.inits, this/rx.act)
