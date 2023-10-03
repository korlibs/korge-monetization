import korlibs.korge.gradle.*

plugins {
    alias(libs.plugins.korge)
}

korge {
    id = "org.korge.samples.mymodule"

// To enable all targets at once

    //targetAll()

// To enable targets based on properties/environment variables
    //targetDefault()

// To selectively enable targets

    targetJvm()
    targetJs()
    targetIos()
    targetAndroid()
    serializationJson()

    androidPermission("INTERNET")
    androidManifestApplicationChunk(
        "<meta-data android:name=\"com.google.android.gms.ads.APPLICATION_ID\" android:value=\"ca-app-pub-3395905965441916~3606887124\" />"
    )
}

dependencies {
    add("commonMainApi", project(":deps"))
}

