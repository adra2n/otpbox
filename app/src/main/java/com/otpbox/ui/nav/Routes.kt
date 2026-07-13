package com.otpbox.ui.nav

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val ADD = "add"
    const val IMPORT = "import"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    fun detail(id: String) = "detail/$id"
}
