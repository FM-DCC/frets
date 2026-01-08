package marge.frontend

import caos.frontend.Site.initSite
import marge.syntax.FRTS

/** Main function called by ScalaJS' compiled javascript when loading. */
object Main {
  def main(args: Array[String]):Unit =
//    initSite[System](CaosConfig)
    initSite[FRTS](CaosConfig)
}