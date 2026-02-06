package marge.backend

import marge.syntax.RTS
import marge.syntax.RTS.{Edges, QName,Edge,Action,State}
import marge.syntax.Show
import caos.sos.SOS

object AnalyseLTS:

  /** Find possible problems in the RTS projection, such as deadlocks, unreachable states/transitions, and inconsistencies. */
  def randomWalkPP(rx:RTS, max:Int=5000): String =
    val (visited, nEdges, fired, probs) = randomWalk(rx, max)
    s"Traversed $nEdges edges and visited ${visited.size} states. " +
      problemsPP(probs, max.toString)

  /** Pretty-print the problems found during analysis. */
  def problemsPP(probs: Problems, max:String=""): String =
    if probs.isEmpty then
      s"No problems found."
    else
      s"Problems found:" +
      (if probs.tooBig then s"\n- Reached limit of transitions $max" else "") +
      (if probs.unreachableStates.nonEmpty then s"\n- Unreachable state(s): ${probs.unreachableStates.mkString(", ")}" else "") +
      (if probs.unreachableEdges.nonEmpty then s"\n- Unreachable edge(s): ${Show(probs.unreachableEdges)}" else "") +
      (if probs.deadlocks.nonEmpty then s"\n- Deadlock(s): ${probs.deadlocks.map(Show.simple).mkString(", ")}" else "") +
      (if probs.inconsistencies.nonEmpty then s"\n- Inconsistencies: ${probs.inconsistencies.map(ss => s"${Show(ss._1)} => ${Show(ss._2)}").mkString("; ")}" else "") +
      (if probs.multipleInits.nonEmpty then s"\n- Multiple initial states reached the same state: ${probs.multipleInits.mkString(", ")}" else "")

  /** data structure to hold problems found during analysis */
  case class Problems(
    tooBig: Boolean = false,
    unreachableStates: Set[State] = Set(),
    unreachableEdges: Edges = Set(),
    deadlocks: Set[RTS] = Set(),
    inconsistencies: Set[(Edge,Set[Edge])] = Set(),
    multipleInits: Set[State] = Set()
  ) :
    def isEmpty: Boolean =
      !tooBig && unreachableStates.isEmpty && unreachableEdges.isEmpty &&
        deadlocks.isEmpty && inconsistencies.isEmpty && multipleInits.isEmpty

  /**
    * Performs a random walk on the RTS, returning the set of visited states, number of edges traversed, set of fired edges, and any problems found (deadlocks, unreachable states/transitions, inconsistencies, etc).
    *
    * @param rx the RTS to analyze
    * @param max maximum number of transitions to traverse
    * @return a tuple containing the set of visited states, number of edges traversed, set of fired edges, and detected problems
    */
  def randomWalk(rx:RTS, max:Int=5000): (Set[RTS],Int,Edges,Problems) =
    val states = for (a,bs)<-rx.edgs.toSet; (b,_)<-bs; s<-Set(a,b) yield s
    def aux(next:Set[RTS], done:Set[RTS],
            nEdges:Int, fired:Edges, probs:Problems,
            limit:Int): (Set[RTS],Int,Edges,Problems) =
      if limit <=0 then
        // error 1: too big
        return (done,nEdges,fired, probs.copy(tooBig = true))
      next.headOption match
        case None =>
          val missingStates: Set[State] =
            (for (a,dest)<-rx.edgs.toSet; (b,_)<-dest; s <- Set(a,b) yield s)
              -- done.flatMap(_.inits.data.keySet)
          val missingEdges: Edges =
            (for (a,dests)<-rx.edgs.toSet; (b,c)<-dests yield (a,b,c))
              -- fired
          if missingStates.isEmpty && missingEdges.isEmpty then
            (done, nEdges, fired, probs) // success
          else // error 2: unreachable states/edges
            (done, nEdges, fired,
              probs.copy(
                unreachableStates = probs.unreachableStates ++ missingStates,
                unreachableEdges = probs.unreachableEdges ++ missingEdges
              )
            )
        case Some(st) if done contains st =>
          aux(next-st,done,nEdges,fired,probs,limit)
        case Some(st) => //visiting new state
          val more = RTSSemantics.nextEdge(st)
          val nEdges2 = more.size
          val newEdges = more.map(_._1)
          var incons = Set[(Edge,Set[Edge])]()
          for e<-newEdges do
            val toAct   = Rel.get(e,st.on)
            val toDeact = Rel.get(e,st.off)
            val shared = toAct.intersect(toDeact)
            if shared.nonEmpty then
              incons = incons + ((e,shared))
          var manyInits = Set[State]()
          val inits = for (i,n) <- st.inits.data if n > 1 yield i            
          manyInits = manyInits ++ inits
          // update probs
          var newProbs = probs
          if more.isEmpty then  newProbs = newProbs.copy(deadlocks = newProbs.deadlocks + st)
          if incons.nonEmpty then newProbs = newProbs.copy(inconsistencies = newProbs.inconsistencies ++ incons)
          if manyInits.nonEmpty then newProbs = newProbs.copy(multipleInits = newProbs.multipleInits ++ manyInits)
          aux((next-st)++more.map(_._2), done+st, nEdges+nEdges2,fired++newEdges,newProbs,limit-nEdges2)

    aux(Set(rx), Set(), 0, Set(), Problems(), max)


  def sanify(rx:RTS): RTS = 
    val (visited, nEdges, fired, probs) = randomWalk(rx)
    val unreachStates = probs.unreachableStates
    val unreachEdges = probs.unreachableEdges
    // remove unreachable states and edges
    rx.copy(
      edgs = rx.edgs.filter(kv => !unreachStates.contains(kv._1))
        .map(kv => (kv._1, kv._2.filter( (dst,act) => !unreachStates.contains(dst) && !unreachEdges.contains((kv._1, dst, act)) ))),
      inits = rx.inits.filter( st => !unreachStates.contains(st) ),
      act = rx.act.filter( e => !unreachStates.contains(e._1) && !unreachStates.contains(e._2) ),
      on = rx.on
            .map(es => es._1 -> es._2.filter(e => !unreachEdges.contains(e)))
            .filter( e => !unreachEdges.contains(e._1) && e._2.nonEmpty ),
      off = rx.off //.filter( e => !unreachStates.contains(e._1._1) && !unreachStates.contains(e._1._2) )
            .map(es => es._1 -> es._2.filter(e => !unreachEdges.contains(e)))
            .filter( e => !unreachEdges.contains(e._1) && e._2.nonEmpty )
    )