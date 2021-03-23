package com.plixplatform.http

import com.plixplatform.api.http.{ApiError, WithSettings}
import com.plixplatform.lang.ValidationError
import com.plixplatform.network._
import com.plixplatform.transaction.Transaction
import com.plixplatform.transaction.smart.script.trace.TracedResult
import com.plixplatform.utx.UtxPool
import io.netty.channel.group.ChannelGroup

trait BroadcastRoute {
  self: WithSettings =>
  def utx: UtxPool
  def allChannels: ChannelGroup

  protected def doBroadcast(v: Either[ValidationError, Transaction]): TracedResult[ApiError, Transaction] = {
    val result = for {
      transaction <- TracedResult(v)
      isNew <- utx.putIfNew(transaction)
    } yield {
      if (isNew || settings.allowTxRebroadcasting) allChannels.broadcastTx(transaction, None)
      transaction
    }

    result.leftMap(ApiError.fromValidationError)
  }
}
