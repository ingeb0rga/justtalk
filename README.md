<p align="center">
  <img src="docs/logo.svg" width="128" height="128" alt="JustTalk Logo">
</p>

# JustTalk (fork)

Push-to-talk dictation for Android.

Fork of [kafkasl/phone-whisper](https://github.com/kafkasl/phone-whisper) (Apache-2.0). Focus of this fork:

- **Russian and multilingual recognition** (Parakeet TDT v3, Qwen3-ASR, GigaAM) with native punctuation
- **Local-only transcription** by default (cloud/OpenAI mode is being phased out)
- **Bundled native libs**: `libsherpa-onnx-jni.so` + `libonnxruntime.so` (arm64-v8a, sherpa-onnx v1.13.8) are in the repo — builds work out of the box
- Reliability: recording limits, cancellation, better error reporting

Tap the floating button, speak, tap again, and your text is inserted into the currently focused text field. Not a keyboard/IME — uses the Accessibility Service only for cross-app text insertion (and never acts without a tap).

## Install

### Download the APK

Grab the latest APK from [GitHub Releases](https://github.com/ingeb0rga/justtalk/releases).

> **Google Play Protect** may warn about unknown apps during install — tap *Install anyway* (the app has no backend, no analytics; see [PRIVACY.md](PRIVACY.md)).

Open it on your phone, install it, then launch the app once to finish setup.

### Build from source

Requires JDK 17 and Android SDK (platform 34).

```bash
git clone https://github.com/ingeb0rga/justtalk.git && cd phone-whisper
make build
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

If you use ADB:

```bash
make adb-install
```

## How it works

1. A small overlay button floats on screen
2. Tap once to start recording
3. Tap again to stop
4. Audio is transcribed locally on-device
5. The text is inserted into the focused text field
6. If insertion fails, the text is copied to the clipboard

## Setup

1. Open **JustTalk**
2. Grant the **audio recording** permission
3. Enable the **Accessibility Service**
4. Download a model in the app (see catalog below)

## Why does it need Accessibility?

JustTalk uses Android Accessibility Service for one narrow reason: to insert dictated text into the currently focused text field across apps.

It does **not** replace your keyboard. It does **not** run background automation. It only acts after you explicitly tap the overlay button.

## Privacy

**Local mode: audio never leaves the device.** No backend, no analytics, no telemetry.

Full policy: [PRIVACY.md](PRIVACY.md)

## Local models

Models are stored in app storage under `/data/data/com.spirt.justtalk/files/models/`.

Catalog (sherpa-onnx release archives, downloaded and extracted by the app):

| Model | Size | Notes |
|---|---:|---|
| Parakeet 110M (en) | 100 MB | Best small default |
| Whisper Base (en) | 199 MB | Solid baseline |
| Parakeet 0.6B v3 (multilingual) | 465 MB | ru + 25 EU languages, native punctuation |
| Moonshine Tiny (en) | 103 MB | Fastest |
| Qwen3-ASR 0.6B (multilingual) | 838 MB | Max quality, native punctuation |
| GigaAM v2 RNN-T (ru) | 164 MB | Russian-specific, no punctuation |

## Development

```bash
make build       # build debug APK
make test        # run unit tests
make adb-install # build + install via ADB
make clean       # clean build artifacts
```

sherpa-onnx vendored Kotlin API and native libs are synced to **v1.13.8** (one tag for both — do not mix versions).

## App compatibility

Works best in apps that use standard Android text fields. Some apps use custom text surfaces or terminal-style views, which may not support direct accessibility paste — in that case the transcript is copied to the clipboard.

### Termux

Termux's main terminal area is not a standard Android text field, so direct insertion may not work there.

1. Focus Termux
2. Swipe the extra keys row (`ESC`, `CTRL`, `ALT`, arrows, etc.) left or right
3. Switch to Termux's native text input box
4. Dictate there

## Current limitations

- Accessibility permission is required for cross-app insertion
- Some apps may block paste or text injection
- Local models are large (up to ~840 MB for the largest)

## License

Apache-2.0. See [LICENSE](LICENSE).
