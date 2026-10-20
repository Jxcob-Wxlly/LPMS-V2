package com.teampointblank.lpms.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SESSIONS = "sessions"
    const val LEARNERS = "learners"
    const val SETTINGS = "settings"
    const val CHANGE_PASSWORD = "change-password"
    const val HELP = "help"
    const val PARENT_HOME = "parent-home"
    const val REGISTER = "register"
    const val NEW_SESSION = "new-session"

    const val ARG_SESSION_ID = "sessionId"
    const val ARG_LEARNER_ID = "learnerId"
    const val SESSION = "session/{$ARG_SESSION_ID}"
    const val LEARNER = "learner/{$ARG_LEARNER_ID}"
    const val CHILD = "child/{$ARG_LEARNER_ID}"

    fun session(id: Int) = "session/$id"
    fun learner(id: Int) = "learner/$id"
    fun child(id: Int) = "child/$id"

    /** First screen after sign-in for each role. */
    fun homeFor(isParent: Boolean) = if (isParent) PARENT_HOME else HOME
}
