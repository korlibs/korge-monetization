package korlibs.korge.admob

import korlibs.korge.view.*

actual suspend fun AdmobCreate(views: Views, testing: Boolean): Admob = AdmobCreateDefault(views, testing)
