package com.plixlatform.settings

import java.io.File

import com.plixlatform.common.state.ByteStr

case class WalletSettings(file: Option[File], password: Option[String], seed: Option[ByteStr])
