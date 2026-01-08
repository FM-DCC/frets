package marge.syntax

import marge.syntax.FExp.FTrue
import marge.syntax.RTS.Edge
import marge.backend.Rel.{Rel, add, join}

case class FRTS(rts: RTS, fm: FExp, pk: Map[Edge,FExp]):
  def getRTS: RTS = rts
  
  def addFM(newFM: FExp) =
    this.copy(fm = if fm==FTrue then newFM else fm && newFM)

  def ++(other: FRTS) =
    FRTS(rts ++ other.rts, fm && other.fm, pk ++ other.pk.map(
      kv => kv._1 -> (if pk.contains(kv._1) then pk(kv._1) && kv._2 else kv._2)))
  def ++(r: RTS): FRTS =
    this.copy(rts = rts ++ r)

//object FRTS:

