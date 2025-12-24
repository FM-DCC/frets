package marge.syntax

import caos.common.Multiset
import marge.backend.Rel
import Rel.*


object FRTS:

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
    def /(rx: XFRTS): XFRTS =
      XFRTS(this / rx.f,
        rx.xon.map( kv => this/kv._1 -> kv._2.map(this / _)),
        rx.xoff.map(kv => this/kv._1 -> kv._2.map(this / _)),
        rx.names.map(kv => this/kv._1 -> this / kv._2))
    def /(rx: FRTS): FRTS =
        FRTS(this/rx.edgs, this add rx.on, this add rx.off,
          this/-rx.inits, this/rx.act)


  type State = QName
  type Action = QName
  type Reaction = Rel[Edge,Edge] // maps edges to their act/deactated edges
  type Edge = (State, State, Action) //from,to,by
  type Edges = Set[Edge]
  type EdgeMap = Rel[State,(State,Action)] // optimised structure for set of edges

  /**
   * Featured Reactive Transition System
   * Each edge is indentified by a triple (from,to,lbl).
   * Labels activate/deactivate other labels
   * From, to, lbl, aut - are all (qualified) names
   * @param edgs - edges s1 -> s2: lbl
   * @param on - activations lbl1 ->> lbl2: lbl3
   * @param off - deactivations lbl1 --x lbl2: lbl3
   * @param act - active edges {e1,e2,...}
   */
  case class FRTS(edgs:EdgeMap,
                  on:Reaction, off: Reaction,
                  inits: Multiset[State],
                  act: Edges):
    //override def toString: String = ??? //Show(this)

    /** Collect all states */
    lazy val states =
      for (src,dests)<-edgs.toSet; (d,_)<-dests; st <- Set(src,d) yield st
    /** Collect all actions and their corresponding edges */
    lazy val lbls: Rel[Action,Edge] =
      (for (src,dests)<-edgs.toSet; (d,l)<-dests
        yield (src,d,l))
        .groupBy(_._3)
    lazy val lblsRev: Rel[(State,State),Edge] =
      (for (src,dests)<-edgs.toSet; (d,l)<-dests
        yield (src,d,l))
        .groupBy(e=>e._1->e._2)
    lazy val rxEdges: Edges =
      for (e1,upd)<-(on.toSet++off.toSet);  e2<-upd; s<-Set(e1,e2) yield s


  /** Extension of FRTS with on/off given by a label (id or action) instead of the actual edge */
  case class XFRTS(f:FRTS,
                   xon: Rel[QName,QName],
                   xoff: Rel[QName,QName],
                   names: Map[QName,Edge]):
    def toFRTS: FRTS =
      def findName(qn:QName): Set[Edge] =
        println(s"finding name for ${qn}")
        val e1: Option[Edge] = names.get(qn) // from names
        val e2 = f.lbls.getOrElse(qn,Set()) // from action names
        if e1.isDefined && e2.nonEmpty then
          sys.error(s"Name $qn cannot be used both as an edge ID and as an action.")
        if e1.isEmpty && e2.isEmpty then
          sys.error(s"Name $qn not found, neither as an edge ID nor as an action.")
        e2 ++ e1.toSet
      val on2 = for (a,bs) <- xon.toSet; ea <- findName(a);
                    b <- bs; eb <- findName(b)
                yield ea -> eb
      val off2 = for (a,bs) <- xoff.toSet; ea <- findName(a);
                     b <- bs; eb <- findName(b)
                 yield ea -> eb
      println(s"## joining ${f.off} with ${off2} to get ${join(f.off, off2)}")
      f.copy(on = join(f.on, on2), off = join(f.off, off2))

    def addEdge(nm: Option[String],s1:State,s2:State,l:Action) =
      val newNames = if nm.isEmpty then Map() else Map(QName(nm.toList) -> (s1,s2,l))
      val updF = XFRTS().copy(f = f.copy(edgs = Map(s1->Set(s2->l)), act = f.act + ((s1,s2,l))), names = newNames)
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
      if !f.act.contains(edge) then sys.error(s"Could not deactivate edge \"${Show(edge)}\" - only edges between states can be deactivated.")
      this.copy(f = f.copy(act = f.act-edge))
    }

    def addInit(s:State) =
      this.copy(f = f.copy(inits = f.inits +s)) // s::inits
    def ++(r:FRTS): XFRTS =
      this.copy(f = FRTS(join(f.edgs,r.edgs),join(f.on,r.on),join(f.off,r.off),f.inits++r.inits,f.act++r.act))
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
    def apply(): XFRTS = XFRTS(FRTS(
      Map().withDefaultValue(Set()),Map().withDefaultValue(Set()),
      Map().withDefaultValue(Set()),Multiset(),Set()),
      Rel.empty, Rel.empty, Map())

    /** Generates a mermaid graph with all edges */
    def toMermaid(rx: FRTS): String = ???


  object FRTS:
    def toMermaid(rx: FRTS): String =
      var i = -1
      def fresh(): Int = {i += 1; i}
      s"flowchart LR\n${
        drawEdges(rx.edgs, rx, fresh, ">", "stroke:black, stroke-width:2px")}${
        drawReaction(rx.on, rx, fresh, ">", "stroke:blue, stroke-width:3px")}${
        drawReaction(rx.off, rx, fresh, "x", "stroke:red, stroke-width:3px")}${
//        drawEdges(rx.on, rx, fresh, ">", "stroke:blue, stroke-width:3px",getLabel)}${
//        drawEdges(rx.off,rx, fresh, "x", "stroke:red, stroke-width:3px",getLabel)}${
        (for s<-rx.inits.data.keySet yield s"  style $s fill:#8f7,stroke:#363,stroke-width:4px\n").mkString
      }"

    /** Generates a mermaid graph with only the ground edges */
    def toMermaidPlain(rx: FRTS): String =
      var i = -1
      def fresh(): Int = {i += 1; i}
      s"flowchart LR\n${
        drawEdges(rx.edgs, rx, fresh, ">", "stroke:black, stroke-width:2px",simple=true)}${
        (for s<-rx.inits.data.keySet yield s"  style $s fill:#8f7,stroke:#363,stroke-width:4px\n").mkString
      }"
    private def getLabel(n:QName, rx:FRTS): Set[String] =
      for (a,b,c) <- rx.lbls.getOrElse(n,Set()) yield s"$a$b$c"

    private def drawEdgesOld(es:EdgeMap,rx:FRTS,fresh:()=>Int,tip:String,
                          style:String,getEnds:(QName,FRTS)=>Set[String],simple:Boolean=false): String =
      (for (a,bs)<-es.toList; (b,c) <- bs.toList; a2<-getEnds(a,rx).toList; b2<-getEnds(b,rx).toList  yield
        val line = if rx.act((a,b,c)) then "---" else "-.-"
        if c.n.isEmpty
        then s"  $a2 $line$tip $b2\n"+
             s"  linkStyle ${fresh()} $style\n"
        else if simple
        then { ""
//          def getEName(from:QName,to:(QName,QName)) = (from,to._1,to._2) match
//            case (_,_,QName(Nil)) => ""
//            case (_,_,QName(List(""))) => ""
//            case e if rx.act(e) => s"[${to._2}]" // active
//            case _ => s"(${to._2})" // not active
//          val add = rx.on(c)
//          val addStr = if add.isEmpty then ""
//                       else " | +" + add.map(x=>s"${x._1}${getEName(c,x)}").mkString(",")
//          val drop = rx.off(c)
//          val dropStr = if drop.isEmpty then ""
//                        else " | -" + drop.map(x=>s"${x._1}${getEName(c,x)}").mkString(",")
//          val debug = "" //s"${rx.act.toString}"
//          s"  $a2 $line$tip |\"$c$addStr$dropStr$debug\"| $b2\n"+
//          s"  linkStyle ${fresh()} $style\n"
        } else s"  $a2 $line $a$b$c( ) $line$tip |$c| $b2\n" + // from --- fromtoact() ---> |act| to
             s"  style $a$b$c width: 0\n"+
             s"  linkStyle ${fresh()} $style\n"+
             s"  linkStyle ${fresh()} $style\n"
      ).mkString

    private def drawEdges(es:EdgeMap,rx:FRTS,fresh:()=>Int,tip:String,
                          style:String,simple:Boolean=false): String =
      (for (a,bs)<-es.toList; (b,c) <- bs.toList  yield
        val isRx = rx.rxEdges.contains((a,b,c))
        val line = if rx.act.contains((a,b,c)) then "---" else "-.-"
        val lbl = if c.n.isEmpty then "" else s"|$c|"
        if simple || !isRx then // no middle point needed
          s"  $a $line$tip $lbl $b\n"+
          s"  linkStyle ${fresh()} $style\n"
        else // middle point needed
          s"  $a $line $a$b$c( ) $line$tip $lbl $b\n" + // from --- fromtoact() ---> |act| to
          s"  style $a$b$c width: 0\n"+
          s"  linkStyle ${fresh()} $style\n"+
          s"  linkStyle ${fresh()} $style\n"
      ).mkString

    private def drawReaction(r:Reaction,rx:FRTS,fresh:()=>Int,tip:String,
                             style:String,simple:Boolean=false): String = {
      def edgNm(e:Edge) = s"${e._1}${e._2}${e._3}"
      (for (e1,es)<-r; e2 <- es.toList yield
        s"  ${edgNm(e1)} ---$tip ${edgNm(e2)}\n"+
        s"  linkStyle ${fresh()} $style\n"
      ).mkString
    }
