package com.plixplatform.utils

import cats.kernel.Monoid
import com.typesafe.config.ConfigFactory
import com.plixplatform.account.{Address, Alias}
import com.plixplatform.block.{Block, BlockHeader}
import com.plixplatform.common.state.ByteStr
import com.plixplatform.lang.ValidationError
import com.plixplatform.lang.script.Script
import com.plixplatform.settings.BlockchainSettings
import com.plixplatform.state._
import com.plixplatform.state.reader.LeaseDetails
import com.plixplatform.transaction.Asset.IssuedAsset
import com.plixplatform.transaction.TxValidationError.GenericError
import com.plixplatform.transaction.lease.LeaseTransaction
import com.plixplatform.transaction.transfer.TransferTransaction
import com.plixplatform.transaction.{Asset, Transaction}

case object EmptyBlockchain extends Blockchain {
  override lazy val settings: BlockchainSettings = BlockchainSettings.fromRootConfig(ConfigFactory.load())

  override def height: Int = 0

  override def score: BigInt = 0

  override def blockHeaderAndSize(height: Int): Option[(BlockHeader, Int)] = None

  override def blockHeaderAndSize(blockId: ByteStr): Option[(BlockHeader, Int)] = None

  override def lastBlock: Option[Block] = None

  override def carryFee: Long = 0

  override def blockBytes(height: Int): Option[Array[Byte]] = None

  override def blockBytes(blockId: ByteStr): Option[Array[Byte]] = None

  override def heightOf(blockId: ByteStr): Option[Int] = None

  /** Returns the most recent block IDs, starting from the most recent  one */
  override def lastBlockIds(howMany: Int): Seq[ByteStr] = Seq.empty

  /** Returns a chain of blocks starting with the block with the given ID (from oldest to newest) */
  override def blockIdsAfter(parentSignature: ByteStr, howMany: Int): Option[Seq[ByteStr]] = None

  override def parentHeader(block: BlockHeader, back: Int): Option[Block] = None

  override def totalFee(height: Int): Option[Long] = None

  /** Features related */
  override def approvedFeatures: Map[Short, Int] = Map.empty

  override def activatedFeatures: Map[Short, Int] = Map.empty

  override def featureVotes(height: Int): Map[Short, Int] = Map.empty

  /** Block reward related */
  override def blockReward(height: Int): Option[Long] = None

  override def lastBlockReward: Option[Long] = None

  override def plixAmount(height: Int): BigInt = 0

  override def transferById(id: ByteStr): Option[(Int, TransferTransaction)] = None

  override def transactionInfo(id: ByteStr): Option[(Int, Transaction)] = None

  override def transactionHeight(id: ByteStr): Option[Int] = None

  override def containsTransaction(tx: Transaction): Boolean = false

  override def assetDescription(id: IssuedAsset): Option[AssetDescription] = None

  override def resolveAlias(a: Alias): Either[ValidationError, Address] = Left(GenericError("Empty blockchain"))

  override def leaseDetails(leaseId: ByteStr): Option[LeaseDetails] = None

  override def filledVolumeAndFee(orderId: ByteStr): VolumeAndFee = VolumeAndFee(0, 0)

  /** Retrieves Plix balance snapshot in the [from, to] range (inclusive) */
  override def balanceSnapshots(address: Address, from: Int, to: ByteStr): Seq[BalanceSnapshot] = Seq.empty

  override def accountScript(address: Address): Option[Script] = None

  override def hasScript(address: Address): Boolean = false

  override def assetScript(asset: IssuedAsset): Option[Script] = None

  override def hasAssetScript(asset: IssuedAsset): Boolean = false

  override def accountDataKeys(acc: Address): Seq[String] = Seq.empty

  override def accountData(acc: Address): AccountDataInfo = AccountDataInfo(Map.empty)

  override def accountData(acc: Address, key: String): Option[DataEntry[_]] = None

  override def balance(address: Address, mayBeAssetId: Asset): Long = 0

  override def leaseBalance(address: Address): LeaseBalance = LeaseBalance.empty

  override def collectActiveLeases[T](pf: PartialFunction[LeaseTransaction, T]): Seq[T] = Nil

  /** Builds a new portfolio map by applying a partial function to all portfolios on which the function is defined.
    *
    * @note Portfolios passed to `pf` only contain Plix and Leasing balances to improve performance */
  override def collectLposPortfolios[A](pf: PartialFunction[(Address, Portfolio), A]): Map[Address, A] = Map.empty

  override def invokeScriptResult(txId: TransactionId): Either[ValidationError, InvokeScriptResult] = Right(Monoid[InvokeScriptResult].empty)
}
