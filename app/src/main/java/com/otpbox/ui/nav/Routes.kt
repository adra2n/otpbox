package com.otpbox.ui.nav

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val ADD = "add"
    const val IMPORT = "import"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    const val PASSWORDS = "passwords"
    const val PASSWORD_DETAIL = "password_detail/{id}"
    fun detail(id: String) = "detail/$id"
    fun passwordDetail(id: String?) = if (id == null) "password_detail/new" else "password_detail/$id"
}
