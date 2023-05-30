package korlibs.korge.admob

import korlibs.io.*
import korlibs.korge.view.Views
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

actual fun AdmobCreate(views: Views, testing: Boolean): Admob {
	val admob: AdmobJsInterface = jsGlobal.asDynamic().admob ?: return AdmobCreateDefault(views, testing)
	return AdmobJs(admob, views, testing)
}

class AdmobJs(val admob: AdmobJsInterface, views: Views, val testing: Boolean) : Admob(views) {
	val admobBanner = admob.banner
	val admobInterstitial = admob.interstitial
	val admobRewardvideo = admob.rewardvideo

	override suspend fun available() = true

	override suspend fun bannerPrepare(config: Admob.Config) {
		@Suppress("unused")
		admobBanner.config(config.convert(testing))
		admobBanner.prepare().awaitDebug("admobBanner.prepare()")
	}

	override suspend fun bannerShow(): Unit {
		admobBanner.show().awaitDebug("admobBanner.show()")
	}
	override suspend fun bannerHide(): Unit {
		admobBanner.hide().awaitDebug("admobBanner.hide()")
	}

	override suspend fun interstitialPrepare(config: Admob.Config) {
		admobInterstitial.config(config.convert(testing))
		admobInterstitial.prepare().awaitDebug("admobInterstitial.prepare()")
	}

	override suspend fun interstitialIsLoaded(): Boolean {
		return admobInterstitial.isReady().awaitDebug("admobInterstitial.isReady()")
	}

	override suspend fun interstitialShowAndWait(): Boolean {
		admobInterstitial.show().awaitDebug("admobInterstitial.show()")
		return true
	}

	/////////////////

	override suspend fun rewardvideoPrepare(config: Admob.Config) {
		admobRewardvideo.config(config.convert(testing))
		admobRewardvideo.prepare().awaitDebug("admobRewardvideo.prepare()")
	}

	override suspend fun rewardvideoIsLoaded(): Boolean {
		return admobRewardvideo.isReady().awaitDebug("admobRewardvideo.isReady()")
	}
	override suspend fun rewardvideoShowAndWait(): Boolean {
		admobRewardvideo.show().awaitDebug("admobRewardvideo.show")
		return true
	}
}

suspend fun <T> Promise<T>.awaitDebug(name: String): T {
	//console.log("awaitDebug[$name]:before:this", this)
	try {
		val result = this.await2()
		//console.log("awaitDebug[$name]:after:result", result)
		return result
	} catch (e: Throwable) {
		//console.log("awaitDebug[$name]:after:error", e)
		e.printStackTrace()
		throw e
	}
}

public suspend fun <T> Promise<T>.await2(): T = suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
	this@await2.then(
			onFulfilled = { cont.resume(it) },
			onRejected = { cont.resumeWithException(RuntimeException("$it")) })
}


//suspend fun awaitDynamic(value: dynamic): dynamic = value

/*
suspend fun awaitDynamic(value: dynamic): dynamic = when {
	value is Promise<*> -> value.unsafeCast<Promise<*>>().await()
	value?.then != null -> value.unsafeCast<Promise<*>>().await()
	else -> value
}
*/

@JsName("AdmobJs")
external interface AdmobJsInterface {
	val banner: AdmobJsBanner
	val interstitial: AdmobJsInterstitialOrVideo
	val rewardvideo: AdmobJsInterstitialOrVideo
}

external interface AdmobJsBanner {
	fun config(data: dynamic): Any
	fun prepare(): Promise<String>
	fun show(): Promise<String>
	fun hide(): Promise<String>
	fun remove(): Promise<String>
}

external interface AdmobJsInterstitialOrVideo {
	fun config(data: dynamic): Any
	fun prepare(): Promise<String>
	fun show(): Promise<String>
	fun hide(): Promise<String>
	fun isReady(): Promise<Boolean>
}

private fun Admob.Config.convert(testing: Boolean): dynamic {
	val config = this
	return jsObject(*LinkedHashMap<String, Any?>().apply {
		this["id"] = config.id
		this["isTesting"] = testing
		this["autoShow"] = false
		if (config.forChild != null) this["forChild"] = config.forChild
		this["overlap"] = config.overlap
		this["bannerAtTop"] = config.bannerAtTop
		this["offsetTopBar"] = config.offsetTopBar
		this["size"] = config.size.name
	}.entries.map { it.key to it.value }.toTypedArray())
}
