package com.plixplatform.lang

import com.plixplatform.lang.v1.BaseGlobal

package object hacks {
  private[lang] val Global: BaseGlobal = com.plixplatform.lang.Global // Hack for IDEA
}
