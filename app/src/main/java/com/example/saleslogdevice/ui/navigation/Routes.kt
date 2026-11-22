package com.example.saleslogdevice.ui.navigation

object Routes {
    const val HOME = "home"
    const val CHECK_IN = "checkin"
    const val REPORT = "report"
    const val SETTINGS = "settings"
    const val SHARE = "share"
    const val HISTORY = "history"

    /** 日報画面の sessionId 引数キー。未指定（< 0）なら今日の日報を表示する。 */
    const val REPORT_ARG_SESSION_ID = "sessionId"

    /** sessionId を任意で受け取る日報ルート。引数なしで navigate すると今日の日報になる。 */
    const val REPORT_WITH_ARG = "report?sessionId={sessionId}"

    /** 過去の日報を sessionId 付きで開くためのルート文字列を組み立てる。 */
    fun report(sessionId: Long): String = "report?sessionId=$sessionId"
}
