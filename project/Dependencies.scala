import sbt.*
import sbt.librarymanagement.ModuleID

/** V - Dependency Versions object */

object V {
  val lang3 = "3.14.0"
  val pureconfig = "0.17.7"
  val riddl = "1.0.0-RC2-1-f0515ead"
  val scalacheck = "1.17.0"
  val scopt = "4.1.0"
  val slf4j = "2.0.4"
}

object Dep {
  val lang3 = "org.apache.commons" % "commons-lang3" % V.lang3
  val pureconfig = "com.github.pureconfig" %% "pureconfig-core" % V.pureconfig
  val slf4j = "org.slf4j" % "slf4j-nop" % V.slf4j
  val minimalJson = {
    "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5" withSources ()
  }
  val kotlin = "org.jetbrains.kotlin" % "kotlin-stdlib" % "2.0.20"
  val riddlCommands = "com.ossuminc" % "riddl-commands_3" % V.riddl

}
