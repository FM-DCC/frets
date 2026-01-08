package marge.syntax

import caos.common.Multiset
import marge.backend
import marge.backend.Rel
import marge.backend.Rel.{Rel, add, join}
import marge.syntax.RTS
import marge.syntax.RTS.{Action, Edge, Edges, QName, State}

/** Extension of FRTS with on/off given by a label (id or action) instead of the actual edge */
case class XFRTS(f:FRTS,
                 xon: Rel[QName,QName],
                 xoff: Rel[QName,QName],
                 names: Map[QName,Edge]):

  def toFRTS: FRTS =
    val on2 = for (a,bs) <- xon.toSet; ea <- findName(a);
                  b <- bs; eb <- findName(b)
    yield ea -> eb
    val off2 = for (a,bs) <- xoff.toSet; ea <- findName(a);
                   b <- bs; eb <- findName(b)
    yield ea -> eb
    f.copy(rts = f.rts.copy(on = join(f.rts.on, on2), off = join(f.rts.off, off2)))

  def toRTS: RTS =
    toFRTS.getRTS

  private def findName(qn:QName): Set[Edge] =
      val e1: Option[Edge] = names.get(qn) // from names
      val e2 = f.rts.lbls.getOrElse(qn,Set()) // from action names
      if e1.isDefined && e2.nonEmpty then
        sys.error(s"Name $qn cannot be used both as an edge ID and as an action.")
      if e1.isEmpty && e2.isEmpty then
        sys.error(s"Name $qn not found, neither as an edge ID nor as an action.")
      e2 ++ e1.toSet

//  def toRTS: RTS =
//    val on2 = for (a,bs) <- xon.toSet; ea <- findName(a);
//                  b <- bs; eb <- findName(b)
//    yield ea -> eb
//    val off2 = for (a,bs) <- xoff.toSet; ea <- findName(a);
//                   b <- bs; eb <- findName(b)
//    yield ea -> eb
//    f.toRTS.copy(on = join(f.rts.on, on2), off = join(f.rts.off, off2))
//
  //    f.copy(rx = f.rx.copy(on = join(f.rx.on, on2), off = join(f.rx.off, off2)))

  def addEdge(nm: Option[String],s1:State,s2:State,l:Action) =
    val newNames = if nm.isEmpty then Map() else Map(QName(nm.toList) -> (s1,s2,l))
    val updF = XFRTS().copy(
      f = f.copy(rts = f.rts.copy(edgs = Map(s1->Set(s2->l)), act = f.rts.act + ((s1,s2,l)))),
      names = newNames)
    this ++ updF
  def addOn(nm: Option[String],s1:State,s2:State,l:Action) =
    noLbs("activating",nm,l)
    this.copy(xon = add(s1->s2,xon))
  def addOff(nm: Option[String],s1:State,s2:State,l:Action) =
    noLbs("deactivating",nm,l)
    this.copy(xoff = add(s1->s2,xoff))
  //    def addOff(es:(Edge,Edge)) =
  //      this.copy(f = f.copy(off  = add(es,f.off)))
  def deactivate(edge:Edge) = {
    if !f.rts.act.contains(edge) then sys.error(s"Could not deactivate edge \"${Show(edge)}\" - only edges between states can be deactivated.")
    this.copy(f = f.copy(rts = f.rts.copy(act = f.rts.act-edge)))
  }

  def addInit(s:State) =
    this.copy(f = f.copy(rts = f.rts.copy(inits = f.rts.inits +s))) // s::inits
  def addFM(newfm:FExp) =
    this.copy(f = f.addFM(newfm))
  def ++(frts:FRTS): XFRTS =
      this.copy(f = f ++ frts)
  def ++(r:RTS): XFRTS =
    this.copy(f = f ++ r)
  def ++(r: XFRTS): XFRTS =
    if this.names.keySet.intersect(r.names.keySet).nonEmpty then
      sys.error(s"IDs of edges should be unique, but the common ID(s) was(were) found: ${this.names.keySet.intersect(r.names.keySet)}")
    (this++(r.f)).copy(
      xon  = Rel.join(this.xon,r.xon),
      xoff = Rel.join(this.xoff,r.xoff),
      names = this.names ++ r.names)


  private def noLbs(from:String, nm: Option[String],l:Action): Unit =
    if nm.isDefined then sys.error(s"No label can be used when $from, but '${nm.get}' found.")
    if l.n.nonEmpty then sys.error(s"No action can be used when $from, but '$l' found.")

object XFRTS:
  /** Initialises a XFRTS with default values */
  def apply(): XFRTS =
    val rts = RTS(
      Map().withDefaultValue(Set()),Map().withDefaultValue(Set()),
      Map().withDefaultValue(Set()),Multiset(),Set())
    val frts = FRTS(rts,FExp.FTrue,Map())
    XFRTS(frts, backend.Rel.empty, Rel.empty, Map())


