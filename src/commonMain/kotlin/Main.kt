import korlibs.io.async.launchImmediately
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.admob.Admob
import korlibs.korge.admob.admob
import korlibs.korge.admob.configureAdmob
import korlibs.korge.input.onClick
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.SContainer
import korlibs.korge.view.text
import korlibs.math.geom.Size
import kotlinx.coroutines.flow.consumeAsFlow

suspend fun main() = Korge(virtualSize = Size(512, 512), displayMode = KorgeDisplayMode.TOP_LEFT_NO_CLIP) {
//suspend fun main() = Korge() {
    views.configureAdmob(testing = true)

    sceneContainer().changeTo({ MainMyModuleScene() })
}

class MainMyModuleScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val stack = uiVerticalStack(adjustSize = false) {
            val outputText = text("Monetization example")

            uiButton("Open interstitial") {
                onClick {
                    outputText.text = "INTERSTITIAL RESULT: " + (views.admob.interstitialPrepareWaitAndShow(
                        Admob.Config(
                            id = "ca-app-pub-3395905965441916/9312372956"
                        )
                    ))
                }
            }

            uiButton("Open reward video") {
                onClick {
                    views.admob.rewardvideoPrepareWaitAndShow(
                        Admob.Config(
                            id = "ca-app-pub-3395905965441916/9312372956"
                        )
                    )
                }
            }
        }

        launchImmediately {
            for (e in views.admob.bannerEvents()) {
                println("BANNER_EVENT: $e")
                stack.text("BANNER_EVENT: $e")
            }
        }

        launchImmediately {
            for (e in views.admob.interstitialEvents()) {
                println("INTERSTITAL_EVENT: $e")
                stack.text("INTERSTITAL_EVENT: $e")
            }
        }

        launchImmediately {
            for (e in views.admob.rewardvideoEvents()) {
                println("REWARD_VIDEO_EVENT: $e")
                stack.text("REWARD_VIDEO_EVENT: $e")
            }
        }

        views.admob.bannerPrepareAndShow(
            Admob.Config(
                id = "ca-app-pub-3395905965441916/9312372956"
            )
        )
    }
}