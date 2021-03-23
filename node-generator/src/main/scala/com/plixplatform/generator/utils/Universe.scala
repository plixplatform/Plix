package com.plixplatform.generator.utils

import com.plixplatform.generator.Preconditions.CreatedAccount
import com.plixplatform.transaction.assets.IssueTransaction
import com.plixplatform.transaction.lease.LeaseTransaction

object Universe {
  var Accounts: List[CreatedAccount] = Nil
  var IssuedAssets: List[IssueTransaction]        = Nil
  var Leases: List[LeaseTransaction]              = Nil
}
