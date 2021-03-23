package com.plixlatform.lang

import com.plixlatform.lang.v1.BaseGlobal

package object hacks {
  private[lang] val Global: BaseGlobal = com.plixlatform.lang.Global // Hack for IDEA
}
