package com.plixplatform.it.transactions

import com.plixplatform.it.{Docker, DockerBased, Node, Nodes}
import monix.eval.Coeval
import org.scalatest.Suite

trait NodesFromDocker extends Nodes with DockerBased { _: Suite =>
  protected val dockerNodes: Coeval[Seq[Docker.DockerNode]] = dockerSingleton
    .map(_.startNodes(nodeConfigs))
    .memoize

  override protected def nodes: Seq[Node] = dockerNodes()
}
