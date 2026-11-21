# VStrike

Client-side Fabric mod for Minecraft 1.20.1 (Fabric Loader, Fabric API, Java 17).
No HUD, no GUI, no overlays, no on-screen elements of any kind.

VStrike makes two changes to how the client processes your own attacks. Both are about
client-side delay between a mouse click and what the client does with it. Neither makes the
server see or process anything faster, and neither changes how many attacks you can make.

## What it changes

### 1. Early entity-attack dispatch (`earlyAttackDispatch`)

Vanilla reads the attack key only in `MinecraftClient.handleInputEvents()`, which runs once per
client tick (20 Hz). The mouse event itself is handled earlier, on the next frame. A click landing
just after a tick boundary waits up to 50 ms (mean about 25 ms) before `doAttack()` builds the
attack packet.

`Mouse.onMouseButton` is hooked at TAIL, after vanilla has registered the press. If the crosshair is
on an entity and every gate the tick path would apply is satisfied, the press is consumed with
`attackKey.wasPressed()` and `doAttack()` is called right away. The tick path then finds no pending
press, so there is never a double attack.

Left to vanilla on purpose: block targets, misses, the 10-tick miss lockout, any open screen or
overlay, item use (vanilla discards attack presses while using an item), and riding.

Measurable effect: click-to-`doAttack()` delay drops from 0-50 ms to roughly 0 ms. Set
`debugLatencyLog=true` and compare the mean written to `latest.log` with `earlyAttackDispatch` on and
off. The server still handles the packet on its own tick, so the benefit is that the packet leaves
the client earlier, not that it is processed earlier.

### 2. Swing start compensation (`swingStartCompensation`)

`LivingEntity.swingHand` starts a swing at `handSwingTicks = -1`. `tickHandSwing()` moves it to 0 on
the first tick (progress 0, no motion) and the arm only starts moving on the second tick, so the
first-person swing begins 50-100 ms after the click. For the local player only, the counter is
started at 0 instead.

Effect: the arm starts moving on the first tick after the click, one tick (50 ms) sooner, and the
swing ends one tick sooner. This is cosmetic. Swing duration, damage, packets and attack-strength
cooldown are untouched.

## What it deliberately does not do

- **Miss lockout.** After a miss in survival, vanilla sets `attackCooldown = 10`, which drops clicks
  for half a second. It is client-only, but removing it changes attack timing, so it is untouched.
- **Click buffering or queueing.** Re-firing a dropped click later is auto-clicking in effect.
- **Predicted hit feedback.** Hurt flash, knockback and damage numbers come from the server. Showing
  them before the server confirms would display hits that may be rejected (i-frames, cooldown).
  Locally predicted sounds and crit particles already happen in `PlayerEntity.attack` on the client.
- **Network or packet-handling changes.** Incoming packets are already applied on the main thread
  each frame, not once per tick. There is nothing measurable to gain.
- **Raycast / targeting changes.** Changing what the crosshair hits would change gameplay.
- **Re-equip dip animation.** It reflects attack-strength cooldown, which is information, so it stays.

## Config

`config/vstrike.properties`, created on first launch. Edit and restart; there is no in-game screen.

| Key | Default | Meaning |
| --- | --- | --- |
| `earlyAttackDispatch` | `true` | Dispatch entity attacks at mouse-event time |
| `swingStartCompensation` | `true` | Start local swing animation one tick sooner |
| `debugLatencyLog` | `false` | Log click to dispatch latency stats to `latest.log` (never on screen) |

Server note: vanilla attack packets are aligned to the 50 ms client tick. With `earlyAttackDispatch`
they are not. The count and ordering of packets are the same, but a strict anticheat that assumes
tick alignment could in theory notice. Turn the option off if a server objects.

## Building

Requires JDK 17+ and network access to `maven.fabricmc.net`, Mojang's servers and Maven Central.

```
./gradlew build
```

The mod jar is `build/libs/vstrike-1.0.0.jar` (the `-sources` jar is not the mod). The included
GitHub Actions workflow (`.github/workflows/build.yml`) builds the same jar and uploads it as an artifact.

## Verification status

This source was written without being compiled or run, because the environment it was authored in
could not reach the Fabric and Mojang servers needed to resolve Minecraft 1.20.1 and Yarn. The
targets below come from knowledge of the 1.20.1 Yarn-mapped code, not from decompiling it here.
On first build, check these; a mismatch fails fast at game launch (`defaultRequire: 1`):

- `Mouse.onMouseButton(long, int, int, int)` exists and runs `KeyBinding.onKeyPressed` before its end.
- `MinecraftClient.doAttack()` is `private boolean` and `attackCooldown` is a `private int` field.
- `KeyBinding.matchesMouse(int)`, `MinecraftClient.getOverlay()`, `isPaused()` and `Mouse.isCursorLocked()` exist.
- `LivingEntity.swingHand(Hand, boolean)` sets `handSwingTicks = -1`, and the fields are named
  `handSwinging` and `handSwingTicks`.

Manual test: on a server or LAN world, hit a mob with `debugLatencyLog=true`, then repeat with
`earlyAttackDispatch=false` and compare the logged means.
