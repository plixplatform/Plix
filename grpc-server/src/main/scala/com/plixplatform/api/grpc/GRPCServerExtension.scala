package com.plixplatform.api.grpc

import java.net.InetSocketAddress

import com.plixplatform.extensions.{Extension, Context => ExtensionContext}
import com.plixplatform.settings.GRPCSettings
import com.plixplatform.utils.ScorexLogging
import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import monix.execution.Scheduler
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.Future

class GRPCServerExtension(context: ExtensionContext) extends Extension with ScorexLogging {
  @volatile
  var server: Server = _

  override def start(): Unit = {
    val settings = context.settings.config.as[GRPCSettings]("plix.grpc")
    this.server = startServer(settings)
  }

  override def shutdown(): Future[Unit] = {
    log.debug("Shutting down gRPC server")
    if (server != null) {
      server.shutdown()
      Future(server.awaitTermination())(context.actorSystem.dispatcher)
    } else {
      Future.successful(())
    }
  }

  private[this] def startServer(settings: GRPCSettings): Server = {
    implicit val apiScheduler: Scheduler = Scheduler(context.actorSystem.dispatcher)

    val bindAddress = new InetSocketAddress(settings.host, settings.port)
    val server: Server = NettyServerBuilder
      .forAddress(bindAddress)
      .addService(TransactionsApiGrpc.bindService(new TransactionsApiGrpcImpl(context.wallet, context.blockchain, context.utx, context.broadcastTx), apiScheduler))
      .addService(BlocksApiGrpc.bindService(new BlocksApiGrpcImpl(context.blockchain), apiScheduler))
      .addService(AccountsApiGrpc.bindService(new AccountsApiGrpcImpl(context.blockchain), apiScheduler))
      .addService(AssetsApiGrpc.bindService(new AssetsApiGrpcImpl(context.blockchain), apiScheduler))
      .addService(BlockchainApiGrpc.bindService(new BlockchainApiGrpcImpl(context.blockchain, context.settings.featuresSettings), apiScheduler))
      .build()
      .start()

    log.info(s"gRPC API was bound to $bindAddress")
    server
  }
}
