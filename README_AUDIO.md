# ESCAPE SHIFT V14 — Sound system

Built on V13: 10 levels, parallax background, animation, monsters, music.

## Sound repository (same filenames = replace without touching Kotlin)

Location: `app/src/main/res/raw/`

| File | Trigger |
| --- | --- |
| sfx_jump.wav | Jump |
| sfx_land.wav | Land |
| sfx_shift.wav | SHIFT toggle |
| sfx_laser.wav | Laser nearby (continuous, distance-based) |
| sfx_saw.wav | Saw nearby (continuous, distance-based) |
| sfx_monster.wav | Monster nearby / stomp effect |
| sfx_conveyor.wav | Conveyor nearby (continuous, distance-based) |
| sfx_crate.wav | Crate pushed (rate-limited) |
| sfx_spring.wav | Spring launch |
| sfx_coin.wav | Coin collected |
| sfx_key.wav | Keycard collected |
| sfx_door.wav | Exit entered |
| sfx_victory.wav | Level completed |
| sfx_death.wav | Death |
| ambient_loop.mp3 | Existing background music (unchanged) |

All 14 WAV files were synthesized as ORIGINAL temporary sounds. To replace: export mono/stereo PCM WAV with the exact filename and replace the file. Android Studio: Build > Rebuild Project. No Kotlin edits needed. If you choose .ogg or .mp3 instead, rename the resource to the same stem (e.g. sfx_laser.ogg); Android R.raw.sfx_laser resolves the same way, but do not keep multiple extensions with the same stem simultaneously.

## Spatial sound
GameAudio.kt loads short sounds through SoundPool once. At most one active loop for each type (laser/saw/monster/conveyor), with the loudness based on the nearest source distance to the hero. The audibility radius and volume are centralized in GameAudio.kt:
- HEARING_DISTANCE = 480 virtual units
- MAX_AMBIENT_VOLUME = 0.32
- MAX_EFFECT_VOLUME = 0.55
- squared falloff; no audible loop if out of range

Audio pauses when the app is backgrounded, and is released when the Activity is destroyed. The existing ambient music is managed separately by GameMusic.kt.

## Run / test
Open this folder in Android Studio, then Build > Rebuild Project > Run on Xiaomi 12.
The virtual player still finishes 10/10 levels in the desktop Kotlin host smoke test. Audio API compile was validated with Android-shaped stubs, but native APK audio playback still needs confirmation on a physical Android device.

## GitHub
Use this project as a private repo. `.gitignore` prevents committing keystore, local.properties, Gradle build outputs and signed APK/AAB. Never commit Play Console passwords or signing keys.
