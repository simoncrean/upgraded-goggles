# Contributing

## Prerequisites

- JDK 17
- Android SDK with platform 34 and build-tools 34 (path in `local.properties`,
  which is machine-local and not committed — copy `local.properties.example`)

## Project layout

```
app/src/main/java/com/bp/carwash/
  MainActivity.kt          # single-activity kiosk UI (menu / processing / result)
  WashTier.kt              # retail catalogue — tiers and prices (cents)
  WashBayController.kt     # wash unlock pulse (site hardware integration point)
  payment/
    PaymentProvider.kt     # payment abstraction + result types
    PaymentGateway.kt      # provider selection (swap point for Quest)
    SimulatedPaymentProvider.kt
    QuestPaymentProvider.kt  # fail-fast stub until Quest onboarding completes
app/src/test/              # JVM unit tests (catalogue, providers)
app/src/androidTest/       # Espresso: components + end-to-end flows
docs/                      # demo recordings, assets
```

## Workflow

1. Branch from `main`: `feature/<short-name>` or `fix/<short-name>`.
2. Make the change, keeping the catalogue (`WashTier`) and any money maths
   covered by unit tests.
3. Run the checks (below) before opening a PR. CI runs the same on every PR.
4. PRs need a green build; squash-merge to keep history linear.

## Checks

```sh
./gradlew testDebugUnitTest lintDebug     # fast, no device needed — CI gate
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest   # full GUI suite
```

The instrumented suite expects the `qt850` AVD (480×640 @ 240dpi — the
terminal's panel). Create it once:

```sh
sdkmanager "emulator" "system-images;android-34;google_apis;arm64-v8a"
avdmanager create avd -n qt850 -k "system-images;android-34;google_apis;arm64-v8a"
# then set hw.lcd.width=480, hw.lcd.height=640, hw.lcd.density=240,
# hw.ramSize=1024 in ~/.android/avd/qt850.avd/config.ini
emulator -avd qt850 &
```

## Conventions

- Money is `Long` cents, never floats.
- Payment behaviour goes behind `PaymentProvider`; tests inject fakes via
  `PaymentGateway.provider`.
- UI is plain Views/XML sized for ~320×427dp — no Compose (1 GB RAM target).
- Strings live in `strings.xml`; no hardcoded user-facing text in Kotlin.
- New screens/components get Espresso coverage in `ComponentTest`, new
  behaviour gets a flow test in `PurchaseFlowTest`.
