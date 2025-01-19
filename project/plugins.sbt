addSbtPlugin("com.ossuminc" % "sbt-ossuminc" % "0.21.0")

addSbtPlugin("org.jetbrains" % "sbt-idea-plugin" % "3.26.2")

addSbtPlugin("org.jetbrains.scala" % "sbt-kotlin-plugin" % "3.0.3")

// This enables sbt-bloop to create bloop config files for Metals editors
// Uncomment locally if you use metals, otherwise don't slow down other
// people's builds by leaving it commented in the repo.
// addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.6")
