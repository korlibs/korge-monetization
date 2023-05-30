package korlibs.korge.admob

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.reward.*
import korlibs.io.async.*
import korlibs.io.lang.CancellableGroup
import korlibs.io.lang.close
import korlibs.korge.view.Views
import korlibs.render.gameWindowAndroidContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

fun Activity.prepareRootRelativeLayout(): RelativeLayout {
	val activity = this
	val initialRootView = activity.window.decorView.findViewById<android.view.View>(android.R.id.content).let {
		if (it is FrameLayout) it.getChildAt(0) else it
	}
	val rootView = initialRootView as? RelativeLayout? ?: RelativeLayout(activity).also { newRelativeLayout ->
		runOnUiThread {
			initialRootView.removeFromParent()
			activity.setContentView(newRelativeLayout)
			newRelativeLayout.addView(initialRootView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
		}
	}
	return rootView
}

actual fun AdmobCreate(views: Views, testing: Boolean): Admob {
	val activity = views.gameWindow.gameWindowAndroidContext as Activity
	val rootRelativeLayout = activity.prepareRootRelativeLayout()
	return AdmobAndroid(views, activity, testing, rootRelativeLayout)
}

private fun runAndroidOnUiThread(context: Context? = null, block: () -> Unit) {
	//Looper.prepare()
	Handler(context?.mainLooper ?: Looper.getMainLooper()).post(block)
}

@SuppressLint("MissingPermission")
private class AdmobAndroid(
	views: Views,
	val activity: Activity,
	val testing: Boolean,
	val rootView: RelativeLayout
) : Admob(views) {

	interface BaseListener {
		val views: Views
		fun <T> Signal<T>.sendInContext(value: T) {
			val signal = this
			views.launchImmediately {
				signal.invoke(value)
			}
		}
	}

	class SignalRewardedVideoListener(override val views: Views) : BaseListener, RewardedVideoAdListener {
		val onRewardedVideoAdClosed = Signal<Unit>()
		val onRewardedVideoAdLeftApplication = Signal<Unit>()
		val onRewardedVideoAdLoaded = Signal<Unit>()
		val onRewardedVideoAdOpened = Signal<Unit>()
		val onRewardedVideoCompleted = Signal<Unit>()
		val onRewarded = Signal<Reward>()
		val onRewardedVideoStarted = Signal<Unit>()
		val onRewardedVideoAdFailedToLoad = Signal<Int>()

		override fun onRewardedVideoAdClosed() = onRewardedVideoAdClosed.sendInContext(Unit)
		override fun onRewardedVideoAdLeftApplication() = onRewardedVideoAdLeftApplication.sendInContext(Unit)
		override fun onRewardedVideoAdLoaded() = onRewardedVideoAdLoaded.sendInContext(Unit)
		override fun onRewardedVideoAdOpened() = onRewardedVideoAdOpened.sendInContext(Unit)
		override fun onRewardedVideoCompleted() = onRewardedVideoCompleted.sendInContext(Unit)
		override fun onRewardedVideoStarted() = onRewardedVideoStarted.sendInContext(Unit)
		override fun onRewarded(rewardItem: RewardItem) = onRewarded.sendInContext(rewardItem.let { Admob.Reward(it.type, it.amount) })
		override fun onRewardedVideoAdFailedToLoad(p0: Int) = onRewardedVideoAdFailedToLoad.sendInContext(p0)

		fun createChannel(): ReceiveChannel<RewardvideoEvent> {
			val channel = Channel<RewardvideoEvent>()
			val group = CancellableGroup()
			group += onRewardedVideoAdClosed { channel.trySend(RewardvideoEvent.REWARDED_VIDEO_AD_CLOSED) }
			group += onRewardedVideoAdLeftApplication { channel.trySend(RewardvideoEvent.REWARDED_VIDEO_AD_LEFT_APPLICATION) }
			group += onRewardedVideoAdLoaded { channel.trySend(RewardvideoEvent.REWARDED_VIDEO_AD_LOADED) }
			group += onRewardedVideoAdOpened { channel.trySend(RewardvideoEvent.REWARDED_VIDEO_AD_OPENED) }
			group += onRewardedVideoCompleted { channel.trySend(RewardvideoEvent.REWARDED_VIDEO_COMPLETED) }
			group += onRewarded { channel.trySend(RewardvideoEvent.REWARDED(it)) }
			group += onRewardedVideoStarted { channel.trySend(RewardvideoEvent.REWARDED_VIDEO_STARTED) }
			group += onRewardedVideoAdFailedToLoad { channel.trySend(RewardvideoEvent.REWARDED_VIDEO_AD_FAILED_TO_LOAD) }
			channel.invokeOnClose {
				group.close()
			}
			return channel
		}
	}

	class SignalAdListener(override val views: Views) : BaseListener, AdListener() {
		val onInterstitialAdImpression = Signal<Unit>()
		val onInterstitialAdLeftApplication = Signal<Unit>()
		val onAdClicked = Signal<Unit>()
		val onAdFailedToLoad = Signal<Int>()
		val onAdClosed = Signal<Unit>()
		val onAdOpened = Signal<Unit>()
		val onAdLoaded = Signal<Unit>()

		override fun onAdImpression() = onInterstitialAdImpression.sendInContext(Unit)
		override fun onAdLeftApplication() = onInterstitialAdLeftApplication.sendInContext(Unit)
		override fun onAdClicked() = onAdClicked.sendInContext(Unit)
		override fun onAdFailedToLoad(p0: Int) = onAdFailedToLoad.sendInContext(p0)
		override fun onAdClosed() = onAdClosed.sendInContext(Unit)
		override fun onAdOpened() = onAdOpened.sendInContext(Unit)
		override fun onAdLoaded() = onAdLoaded.sendInContext(Unit)

		fun createChannel(): ReceiveChannel<AdEvent> {
			val channel = Channel<AdEvent>()
			val group = CancellableGroup()
			group += onInterstitialAdImpression { channel.trySend(InterstitialEvent.INTERSTITIAL_AD_IMPRESSION) }
			group += onInterstitialAdLeftApplication { channel.trySend(InterstitialEvent.INTERSTITIAL_AD_LEFT_APPLICATION) }
			group += onAdClicked { channel.trySend(AdEvent.AD_CLICKED) }
			group += onAdFailedToLoad { channel.trySend(AdEvent.AD_FAILED_TO_LOAD(it)) }
			group += onAdClosed { channel.trySend(AdEvent.AD_CLOSED) }
			group += onAdOpened { channel.trySend(AdEvent.AD_OPENED) }
			group += onAdLoaded { channel.trySend(AdEvent.AD_LOADED) }
			channel.invokeOnClose { group.close() }
			return channel
		}
	}


	val interstitialListener = SignalAdListener(views)
	val interstitial by lazy { InterstitialAd(activity).apply {
		adListener = interstitialListener
	} }

	val rewardVideoListener = SignalRewardedVideoListener(views)
	val rewardVideo by lazy { MobileAds.getRewardedVideoAdInstance(activity).apply {
		rewardedVideoAdListener = rewardVideoListener
	} }

	val adViewListener = SignalAdListener(views)
	private var adView = AdView(activity).apply {
		adListener = adViewListener
	}
	private var bannerAtTop = false

	fun Admob.Config.toAdRequest(): AdRequest {
		val builder = AdRequest.Builder()
		if (this.keywords != null) {
			for (keyword in keywords) builder.addKeyword(keyword)
		}
		if (this.forChild != null) builder.tagForChildDirectedTreatment(forChild)
		return builder.build()
	}

	override suspend fun available() = true

	override suspend fun bannerPrepare(config: Admob.Config) {
		this.bannerAtTop = config.bannerAtTop
		runOnUiThread {
			adView.adUnitId = if (testing) "ca-app-pub-3940256099942544/6300978111" else config.id
			adView.adSize = when (config.size) {
				Size.BANNER -> AdSize.BANNER
				Size.IAB_BANNER -> AdSize.BANNER
				Size.IAB_LEADERBOARD -> AdSize.BANNER
				Size.IAB_MRECT -> AdSize.BANNER
				Size.LARGE_BANNER -> AdSize.LARGE_BANNER
				Size.SMART_BANNER -> AdSize.SMART_BANNER
				Size.FLUID -> AdSize.FLUID
			}
			adView.loadAd(config.toAdRequest())
		}
	}

	override suspend fun bannerShow() {
		runOnUiThread {
			if (adView.parent != rootView) {
				adView.removeFromParent()
				rootView.addView(
					adView,
					RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
						addRule(RelativeLayout.CENTER_HORIZONTAL)
						addRule(if (bannerAtTop) RelativeLayout.ALIGN_PARENT_TOP else RelativeLayout.ALIGN_PARENT_BOTTOM)
					}
				)
			}
			adView.visibility = View.VISIBLE
		}
		//activity.view
	}

	override suspend fun bannerHide() {
		runOnUiThread {
			adView.visibility = View.INVISIBLE
		}
	}

	override suspend fun interstitialPrepare(config: Admob.Config) {
		runOnUiThread {
			val adUnitId = if (testing) "ca-app-pub-3940256099942544/1033173712" else config.id
			if (interstitial.adUnitId != adUnitId) {
				interstitial.adUnitId = adUnitId
			}
			interstitial.loadAd(config.toAdRequest())
		}
	}

	override suspend fun interstitialIsLoaded(): Boolean = runOnUiThread { interstitial.isLoaded }

	override suspend fun interstitialShowAndWait(): Boolean {
		val closeables = CancellableGroup()
		try {
			val result = mapOf(
				interstitialListener.onAdClosed to false,
				interstitialListener.onAdClicked to true
			).executeAndWaitAnySignal {
				runOnUiThread {
					interstitial.show()
				}
			}
			return result
		} finally {
			closeables.close()
		}
	}

	override suspend fun rewardvideoPrepare(config: Admob.Config) {
		runOnUiThread {
			rewardVideo.userId = config.userId
			if (config.immersiveMode != null) {
				rewardVideo.setImmersiveMode(config.immersiveMode)
			}
			rewardVideo.loadAd(
				if (testing) "ca-app-pub-3940256099942544/5224354917" else config.id,
				config.toAdRequest()
			)
		}
	}

	override suspend fun rewardvideoIsLoaded(): Boolean = runOnUiThread { rewardVideo.isLoaded }

	override suspend fun rewardvideoShowAndWait(): Boolean {
		rewardVideoListener.onRewardedVideoAdClosed.executeAndWaitSignal {
			runOnUiThread {
				rewardVideo.show()
			}
		}
		return true
	}

	override suspend fun bannerEvents(): ReceiveChannel<AdEvent> = adViewListener.createChannel()
	override suspend fun rewardvideoEvents(): ReceiveChannel<RewardvideoEvent> = rewardVideoListener.createChannel()
	override suspend fun interstitialEvents(): ReceiveChannel<AdEvent> = interstitialListener.createChannel()


	private suspend fun <T> runOnUiThread(block: () -> T): T {
		val result = CompletableDeferred<T>()
		activity.runOnUiThread {
			result.complete(block())
		}
		return result.await()
	}
}

private suspend inline fun <T> Map<Signal<Unit>, T>.executeAndWaitAnySignal(callback: () -> Unit): T {
	val deferred = CompletableDeferred<T>()
	val closeables = this.map { pair -> pair.key.once { deferred.complete(pair.value) } }
	try {
		callback()
		return deferred.await()
	} finally {
		closeables.close()
	}
}

private suspend inline fun <T> Iterable<Signal<T>>.executeAndWaitAnySignal(callback: () -> Unit): Pair<Signal<T>, T> {
	val deferred = CompletableDeferred<Pair<Signal<T>, T>>()
	val closeables = this.map { signal -> signal.once { deferred.complete(signal to it) } }
	try {
		callback()
		return deferred.await()
	} finally {
		closeables.close()
	}
}

private suspend inline fun <T> Signal<T>.executeAndWaitSignal(callback: () -> Unit): T {
	val deferred = CompletableDeferred<T>()
	val closeable = this.once { deferred.complete(it) }
	try {
		callback()
		return deferred.await()
	} finally {
		closeable.close()
	}
}

private fun View.removeFromParent() {
	(parent as? ViewGroup?)?.removeView(this)
}

