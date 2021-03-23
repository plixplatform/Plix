package com.plixlatform.generator.utils

import com.plixlatform.generator.Preconditions.CreatedAccount
import com.plixlatform.transaction.assets.IssueTransaction
import com.plixlatform.transaction.lease.LeaseTransaction

object Universe {
  var Accounts: List[CreatedAccount] = Nil
  var IssuedAssets: List[IssueTransaction]        = Nil
  var Leases: List[LeaseTransaction]              = Nil
}
