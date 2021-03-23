package com.plixlatform.lang.script
import com.plixlatform.lang.ValidationError.ScriptParseError
import com.plixlatform.lang.contract.ContractSerDe
import com.plixlatform.lang.directives.DirectiveDictionary
import com.plixlatform.lang.directives.values._
import com.plixlatform.lang.script.v1.ExprScript
import com.plixlatform.lang.v1.{BaseGlobal, ContractLimits, Serde}

object ScriptReader {

  private val Global: BaseGlobal = com.plixlatform.lang.Global // Hack for IDEA

  val checksumLength = 4

  def fromBytes(bytes: Array[Byte], checkComplexity: Boolean = true): Either[ScriptParseError, Script] = {
    val checkSum          = bytes.takeRight(checksumLength)
    val computedCheckSum  = Global.secureHash(bytes.dropRight(checksumLength)).take(checksumLength)
    val versionByte: Byte = bytes.head
    (for {
      a <- {
        if (versionByte == 0)
          Right((DirectiveDictionary[ContentType].idMap(bytes(1)), DirectiveDictionary[StdLibVersion].idMap(bytes(2)), 3))
        else if (versionByte == V1.id || versionByte == V2.id || versionByte == V3.id)
          Right((Expression, DirectiveDictionary[StdLibVersion].idMap(versionByte.toInt), 1))
        else Left(ScriptParseError(s"Can't parse script bytes starting with [${bytes(0).toInt},${bytes(1).toInt},${bytes(2).toInt}]"))
      }
      (scriptType, stdLibVersion, offset) = a
      scriptBytes                         = bytes.drop(offset).dropRight(checksumLength)

      _ <- Either.cond(java.util.Arrays.equals(checkSum, computedCheckSum), (), ScriptParseError("Invalid checksum"))
      s <- scriptType match {
        case Expression =>
          for {
            _ <- if (checkComplexity) {
              ExprScript.validateBytes(scriptBytes)
            } else {
              Right(())
            }
            bytes <- Serde.deserialize(scriptBytes).map(_._1)
            s     <- ExprScript(stdLibVersion, bytes, checkSize = false, checkComplexity = checkComplexity)
          } yield s
        case DApp =>
          for {
            dapp <- ContractSerDe.deserialize(scriptBytes)
            _ <- Either
              .cond(
                dapp.meta.size <= ContractLimits.MaxContractMetaSizeInBytes,
                (),
                s"Script meta size in bytes must be not greater than ${ContractLimits.MaxContractMetaSizeInBytes}"
              )
            s <- ContractScript(stdLibVersion, dapp)
          } yield s
      }
    } yield s).left
      .map(m => ScriptParseError(m.toString))
  }

}
