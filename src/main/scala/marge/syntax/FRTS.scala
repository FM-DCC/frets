package marge.syntax

import marge.syntax.FExp.{FTrue, Literal}
import marge.syntax.RTS.{Edge, QName, toMermaid, State}
import marge.backend.Rel.{Rel, add, join}

case class FRTS(rts: RTS,
                fm: FExp,
                pk: Map[Edge,FExp],
                main: Set[String],
                equivs: List[(State,State,Boolean)]): // Boolean indicates if the equivalence is "bisimularity"
  def getRTS: RTS = project(main)//rts

  def feats: Set[String] =
    pk.values.toSet.flatMap(_.feats) ++ fm.feats
    
  def products: Set[Set[String]] =
    fm.products(feats)
  
  def addFM(newFM: FExp) =
    this.copy(fm = if fm==FTrue then newFM else fm && newFM)

  def addSel(selected: Set[String]) =
    this.copy(main = selected)

  def ++(other: FRTS) =
    FRTS(rts ++ other.rts,
         fm && other.fm,
         pk ++ other.pk.map(
          kv => kv._1 -> (if pk.contains(kv._1) then pk(kv._1) && kv._2 else kv._2)),
         main ++ other.main,
         equivs ++ other.equivs
    )
  def ++(r: RTS): FRTS =
    this.copy(rts = rts ++ r)

  def project(sel: Set[String]): RTS = {
    val sol: Map[String,Boolean] = sel.map(_ -> true).toMap
    if (!fm.check(sol))
      sys.error(s"Feature selection {${sel.mkString(",")}} does not obey the feature model ${Show(fm)}")
    val keepEdges = for (from,tos) <- rts.edgs.toSet; (to,a) <- tos
                         if pk.getOrElse((from,to,a),FExp.FTrue).check(sol)
                      yield (from,to,a)
    val newAct = rts.act.filter(keepEdges(_))
    val newEdges = for (from,tos) <- rts.edgs yield
      (from,tos.filter((to,a) => keepEdges(from,to,a)))
//    val newEdges2 = for (f,tos)<-newEdges.toSet; (a,to)<-tos yield (f,a,to)
    val newOn = for (e1,e2s) <- rts.on
                    if keepEdges(e1)
                yield (e1, e2s.filter(e => keepEdges(e)))
    val newOff = for (e1,e2s) <- rts.off
                     if keepEdges(e1)
                  yield (e1, e2s.filter(e => keepEdges(e)))

    //sys.error(s"--\nrts.act: ${rts.act}\n--\nnewAct: ${newAct}\n--\nremoved: ${dropActions}")
    rts.copy(edgs = newEdges, on = newOn, off = newOff, act = newAct)
  }


object FRTS:
  def toMermaid(f: FRTS): String =
    RTS.toMermaid(f.rts)(using f.pk)


