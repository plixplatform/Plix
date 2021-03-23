package com.plixplatform.settings

import java.io.File

import com.plixplatform.common.state.ByteStr

case class WalletSettings(file: Option[File], password: Option[String], seed: Option[ByteStr])
