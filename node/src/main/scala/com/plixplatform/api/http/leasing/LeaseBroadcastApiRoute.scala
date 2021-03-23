package com.plixplatform.api.http.leasing

import akka.http.scaladsl.server.Route
import com.plixplatform.api.http._
import com.plixplatform.http.BroadcastRoute
import com.plixplatform.settings.RestAPISettings
import com.plixplatform.utx.UtxPool
import io.netty.channel.group.ChannelGroup

case class LeaseBroadcastApiRoute(settings: RestAPISettings, utx: UtxPool, allChannels: ChannelGroup)
    extends ApiRoute
    with BroadcastRoute
    with WithSettings {
  override val route = pathPrefix("leasing" / "broadcast") {
    signedLease ~ signedLeaseCancel
  }

  def signedLease: Route = (path("lease") & post) {
    json[SignedLeaseV1Request] { leaseReq =>
      doBroadcast(leaseReq.toTx)
    }
  }

  def signedLeaseCancel: Route = (path("cancel") & post) {
    json[SignedLeaseCancelV1Request] { leaseCancelReq =>
      doBroadcast(leaseCancelReq.toTx)
    }
  }
}
