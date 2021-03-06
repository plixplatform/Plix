package com.plixplatform.lang
import com.plixplatform.common.state.ByteStr
import com.plixplatform.common.utils.EitherExt2
import com.plixplatform.lang.v1.compiler.Terms._

import scala.util.{Left, Right}

object Testing {

  def evaluated(i: Any): Either[String, EVALUATED] = i match {
    case s: String        => CONST_STRING(s)
    case s: Long          => Right(CONST_LONG(s))
    case s: Int           => Right(CONST_LONG(s))
    case s: ByteStr       => CONST_BYTESTR(s)
    case s: CaseObj       => Right(s)
    case s: Boolean       => Right(CONST_BOOLEAN(s))
    case a: Seq[_] => Right(ARR(a.map(x => evaluated(x).explicitGet()).toIndexedSeq))
    case _                => Left("Bad Assert: unexprected type")
  }
}
