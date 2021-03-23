package com.plixlatform.http

import com.plixlatform.api.http.{ApiError, WithSettings}
import com.plixlatform.lang.ValidationError
import com.plixlatform.network._
import com.plixlatform.transaction.Transaction
import com.plixlatform.transaction.smart.script.trace.TracedResult
import com.plixlatform.utx.UtxPool
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
