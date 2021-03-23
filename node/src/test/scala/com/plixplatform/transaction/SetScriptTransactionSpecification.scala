package com.plixplatform.transaction

import com.plixplatform.account.{KeyPair, PublicKey}
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.transaction.smart.SetScriptTransaction
import org.scalacheck.Gen
import play.api.libs.json._

class SetScriptTransactionSpecification extends GenericTransactionSpecification[SetScriptTransaction] {

  def transactionParser: com.plixplatform.transaction.TransactionParserFor[SetScriptTransaction] = SetScriptTransaction

  def updateProofs(tx: SetScriptTransaction, p: Proofs): SetScriptTransaction = {
    tx.copy(proofs = p)
  }

  def assertTxs(first: SetScriptTransaction, second: SetScriptTransaction): Unit = {
    first.sender.address shouldEqual second.sender.address
    first.timestamp shouldEqual second.timestamp
    first.fee shouldEqual second.fee
    first.version shouldEqual second.version
    first.proofs shouldEqual second.proofs
    first.bytes() shouldEqual second.bytes()
    first.script shouldEqual second.script
  }

  def generator: Gen[((Seq[com.plixplatform.transaction.Transaction], SetScriptTransaction))] = setScriptTransactionGen.map(t => (Seq(), t))

  def jsonRepr: Seq[(JsValue, SetScriptTransaction)] =
    Seq(
      (Json.parse("""{
                       "type": 13,
                       "id": "9TQamA3jMsREnuVdxHtaPAm3ECfqbpyY2xJ6txVsyiMn",
                       "sender": "3JTDzz1XbK7KeRJXGqpaRFraC92ebStimJ9",
                       "senderPublicKey": "FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z",
                       "fee": 100000,
                       "feeAssetId": null,
                       "timestamp": 1526983936610,
                       "proofs": [
                       "tcTr672rQ5gXvcA9xCGtQpkHC8sAY1TDYqDcQG7hQZAeHcvvHFo565VEv1iD1gVa3ZuGjYS7hDpuTnQBfY2dUhY"
                       ],
                       "version": 1,
                       "chainId": 75,
                       "script": null
                       }
    """),
       SetScriptTransaction
         .create(
           PublicKey.fromBase58String("FM5ojNqW7e9cZ9zhPYGkpSP1Pcd8Z3e3MNKYVS5pGJ8Z").explicitGet(),
           None,
           100000,
           1526983936610L,
           Proofs(Seq(ByteStr.decodeBase58("tcTr672rQ5gXvcA9xCGtQpkHC8sAY1TDYqDcQG7hQZAeHcvvHFo565VEv1iD1gVa3ZuGjYS7hDpuTnQBfY2dUhY").get))
         )
         .right
         .get))

  def transactionName: String = "SetScriptTransaction"

  property("SetScriptTransaction id doesn't depend on proof (spec)") {
    forAll(accountGen, proofsGen, proofsGen, contractOrExpr) {
      case (acc: KeyPair, proofs1, proofs2, script) =>
        val tx1 = SetScriptTransaction.create(acc, Some(script), 1, 1, proofs1).explicitGet()
        val tx2 = SetScriptTransaction.create(acc, Some(script), 1, 1, proofs2).explicitGet()
        tx1.id() shouldBe tx2.id()
    }
  }

}
