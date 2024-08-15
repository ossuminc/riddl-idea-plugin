import sbt.*
import sbt.librarymanagement.ModuleID

/** V - Dependency Versions object */

object V {
  val lang3 = "3.14.0"
  val pureconfig = "0.17.6"
  val scalacheck = "1.17.0"
  val scalatest = "3.2.18"
  val scopt = "4.1.0"
  val slf4j = "2.0.4"
  val ossumRiddl = "0.48.0-8-9aefadb2"
}

object Dep {
  val lang3 = "org.apache.commons" % "commons-lang3" % V.lang3
  val pureconfig = "com.github.pureconfig" %% "pureconfig-core" % V.pureconfig
  val scalactic = "org.scalactic" %% "scalactic" % V.scalatest
  val scalatest = "org.scalatest" %% "scalatest" % V.scalatest
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck
  val scopt = "com.github.scopt" %% "scopt" % V.scopt
  val slf4j = "org.slf4j" % "slf4j-nop" % V.slf4j
  val riddlCommands =
    "com.ossuminc" %% "riddl-commands" % V.ossumRiddl
  val minimalJson = {
    "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5" withSources ()
  }
  val fansi = "com.lihaoyi" %% "fansi" % "0.5.0"

  val basic: Seq[ModuleID] = Seq(minimalJson, scalactic, scalatest, scalacheck)
  val riddl: Seq[ModuleID] = Seq(
    riddlCommands
  )

  val testing: Seq[ModuleID] =
    Seq(scalactic % "test", scalatest % "test", scalacheck % "test")
}
