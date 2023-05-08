package korlibs.korge.admob

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.view.*
import korlibs.math.geom.*

suspend fun AdmobCreateDefault(views: Views, testing: Boolean): Admob = object : Admob(views) {
    override suspend fun available(): Boolean = false
    override suspend fun bannerShow() {
        views.onAfterRender {
            it.batch.drawQuad(
                it.getTex(Bitmaps.white),
                x = 0f,
                y = 0f,
                //width = it.ag.mainRenderBuffer.width.toFloat(),
                width = it.ag.mainFrameBuffer.width.toFloat(),
                height = 86f,
                colorMul = Colors["#f0f0f0"],
                m = Matrix()
            )
        }
    }
}
