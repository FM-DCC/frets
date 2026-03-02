package marge.backend

import caos.sos.SOS
import marge.syntax.FExp.FTrue
import marge.syntax.{FExp, RTS}
import marge.syntax.RTS.{Action, Edge, Edges, QName, State}

import scala.annotation.tailrec

object RTSSemantics extends SOS[Action,RTS]:

  override def accepting(s: RTS): Boolean = true // all states are accepting
  /** Calulates the next possible init states */
  def next[Name >: Action](rx: RTS): Set[(Name, RTS)] =
    for (st,i) <- rx.inits.data.toSet
        _ <- 1 to i
        (st2, lbl) <- Rel.get(st,rx.edgs) if rx.act((st, st2, lbl))
    yield
      val toAct = Rel.get((st,st2,lbl),rx.on)
      val toDeact = Rel.get((st,st2,lbl),rx.off)
      val newAct = (rx.act ++ toAct) -- toDeact // biased to deactivation
      val newInits = (rx.inits - st) + st2
      lbl -> rx.copy(inits = newInits, act = newAct)

  /** Similar to `next`, but include the full transition instead of only the action name */
  def nextEdge(rx: RTS): Set[(Edge, RTS)] =
    for (st,i) <- rx.inits.data.toSet
        _ <- 1 to i
        (st2, lbl) <- Rel.get(st,rx.edgs) if rx.act((st, st2, lbl))
    yield
      val toAct = Rel.get((st, st2, lbl),rx.on)
      val toDeact = Rel.get((st, st2, lbl),rx.off)
      val newAct = (rx.act ++ toAct) -- toDeact // biased to deactivation
      val newInits = (rx.inits - st) + st2
      (st, st2, lbl) -> rx.copy(inits = newInits, act = newAct)

  /** Interprets an FRTS as an FTS by attaching a feature expression to each edge */
  def asFTS(pk: Map[Edge,FExp]): SOS[(Action,FExp),RTS] =
    new SOS[(Action,FExp),RTS]:
      override def accepting(s: RTS): Boolean =
        RTSSemantics.accepting(s)
      override def next[A >: (Action,FExp)](s: RTS): Set[(A, RTS)] = {
        nextEdge(s).map((edg,rts) => ((edg._3,pk.getOrElse(edg,FTrue)),rts))
      }


