package com.plixlatform.it.sync.debug

import com.typesafe.config.Config
import com.plixlatform.it.NodeConfigs
import com.plixlatform.it.api.SyncHttpApi._
import com.plixlatform.it.transactions.NodesFromDocker
import org.scalatest.FunSuite

class DebugConfigInfo extends FunSuite with NodesFromDocker {

  override protected val nodeConfigs: Seq[Config] = NodeConfigs.newBuilder.withDefault(1).build()

  test("getting a configInfo") {
    nodes.head.getWithApiKey(s"/debug/configInfo?full=false")
    nodes.last.getWithApiKey(s"/debug/configInfo?full=true")
  }

}
