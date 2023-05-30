import korlibs.korge.*
import korlibs.korge.admob.*
import korlibs.korge.scene.*
import korlibs.korge.view.*

suspend fun main() = Korge {
    val admob = AdmobCreate(views, testing = true)
    admob.bannerPrepare(
        Admob.Config(
            id = "ca-app-pub-3395905965441916/9312372956"
        )
    )
    admob.bannerShow()

    admob.rewardvideolPrepare()
    sceneContainer().changeTo({ MainMyModuleScene() })
}

class MainMyModuleScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        text("Monetization example")
    }
}