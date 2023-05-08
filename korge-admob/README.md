# KorGE Admob Support

<img src="./admob.svg" width="128" height="128" />

Adds support for <https://admob.google.com/>.

## Usage

In `build.gradle.kts`:

```kotlin
korge {
    androidPermission("INTERNET")
    androidManifestApplicationChunk(
        "<meta-data android:name=\"com.google.android.gms.ads.APPLICATION_ID\" android:value=\"ca-app-pub-3395905965441916~3606887124\" />"
    )
}
```

In your `fun main() = Korge {`:

```kotlin
val admob = AdmobCreate(views, testing = true)
admob.bannerPrepare(
    Admob.Config(
        id = "ca-app-pub-3395905965441916/9312372956"
    )
)
admob.bannerShow()
```
