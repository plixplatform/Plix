package com.plixplatform.protobuf.block
import cats.instances.all._
import cats.syntax.traverse._
import com.google.protobuf.ByteString
import com.plixplatform.account.{AddressScheme, PublicKey}
import com.plixplatform.block.SignerData
import com.plixplatform.common.state.ByteStr
import com.plixplatform.consensus.nxt.NxtLikeConsensusBlockData
import com.plixplatform.lang.ValidationError
import com.plixplatform.protobuf.transaction.{PBTransactions, VanillaTransaction}
import com.plixplatform.transaction.TxValidationError.GenericError

object PBBlocks {
  def vanilla(block: PBBlock, unsafe: Boolean = false): Either[ValidationError, VanillaBlock] = {
    def create(version: Int,
               timestamp: Long,
               reference: ByteStr,
               consensusData: NxtLikeConsensusBlockData,
               transactionData: Seq[VanillaTransaction],
               featureVotes: Set[Short],
               generator: PublicKey,
               signature: ByteStr): VanillaBlock = {
      VanillaBlock(timestamp, version.toByte, reference, SignerData(generator, signature), consensusData, transactionData, featureVotes)
    }

    for {
      header       <- block.header.toRight(GenericError("No block header"))
      transactions <- block.transactions.map(PBTransactions.vanilla(_, unsafe)).toVector.sequence
      result = create(
        header.version,
        header.timestamp,
        ByteStr(header.reference.toByteArray),
        NxtLikeConsensusBlockData(header.baseTarget, ByteStr(header.generationSignature.toByteArray)),
        transactions,
        header.featureVotes.map(intToShort).toSet,
        PublicKey(header.generator.toByteArray),
        ByteStr(block.signature.toByteArray)
      )
    } yield result
  }

  def protobuf(block: VanillaBlock): PBBlock = {
    import block._
    import consensusData._
    import signerData._

    new PBBlock(
      Some(
        PBBlock.Header(
          AddressScheme.current.chainId,
          ByteString.copyFrom(reference),
          baseTarget,
          ByteString.copyFrom(generationSignature),
          featureVotes.map(shortToInt).toSeq,
          timestamp,
          version,
          ByteString.copyFrom(generator)
        )),
      ByteString.copyFrom(signature),
      transactionData.map(PBTransactions.protobuf)
    )
  }

  def clearChainId(block: PBBlock): PBBlock = {
    block.update(
      _.header.chainId := 0,
      _.transactions.foreach(_.transaction.chainId := 0)
    )
  }

  def addChainId(block: PBBlock): PBBlock = {
    val chainId = AddressScheme.current.chainId

    block.update(
      _.header.chainId := chainId,
      _.transactions.foreach(_.transaction.chainId := chainId)
    )
  }

  private[this] def shortToInt(s: Short): Int = {
    java.lang.Short.toUnsignedInt(s)
  }

  private[this] def intToShort(int: Int): Short = {
    require(int >= 0 && int <= 65535, s"Short overflow: $int")
    int.toShort
  }
}
