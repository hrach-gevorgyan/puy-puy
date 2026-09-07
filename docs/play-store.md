# If this ever goes to Google Play

Distribution today is the APK on the Releases page, and **Play is not decided**. This is what
would have to be true first, extracted from a full pre-ship audit of an earlier version of the
app. Everything here is about publishing, not about the code.

| Item | Where | State |
|---|---|---|
| **targetSdk** | `app/build.gradle.kts` | `35`. The step to API 36 has passed, so an upload is a hard rejection. Bump `compileSdk`/`targetSdk` and AGP with it. API 36 also enforces edge-to-edge with no opt-out, so check the insets on every screen after. |
| **App Bundle** | `.github/workflows/release.yml` | Missing — only `assembleRelease` runs. Play has required `.aab` for new apps since 2021. Add `bundleRelease` and keep the APK for the GitHub sideload channel. |
| **Gradle signing** | `app/build.gradle.kts` | No `signingConfigs` block. `apksigner` cannot sign an AAB, so signing has to move into Gradle, and the app has to be enrolled in Play App Signing. |
| **versionCode** | `app/build.gradle.kts` | Still `1`. Fine for a first upload; needs a bump discipline after. |
| **Privacy policy** | Nowhere | Mandatory and non-waivable for a child-audience app, whatever it collects. One page is enough: no data collected, no network access, no permissions, no analytics, no ads, no crash reporting, drawings and audio never leave the device, plus a contact address. GitHub Pages off this repo will do. |
| **Target audience declaration** | Play Console | An under-13 band, which pulls in the Families requirements — both trivially satisfied here, since there are no ads and no third-party SDKs. |
| **Content rating** | Play Console | Questionnaire not started. |
| **Data safety form** | Play Console | "No data collected or shared" is accurate and defensible: no `INTERNET` permission, and emoji2's Play-Services initializer is removed from the merged manifest. |
| **Audio licensing** | `audio.md`, `licenses/` | The Piper Armenian voice is trained on a GPL-2.0 dataset and this repo is MIT. Resolve it once and write it down — either a statement of why generated audio is not derivative, or regenerate through the Azure `hy-AM-AnahitNeural` path that `tools/gen_voices.py` already supports. |
