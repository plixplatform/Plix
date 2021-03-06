import java.io.File

import PlixDockerKeys._
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Def, Plugins, inTask, taskKey}
import sbtdocker.DockerPlugin
import sbtdocker.DockerPlugin.autoImport._

object PlixDockerPlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin && DockerPlugin

  override def projectSettings: Seq[Def.Setting[_]] =
    inTask(docker)(
      Seq(
        additionalFiles := Seq.empty,
        exposedPorts := Set.empty,
        baseImage := "anapsix/alpine-java:8_server-jre",
        dockerfile := {
          val yourKitArchive = "YourKit-JavaProfiler-2019.1-docker.zip"
          val bin            = "/opt/plix/start-plix.sh"

          new Dockerfile {
            from(baseImage.value)

            runRaw(s"""mkdir -p /opt/plix && \\
                    |apk update && \\
                    |apk add --no-cache openssl ca-certificates && \\
                    |wget --quiet "https://search.maven.org/remotecontent?filepath=org/aspectj/aspectjweaver/1.9.1/aspectjweaver-1.9.1.jar" -O /opt/plix/aspectjweaver.jar && \\
                    |wget --quiet "https://www.yourkit.com/download/docker/$yourKitArchive" -P /tmp/ && \\
                    |unzip /tmp/$yourKitArchive -d /usr/local && \\
                    |rm -f /tmp/$yourKitArchive""".stripMargin)

            add(additionalFiles.value, "/opt/plix/")
            runShell("chmod", "+x", bin)
            entryPoint(bin)
            expose(exposedPorts.value.toSeq: _*)
          }
        },
        buildOptions := BuildOptions(removeIntermediateContainers = BuildOptions.Remove.OnSuccess)
      ))
}

object PlixDockerKeys {
  val additionalFiles = taskKey[Seq[File]]("Additional files to copy to /opt/plix")
  val exposedPorts    = taskKey[Set[Int]]("Exposed ports")
  val baseImage       = taskKey[String]("A base image for this container")
}
