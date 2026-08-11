# Pay-Carwash — BP Carwash for Quest QT850

Premium BP-branded 4-button carwash retail app for the
[Quest QT850 smart payment terminal](https://www.questpaymentsystems.com/qt850-smart-payment-terminal.html).

## Product

Four wash tiers, fixed price points:

| Tier          | Price | Description                  |
|---------------|-------|------------------------------|
| Quick Wash    | $10   | Rinse & dry                  |
| Express Wash  | $20   | Wash, wax & dry              |
| Deluxe Wash   | $30   | Triple foam, wax & dry       |
| Ultimate Wash | $40   | Full detail shine & protect  |

Flow: select wash → present card → approved → the app pulses the wash-bay
controller to unlock the wash (30s idle timeout back to menu). Kiosk posture: portrait-locked, fullscreen, screen
kept on, back button swallowed, `lockTaskMode=if_whitelisted` for MDM
pinning, and the activity registers as HOME so a kiosk policy can make it
the default launcher.

## Demo

Recorded on the `qt850` emulator (480×640, the terminal's panel size) —
two purchases end to end: Deluxe $30 and Ultimate $40, each approving and
unlocking the wash.

![BP Carwash demo](docs/demo.gif)

Higher-quality [MP4 version](docs/demo.mp4).

## Target hardware (QT850 spec sheet v10)

- Android 9.0 (Pie) → `minSdk 28`
- 3.5" IPS, **480×640 portrait** — UI is sized for ~320×427dp
- 1 GB RAM / 8 GB flash → plain Views + Kotlin, no Compose
- Payments: EMV chip, NFC contactless, MSR — driven by Quest's payment app

## Build

Requires JDK 17 and the Android SDK. Copy `local.properties.example` to
`local.properties` and point `sdk.dir` at your SDK (machine-local, not
committed).

```sh
./gradlew assembleDebug      # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease
```

## Tests

Two layers; both must pass before any release.

**Unit tests** (host JVM, fast — run on every change):

```sh
./gradlew testDebugUnitTest
```

- `WashTierTest` — pins the retail catalogue: 4 tiers, $10/$20/$30/$40 in
  cents, ascending/distinct, whole dollars, labels, only Ultimate featured.
  A failure here means the money changed.
- `SimulatedPaymentProviderTest` — approval contract and receipt refs.
- `QuestPaymentProviderTest` — guard rail: the unconfigured Quest provider
  must fail fast so it can never silently drop sales.

**Instrumented regression suite** (Espresso, runs on emulator or terminal):

```sh
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
```

- `MenuScreenTest` — all tiers, prices, and descriptions visible.
- `ComponentTest` — component-level GUI checks per screen: header (Helios
  logo, wordmark, prompt), footer tagline, each tier card is a clickable
  unit with price/name/description, only Ultimate carries the featured
  styling, price renders larger than and above the name, processing screen
  (logo, spinner, amount, tier, cancel), and approved/declined result
  components.
- `PurchaseFlowTest` — the retail flow end to end with fake providers
  injected via `PaymentGateway`:
  - approval charges exactly the tapped tier's price, pulses
    `WashBayController` with the right tier/receipt, returns to menu
  - every tier charges its own price
  - decline shows the reason and does **not** pulse (wash stays locked)
  - cancel mid-payment returns to menu without pulsing
  - double-tap protection while a payment is in flight

HTML reports land in `app/build/reports/`.

## Payment integration status

`PaymentProvider` abstracts the payment stack:

- `SimulatedPaymentProvider` — **active**. Approves after 2.5s so the full
  flow works on any Android device/emulator today.
- `QuestPaymentProvider` — stub. The QT850's card readers are driven by
  Quest's own payment application; custom apps hand off the sale amount and
  receive the result (on-terminal intent contract, or the Cloud EFTPOS API).
  This requires merchant onboarding with Quest: integration pack, fleet
  registration, and app deployment via Quest's MAM.
  Contact Quest: +61 3 8807 4400 / info@questpaymentsystems.com.

Swap the provider in `PaymentGateway.provider` once credentials exist.

## Deploying to the terminal

1. **Development**: enable ADB on the terminal (Quest supervisor menu),
   then `adb connect <terminal-ip>` over WiFi and
   `adb install -r app-debug.apk`. Dev access on a live payment terminal is
   gated by Quest — request a developer-unlocked unit.
2. **Production**: QT850 fleets are PCI PTS devices; APKs must be submitted
   to Quest for signing and are pushed to terminals via Quest's mobile app
   management (MAM). Release builds here are debug-signed until Quest issues
   the production signing process.

## Notes

- `WashBayController.pulse()` is the wash unlock integration point; wire
  it to the site's bay hardware (RS232 / network I/O module to the bay PLC —
  pulse count typically encodes the tier) for real fulfilment.
- Branding uses the BP Helios logo (vector, converted from
  BPositive `flutter_ui/assets/bp_logo.svg`) and BP palette colours. Ensure
  BP franchise brand approval before production rollout.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for project layout, branch/PR
workflow, emulator setup, and conventions. CI (GitHub Actions) runs unit
tests, lint, and a debug build on every push and pull request.
