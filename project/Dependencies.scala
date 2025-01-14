import sbt.*
import sbt.librarymanagement.ModuleID

/** V - Dependency Versions object */

object V {
  val lang3 = "3.14.0"
  val riddl = "1.0.0-RC5"
  val scalatest = "3.2.19"
  val scopt = "4.1.0"
  val slf4j = "2.0.4"
}

object Dep {
  val junit = "org.scalatestplus" %% "junit-4-13" % (V.scalatest + ".0") % Test
  val lang3 = "org.apache.commons" % "commons-lang3" % V.lang3
  val slf4j = "org.slf4j" % "slf4j-nop" % V.slf4j
  val minimalJson = {
    "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5" withSources ()
  }
  val kotlin = "org.jetbrains.kotlin" % "kotlin-stdlib" % "2.0.20"
  val riddlCommands = "com.ossuminc" % "riddl-commands_3" % V.riddl
  val jbKotlin = "org.jetbrains.kotlin" % "kotlin-stdlib" % "2.0.20"
  val jbAnnotations = "org.jetbrains" % "annotations" % "24.1.0"

  val riddlCommands = "com.ossuminc" % "riddl-commands_3" % "0.56.0-20-18b4853b"

  val basic: Seq[ModuleID] = Seq(minimalJson, scalactic, scalatest, scalacheck)
  val jetbrains: Seq[ModuleID] = Seq(jbKotlin, jbAnnotations)
  val testing: Seq[ModuleID] =
    Seq(scalactic % "test", scalatest % "test", scalacheck % "test")
}
