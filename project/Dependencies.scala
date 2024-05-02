import sbt.*
import sbt.Keys.baseDirectory
import sbt.librarymanagement.ModuleID

/** V - Dependency Versions object */

object V {
  val lang3 = "3.14.0"
  val pureconfig = "0.17.6"
  val scalacheck = "1.17.0"
  val scalatest = "3.2.18"
  val scopt = "4.1.0"
  val slf4j = "2.0.4"
  val ossumRiddl = "0.45.0"
}

object Dep {
  val currentDirectory: String = new java.io.File(".").getCanonicalPath

  val lang3 = "org.apache.commons" % "commons-lang3" % V.lang3
  val pureconfig = "com.github.pureconfig" %% "pureconfig-core" % V.pureconfig
  val scalactic = "org.scalactic" %% "scalactic" % V.scalatest
  val scalatest = "org.scalatest" %% "scalatest" % V.scalatest
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck
  val scopt = "com.github.scopt" %% "scopt" % V.scopt
  val slf4j = "org.slf4j" % "slf4j-nop" % V.slf4j
  val riddlc = "com.ossuminc" %% "riddlc" % V.ossumRiddl
  val riddlTestkit = "com.ossuminc" %% "riddl-testkit" % V.ossumRiddl
  val riddlHugo = "com.ossuminc" %% "riddl-hugo" % V.ossumRiddl % "test"
  val minimalJson =
    "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5" withSources ()

  val basic: Seq[ModuleID] = Seq(minimalJson, scalactic, scalatest, scalacheck)
  val riddl: Seq[ModuleID] = Seq(riddlTestkit, riddlc)

  val testing: Seq[ModuleID] =
    Seq(scalactic % "test", scalatest % "test", scalacheck % "test")
}
