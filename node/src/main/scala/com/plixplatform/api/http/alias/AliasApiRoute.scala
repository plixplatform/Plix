package com.plixplatform.api.http.alias

import akka.http.scaladsl.server.Route
import com.plixplatform.account.Alias
import com.plixplatform.api.http.ApiError.AliasDoesNotExist
import com.plixplatform.api.http._
import com.plixplatform.http.BroadcastRoute
import com.plixplatform.settings.RestAPISettings
import com.plixplatform.state.Blockchain
import com.plixplatform.transaction._
import com.plixplatform.utils.Time
import com.plixplatform.utx.UtxPool
import com.plixplatform.wallet.Wallet
import io.netty.channel.group.ChannelGroup
import io.swagger.annotations._
import javax.ws.rs.Path
import play.api.libs.json.{Format, Json}

@Path("/alias")
@Api(value = "/alias")
case class AliasApiRoute(settings: RestAPISettings, wallet: Wallet, utx: UtxPool, allChannels: ChannelGroup, time: Time, blockchain: Blockchain)
    extends ApiRoute
    with BroadcastRoute
    with WithSettings {

  override val route = pathPrefix("alias") {
    alias ~ addressOfAlias ~ aliasOfAddress
  }

  def alias: Route = processRequest("create", (t: CreateAliasV1Request) => doBroadcast(TransactionFactory.aliasV1(t, wallet, time)))

  @Path("/by-alias/{alias}")
  @ApiOperation(
    value = "Address by alias",
    notes = "Returns an address associated with an Alias. Alias should be plain text without an 'alias' prefix and network code.",
    httpMethod = "GET"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(name = "alias", value = "Alias", required = true, dataType = "string", paramType = "path")
    ))
  def addressOfAlias: Route = (get & path("by-alias" / Segment)) { aliasName =>
    val result = Alias.create(aliasName) match {
      case Right(alias) =>
        blockchain.resolveAlias(alias) match {
          case Right(addr) => Right(Address(addr.stringRepr))
          case _ => Left(AliasDoesNotExist(alias))
        }
      case Left(err) => Left(ApiError.fromValidationError(err))
    }
    complete(result)
  }

  @Path("/by-address/{address}")
  @ApiOperation(value = "Aliases by address", notes = "Returns a collection of aliases associated with an address", httpMethod = "GET")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(name = "address", value = "Address", required = true, dataType = "string", paramType = "path")
    ))
  def aliasOfAddress: Route = (get & path("by-address" / Segment)) { addressString =>
    val result: Either[ApiError, Seq[String]] = com.plixplatform.account.Address
      .fromString(addressString)
      .map(acc => blockchain.aliasesOfAddress(acc).map(_.stringRepr).toVector)
      .left
      .map(ApiError.fromValidationError)
    complete(result)
  }

  case class Address(address: String)

  implicit val addressFormat: Format[Address] = Json.format
}
