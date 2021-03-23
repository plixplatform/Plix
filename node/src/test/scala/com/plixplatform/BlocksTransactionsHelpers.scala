package com.plixlatform
import com.plixlatform.account.{AddressOrAlias, KeyPair}
import com.plixlatform.block.{Block, MicroBlock, SignerData}
import com.plixlatform.common.state.ByteStr
import com.plixlatform.common.utils._
import com.plixlatform.consensus.nxt.NxtLikeConsensusBlockData
import com.plixlatform.history.DefaultBaseTarget
import com.plixlatform.state.StringDataEntry
import com.plixlatform.transaction.Asset.Plix
import com.plixlatform.transaction.lease.{LeaseCancelTransactionV1, LeaseTransactionV1}
import com.plixlatform.transaction.transfer.{TransferTransactionV1, TransferTransactionV2}
import com.plixlatform.transaction.{DataTransaction, Transaction}
import org.scalacheck.Gen

trait BlocksTransactionsHelpers { self: TransactionGen =>
  object QuickTX {
    val FeeAmount = 400000

    def transfer(from: KeyPair,
                 to: AddressOrAlias = accountGen.sample.get,
                 amount: Long = smallFeeGen.sample.get,
                 timestamp: Gen[Long] = timestampGen): Gen[Transaction] =
      for {
        timestamp <- timestamp
      } yield TransferTransactionV1.selfSigned(Plix, from, to, amount, timestamp, Plix, FeeAmount, Array.empty).explicitGet()

    def transferV2(from: KeyPair,
                   to: AddressOrAlias = accountGen.sample.get,
                   amount: Long = smallFeeGen.sample.get,
                   timestamp: Gen[Long] = timestampGen): Gen[Transaction] =
      for {
        timestamp <- timestamp
      } yield TransferTransactionV2.selfSigned(Plix, from, to, amount, timestamp, Plix, FeeAmount, Array.empty).explicitGet()

    def lease(from: KeyPair,
              to: AddressOrAlias = accountGen.sample.get,
              amount: Long = smallFeeGen.sample.get,
              timestamp: Gen[Long] = timestampGen): Gen[LeaseTransactionV1] =
      for {
        timestamp <- timestamp
      } yield LeaseTransactionV1.selfSigned(from, amount, FeeAmount, timestamp, to).explicitGet()

    def leaseCancel(from: KeyPair, leaseId: ByteStr, timestamp: Gen[Long] = timestampGen): Gen[LeaseCancelTransactionV1] =
      for {
        timestamp <- timestamp
      } yield LeaseCancelTransactionV1.selfSigned(from, leaseId, FeeAmount, timestamp).explicitGet()

    def data(from: KeyPair, dataKey: String, timestamp: Gen[Long] = timestampGen): Gen[DataTransaction] =
      for {
        timestamp <- timestamp
      } yield DataTransaction.selfSigned(from, List(StringDataEntry(dataKey, Gen.numStr.sample.get)), FeeAmount, timestamp).explicitGet()
  }

  object UnsafeBlocks {
    def unsafeChainBaseAndMicro(totalRefTo: ByteStr,
                                base: Seq[Transaction],
                                micros: Seq[Seq[Transaction]],
                                signer: KeyPair,
                                version: Byte,
                                timestamp: Long): (Block, Seq[MicroBlock]) = {
      val block = unsafeBlock(totalRefTo, base, signer, version, timestamp)
      val microBlocks = micros
        .foldLeft((block, Seq.empty[MicroBlock])) {
          case ((lastTotal, allMicros), txs) =>
            val (newTotal, micro) = unsafeMicro(totalRefTo, lastTotal, txs, signer, version, timestamp)
            (newTotal, allMicros :+ micro)
        }
        ._2
      (block, microBlocks)
    }

    def unsafeMicro(totalRefTo: ByteStr,
                    prevTotal: Block,
                    txs: Seq[Transaction],
                    signer: KeyPair,
                    version: Byte,
                    ts: Long): (Block, MicroBlock) = {
      val newTotalBlock = unsafeBlock(totalRefTo, prevTotal.transactionData ++ txs, signer, version, ts)
      val unsigned      = new MicroBlock(version, signer, txs, prevTotal.uniqueId, newTotalBlock.uniqueId, ByteStr.empty)
      val signature     = crypto.sign(signer, unsigned.bytes())
      val signed        = unsigned.copy(signature = ByteStr(signature))
      (newTotalBlock, signed)
    }

    def unsafeBlock(reference: ByteStr,
                    txs: Seq[Transaction],
                    signer: KeyPair,
                    version: Byte,
                    timestamp: Long,
                    bTarget: Long = DefaultBaseTarget): Block = {
      val unsigned = Block(
        version = version,
        timestamp = timestamp,
        reference = reference,
        consensusData = NxtLikeConsensusBlockData(
          baseTarget = bTarget,
          generationSignature = com.plixlatform.history.generationSignature
        ),
        transactionData = txs,
        signerData = SignerData(
          generator = signer,
          signature = ByteStr.empty
        ),
        featureVotes = Set.empty
      )

      unsigned.copy(signerData = SignerData(signer, ByteStr(crypto.sign(signer, unsigned.bytes()))))
    }
  }
}
