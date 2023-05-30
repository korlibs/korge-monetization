package korlibs.korge.admob

import korlibs.datastructure.Extra
import korlibs.io.async.Signal
import korlibs.time.milliseconds
import korlibs.korge.view.Views
import korlibs.io.async.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.jvm.JvmOverloads

expect fun AdmobCreate(views: Views, testing: Boolean): Admob

var Views.admobTesting by Extra.PropertyThis { true }
fun Views.configureAdmob(testing: Boolean) { this.admobTesting = testing }
val Views.admob: Admob by Extra.PropertyThis { AdmobCreate(this, admobTesting) }

abstract class Admob(val views: Views) {
	enum class Size {
		BANNER, IAB_BANNER, IAB_LEADERBOARD, IAB_MRECT, LARGE_BANNER, SMART_BANNER, FLUID
	}

	data class Reward(val type: String, val amount: Int)
	data class Config @JvmOverloads constructor(
		val id: String,
		val userId: String? = null,
		val size: Size = Size.SMART_BANNER,
		val bannerAtTop: Boolean = false,
		val overlap: Boolean = true,
		val offsetTopBar: Boolean = false,
		val forChild: Boolean? = null,
		val keywords: List<String>? = null,
		val immersiveMode: Boolean? = null
	)

	open class AdEvent(val name: String) {
		override fun toString(): String = name

		data class AD_FAILED_TO_LOAD(val reason: Int) : InterstitialEvent("AD_FAILED_TO_LOAD")

		companion object {
			val AD_CLICKED = AdEvent("AD_CLICKED")
			val AD_CLOSED = AdEvent("AD_CLOSED")
			val AD_OPENED = AdEvent("AD_OPENED")
			val AD_LOADED = AdEvent("AD_LOADED")
		}
	}

	open class RewardvideoEvent(name: String) : AdEvent(name) {
		data class REWARDED(val reward: Admob.Reward) : RewardvideoEvent("REWARDED")

		companion object {
			val REWARDED_VIDEO_AD_CLOSED = RewardvideoEvent("REWARDED_VIDEO_AD_CLOSED")
			val REWARDED_VIDEO_AD_LEFT_APPLICATION = RewardvideoEvent("REWARDED_VIDEO_AD_LEFT_APPLICATION")
			val REWARDED_VIDEO_AD_LOADED = RewardvideoEvent("REWARDED_VIDEO_AD_LOADED")
			val REWARDED_VIDEO_AD_OPENED = RewardvideoEvent("REWARDED_VIDEO_AD_OPENED")
			val REWARDED_VIDEO_COMPLETED = RewardvideoEvent("REWARDED_VIDEO_COMPLETED")
			val REWARDED_VIDEO_STARTED = RewardvideoEvent("REWARDED_VIDEO_STARTED")
			val REWARDED_VIDEO_AD_FAILED_TO_LOAD = RewardvideoEvent("REWARDED_VIDEO_AD_FAILED_TO_LOAD")
		}
	}

	open class InterstitialEvent(name: String) : AdEvent(name) {
		companion object {
			val INTERSTITIAL_AD_IMPRESSION = InterstitialEvent("INTERSTITIAL_AD_IMPRESSION")
			val INTERSTITIAL_AD_LEFT_APPLICATION = InterstitialEvent("INTERSTITIAL_AD_LEFT_APPLICATION")
		}
	}


	open suspend fun available() = false

	open suspend fun bannerEvents(): ReceiveChannel<AdEvent> = Channel<InterstitialEvent>()
	open suspend fun bannerPrepare(config: Config) = Unit
	open suspend fun bannerShow() = Unit
	open suspend fun bannerHide() = Unit

	open suspend fun interstitialEvents(): ReceiveChannel<AdEvent> = Channel<InterstitialEvent>()
	open suspend fun interstitialPrepare(config: Config) = Unit
	open suspend fun interstitialIsLoaded(): Boolean = false
	open suspend fun interstitialShowAndWait(): Boolean = true

	open suspend fun rewardvideoEvents(): ReceiveChannel<RewardvideoEvent> = Channel<RewardvideoEvent>()
	open suspend fun rewardvideoPrepare(config: Config) = Unit
	open suspend fun rewardvideoIsLoaded(): Boolean = false
	open suspend fun rewardvideoShowAndWait(): Boolean = true

	suspend fun interstitialWaitLoaded() {
		while (!interstitialIsLoaded()) delay(100.milliseconds)
	}

	suspend fun rewardvideoWaitLoaded() {
		while (!rewardvideoIsLoaded()) delay(100.milliseconds)
	}

	// Utility methods

	suspend fun bannerPrepareAndShow(config: Config) {
		bannerPrepare(config)
		bannerShow()
	}

	suspend fun interstitialPrepareWaitAndShow(config: Config): Boolean {
		interstitialPrepare(config)
		interstitialWaitLoaded()
		return interstitialShowAndWait()
	}

	suspend fun rewardvideoPrepareWaitAndShow(config: Config): Boolean {
		rewardvideoPrepare(config)
		rewardvideoWaitLoaded()
		return rewardvideoShowAndWait()
	}
}
