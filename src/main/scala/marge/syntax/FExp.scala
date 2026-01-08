package marge.syntax

import scala.annotation.tailrec
import scala.collection.immutable.{AbstractSet, SortedSet}

enum FExp:
  case FTrue
  case Feat(n:String)
  case FAnd(e1:FExp,e2:FExp)
  case FOr(e1:FExp,e2:FExp)
  case FNot(e:FExp)
// to simplify notation
  case FImp(e1:FExp,e2:FExp)
  case FEq(e1:FExp,e2:FExp)

  def feats: List[String] = this match
    case FTrue        => List()
    case Feat(name)   => List(name)
    case FAnd(e1, e2) => e1.feats ++ e2.feats
    case FOr(e1, e2)  => e1.feats ++ e2.feats
    case FNot(e)      => e.feats
    case FImp(e1, e2) => e1.feats ++ e2.feats
    case FEq(e1, e2)  => e1.feats ++ e2.feats

  /**
    * Checks if a given instantiation of features satisfies the feature expression
    * @param sol instantiation of features
    * @return
    */
  def check(sol:Map[String,Boolean]): Boolean = this match
    case FTrue        => true
    case Feat(name)   => sol.getOrElse(name,false) // elements not in the solution are considered false
    case FAnd(e1, e2) => e1.check(sol) && e2.check(sol)
    case FOr(e1, e2)  => e1.check(sol) || e2.check(sol)
    case FNot(e)      => !e.check(sol)
    // removing syntactic sugar
    case FImp(e1, e2) => (FNot(e1)||e2).check(sol)
    case FEq(e1, e2)  => ((e1-->e2)&&(e2-->e1)).check(sol)

  import FExp.Literal

  /** Returns a disjointive normal form, capturing the possible solutions. */
  def dnf: Set[Set[Literal]] = this match
    case FTrue => Set(Set())
    case Feat(n) => Set(Set(Right(n)))
    case FAnd(e1, e2) =>
      for ands1 <- e1.dnf; ands2 <- e2.dnf
          newAnds <- joinAnd(ands1,ands2).toSet
      yield newAnds
    case FOr(e1, e2) => e1.dnf ++ e2.dnf
    case FNot(e) => e match
      case FTrue => Set()
      case Feat(n) => Set(Set(Left(n)))
      case FAnd(e1, e2) => (FNot(e1) || FNot(e2)).dnf
      case FOr(e1, e2) => (FNot(e1) && FNot(e2)).dnf
      case FNot(e) => e.dnf
      case FImp(e1, e2) => FNot(e2).dnf ++ e1.dnf
      case FEq(e1, e2) => FNot((e1-->e2)&&(e2-->e1)).dnf

    case FImp(e1, e2) => FNot(e1).dnf ++ e2.dnf
    case FEq(e1, e2) => ((e1 --> e2)&&(e2 --> e1)).dnf

  @tailrec
  private def joinAnd(a1:Set[Literal], a2:Set[Literal]): Option[Set[Literal]] =
    a2.headOption match
      case Some(next) =>
        if a1.contains(dual(next)) then None
        else joinAnd(a1+next,a2-next)
      case None => Some(a1)

  private def dual(l:Literal): Literal =
    l.fold(Right.apply,Left.apply)


  /**
    * Calculates the set of products allowed by the feature expression
    * w.r.t a set of features
    * @param fts set of features
    * @return the set of all valid feature selections, i.e., a set of valid products
    */
  def products(fts:Set[String]): Set[Set[String]] =
    val used = feats.toSet
    val ftsNotUsed = fts -- feats.toSet
    val sols = for sol <- dnf yield
      expand(sol,used,ftsNotUsed)
      //println(s"== $this ==\nexpanding ${sol.mkString(" & ")} (with $used / ${ftsNotUsed})\ngot ${res.mkString(" -- ")}")
    sols.flatten

  /** Adds new combinations of possible solutions */
  private def expand(sol: Set[Literal], used: Set[String], more: Set[String]): Set[Set[String]] =
    val pos: Set[String] = for case Right(n) <- sol yield n
    val extra = more ++ (used -- sol.map(_.fold(x=>x,x=>x)))
    val opts:Set[Set[String]] = extra.subsets().toSet
    opts.map(set => pos ++ set)

  def &&(other:FExp): FExp = try
    if this == FTrue then other else FAnd(this,other)
  catch
    case e:StackOverflowError => sys.error("Stack overflow when creating new FAnd")
    case e:Throwable => throw e

  def ||(other:FExp): FExp = try
    if this == FNot(FTrue) then other else FOr(this,other)
  catch
    case e:StackOverflowError => sys.error("Stack overflow when creating new FOr")
    case e:Throwable => throw e

  def -->(other:FExp) =  try
    FImp(this,other) //FNot(this) || other
  catch
    case e:StackOverflowError => sys.error("Stack overflow when creating new FImp")
    case e:Throwable => throw e

  def <->(other:FExp) = try
    FEq(this,other)  //(this --> other) && (other --> this)
  catch
    case e: StackOverflowError => sys.error("Stack overflow when creating new FEq")
    case e: Throwable => throw e

object FExp:
  /** Either a negative (left) or a positive (right) feature name. */
  type Literal = Either[String, String]
