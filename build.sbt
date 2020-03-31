name := "simple-terminal-zio"

version := "0.0.1"
scalaVersion := "2.13.1"
scalacOptions += ("-deprecation")
val zioVersion       = "1.0.0-RC18-2"
val scalaTestVersion = "3.0.8"

mainClass in (Compile, run) := Some("id.nukuba.Main")

libraryDependencies ++= Seq(
  "dev.zio" 	%% "zio"          % zioVersion,
  "dev.zio" 	%% "zio-interop-cats" % "2.0.0.0-RC12",
  "io.iteratee" %% "iteratee-core" % "0.19.0",
  "io.iteratee" %% "iteratee-files" % "0.19.0",
  "dev.zio" 	%% "zio-test"     % zioVersion % "test",
  "dev.zio" 	%% "zio-test-sbt" % zioVersion % "test",
)
testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

addCommandAlias("com", "all compile test:compile")
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
