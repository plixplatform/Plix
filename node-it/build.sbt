import PlixDockerKeys._

enablePlugins(PlixDockerPlugin, ItTestPlugin)

description := "NODE integration tests"
libraryDependencies ++= Dependencies.itTest

inTask(docker)(
  Seq(
    imageNames := Seq(ImageName("com.plixplatform/node-it")),
    exposedPorts := Set(6870, 6869), // NetworkApi, RestApi
    additionalFiles ++= Seq(
      (LocalProject("node") / Universal / stage).value,
      (Test / resourceDirectory).value / "template.conf",
      (Test / sourceDirectory).value / "container" / "start-plix.sh"
    )
  ))

lazy val printTests = taskKey[Unit]("Print all available Test's")

printTests := {
  val logger: Logger = ConsoleLogger()
  val tests = (definedTests in Test).value
  tests.zipWithIndex foreach { case (t, i) =>
    logger.info(s"[$i] ${t.name}")
  }
}
