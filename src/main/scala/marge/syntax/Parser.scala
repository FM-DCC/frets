package marge.syntax

import cats.parse.Parser.*
import cats.parse.{LocationMap, Parser as P, Parser0 as P0}
import cats.parse.Rfc5234.{alpha, digit, sp}
//import marge.syntax.Syntax.{QName, RxGraph}
import marge.syntax.RTS.{QName}
import marge.syntax.RTS
import marge.syntax.XFRTS

import scala.sys.error

object Parser :

  // /** Parse a command  */
  def parseProgram(str:String):FRTS =
    pp(program,str) match
      case Left(e) => error(e)
      case Right(c) => c.toFRTS

  /** Applies a parser to a string, and prettifies the error message */
  def pp[A](parser:P[A], str:String): Either[String,A] =
    parser.parseAll(str) match
      case Left(e) => Left(prettyError(str,e))
      case Right(x) => Right(x)

  private def prettyError(str:String, err:Error): String =
    val loc = LocationMap(str)
    val pos = loc.toLineCol(err.failedAtOffset) match
      case Some((x,y)) =>
        s"""<pre>at (${x+1},$y):
           |${loc.getLine(x).getOrElse("-")}
           |${("-" * y)+"^\n"}</pre>""".stripMargin
      case _ => ""
    s"${pos}expected: ${err.expected.toList.mkString(", ")}\noffsets: ${
      err.failedAtOffset};${err.offsets.toList.mkString(",")}"

  // Simple parsers for spaces and comments
  /** Parser for a sequence of spaces or comments */
  /** Parser for a sequence of spaces or comments *//** Parser for a sequence of spaces or comments */
  private val whitespace: P[Unit] = P.charIn(" \t\r\n").void
  private val comment: P[Unit] = string("//") *> P.charWhere(_!='\n').rep0.void
  private val sps: P0[Unit] = (whitespace | comment).rep0.void

  // Parsing smaller tokens
  def alphaDigit: P[Char] =
    P.charIn('A' to 'Z') | P.charIn('a' to 'z') | P.charIn('0' to '9') | P.charIn('_')
  private def Digit: P[Char] =
    P.charIn('0' to '9') | P.charIn('.')
  def varName: P[String] =
    (charIn('a' to 'z') ~ alphaDigit.rep0).string
  def procName: P[String] =
    (charIn('A' to 'Z') ~ alphaDigit.rep0).string
  def anyName: P[String] =
    ((charIn('a' to 'z') | charIn('A' to 'Z')) ~ alphaDigit.rep0).string
  private def symbols: P[String] =
  // symbols starting with "--" are meant for syntactic sugar of arrows, and ignored as symbols of terms
    P.not(string("--")).with1 *>
      oneOf("+-><!%/*=|&".toList.map(char)).rep.string

  import scala.language.postfixOps

  /////

  def program: P[XFRTS] =
    sps.with1 *> statements <* sps

  def statements: P[XFRTS] = P.recursive(rx =>
    statement(rx).repSep(sps)
      .map(res => res.toList.fold(XFRTS())(_ ++ _))
  )
  def statement(rx:P[XFRTS]): P[XFRTS] =
    init | aut(rx) | fm | select | check | edge

  def init: P[XFRTS] =
    (string("init") *> sps *> qname) // <* (sps<*char(';')))
      .map(XFRTS().addInit(_))
  def aut(rx:P[XFRTS]): P[XFRTS] =
    ((string("aut") *> sps *> qname) ~
      (sps *> char('{') *> sps *> (rx <* sps <* char('}')))
    ).map(x => x._1 / x._2)
  def fm: P[XFRTS] =
    (string("fm") *> sps *> fexp) // (fexp <* sps <* char(';')))
      .map(XFRTS().addFM(_))
  def select: P[XFRTS] =
    (string("select") *> sps *> ((qname <* sps).repSep0(char(',')*>sps) <* char(';')))
      .map(names => XFRTS().addSel(names.map(_.toString).toSet))
  def check: P[XFRTS] =
    (string("check") *> sps *> (trace|bism))
      .map(triple => XFRTS().addCheck(triple))
  def trace: P[(QName,QName,Boolean)] =
    ((string("Tr(") *> sps *> qname) ~ (sps *> char(')') *> sps  *> char('=') *>
        sps *> string("Tr(") *> sps *> (qname <* sps *> char(')') <* sps)))
      .map(pair => (pair._1, pair._2, false))
  def bism: P[(QName,QName,Boolean)] =
    (qname ~ (sps *> (string("<->")|string("~")) *> sps *> qname))
      .map(pair => (pair._1, pair._2, true))

  def edge: P[XFRTS] =
    ( (alias <* sps).?.with1 ~ // optional alias
      qname ~ // n1
      arrow.surroundedBy(sps) ~ // n2
      (qname <* sps) ~ // ar
      (char(':') *> sps *> (qname <* sps)).? ~ // mn3
      (string("if") *> sps *> (fexp <* sps)).? ~ // fe
      string("disabled").? // off
      //      <* char(';')))
    ).map{
        case ((((((al,n1),ar),n2),mn3),fe),None) =>
          ar(al,n1,n2,mn3.getOrElse(QName(List())),fe)
        case ((((((al,n1), ar), n2), mn3), fe), Some(_)) =>
          ar(al,n1,n2,mn3.getOrElse(QName(List())),fe)
            .deactivate(n1, n2, mn3.getOrElse(QName(List())))
    }

  def alias: P[String] =
    char('[') *> sps *> (anyName <* sps <* char(']'))

  def qname: P[QName] =
    alphaDigit.rep.string.repSep(char('.'))
      .map(l => QName(l.toList))

  def arrow: P[(Option[String],QName,QName,QName,Option[FExp])=>XFRTS] =
    string("-->").as(XFRTS().addEdge) |
    string("->>").as(XFRTS().addOn) |
    string("--!").as(XFRTS().addOff) |
    string("--x").as(XFRTS().addOff) |
    string("--#--").as((al:Option[String],a:QName,b:QName,c:QName,fe:Option[FExp]) => XFRTS()
      .addOff(al,a,b,c,fe).addOff(al,b,a,c,fe)) |
    string("---->").as((al:Option[String],a:QName,b:QName,c:QName,fe:Option[FExp]) => XFRTS()
      .addOn(al,a,b,c,fe).addOff(al,b,b,c,fe))

    /** Parse a feature expression */
  def fexp: P[FExp] = P.recursive((recFExp:P[FExp]) => {
    def lit: P[FExp] = P.recursive( (recLit:P[FExp]) =>
      string("true").as(FExp.FTrue) |
      string("false").as(FExp.FNot(FExp.FTrue)) |
      (char('!') *> recLit).map(FExp.FNot.apply) |
      qname.map(q => FExp.Feat(q.toString)) |
      char('(') *> recFExp.surroundedBy(sps) <* char(')')
    )

    def or: P[(FExp, FExp) => FExp] =
      (string("||")|string("\\/")).as(FExp.FOr.apply)

    def and: P[(FExp, FExp) => FExp] =
      (string("&&")|string("/\\")).as(FExp.FAnd.apply)

    def impl: P[(FExp, FExp) => FExp] =
      (string("->")|string("=>")).as(FExp.FImp.apply)

    def equiv: P[(FExp, FExp) => FExp] =
      (string("<->")|string("<=>")).as(FExp.FEq.apply)

    listSep(listSep(listSep(lit, and), or), impl | equiv)
  })

  //// Auxiliary functions

  def listSep[A](elem: P[A], op: P[(A, A) => A]): P[A] =
    (elem ~ (op.surroundedBy(sps).backtrack ~ elem).rep0)
      .map(x => {
        val pairlist = x._2
        val first = x._1;
        pairlist.foldLeft(first)((rest, pair) => pair._1(rest, pair._2))
      })

  //////////////////////////////
  // Examples and experiments //
  //////////////////////////////

  object Examples:
    val ex1 =
      """
        init = s0;
        l0={(s0,s1,a,0,Bullet),(s1,s1,b,0,Circ)};

        ln = {((s1,s1,b,0,Circ), (s1,s1,b,0,Circ),0,Bullet,ON),((s1,s1,b,0,Circ), (s2,s1,b,0,Circ),0,Bullet,ON),((s1,s1,b,0,Circ),((s1,s1,b,0,Circ),(s1,s1,b,0,Circ),0,Bullet,ON),0,Circ,OFF)}

      """