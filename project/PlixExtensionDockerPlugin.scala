import PlixDockerKeys._
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Def, Plugins, inTask}
import sbtdocker.DockerPlugin
import sbtdocker.DockerPlugin.autoImport._

object PlixExtensionDockerPlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin && DockerPlugin

  override def projectSettings: Seq[Def.Setting[_]] =
    inTask(docker)(
      Seq(
        additionalFiles := Seq.empty,
        exposedPorts := Set.empty,
        baseImage := "com.plixplatform/node-it:latest",
        dockerfile := {
          new Dockerfile {
            from(baseImage.value)
            add(additionalFiles.value, "/opt/plix/")
            expose(exposedPorts.value.toSeq: _*)
          }
        },
        buildOptions := BuildOptions(removeIntermediateContainers = BuildOptions.Remove.OnSuccess)
      ))
}
