# Kumpel ⛏️

*Glück auf!* A small mining golem that keeps you company underground.
Your Kumpel follows you, picks up loot, senses ores around you and grows stronger the more you work together.

**Minecraft 26.3 · Fabric Loader ≥ 0.19.5 · Fabric API · Java 25**

![All five Kumpel levels: netherite, diamond, gold, iron and copper](../.github/media/kumpel/tiers.jpg)

## Getting started

Craft a **Kumpel Core** and use it on a block, and your own Kumpel appears.

```
 . A .      A = Amethyst Shard
 C R C      C = Copper Ingot
 C C C      R = Redstone Dust
```

A Kumpel spawned another way (e.g. `/summon kumpel:kumpel`) can be tamed with a copper ingot.

## What your Kumpel does

| | |
|---|---|
| **Follows you** | Keeps close and teleports to you if it falls behind. |
| **Collects loot** | Picks up dropped items near you (up to 9 stacks) and brings them to you. Items *you* throw away are left alone. |
| **Senses ores** | Every few seconds it scans the area around it. When it finds ore it points at it, rings a chime and tells you what it found, how far away it is and in which direction. |
| **Levels up** | Collecting items, finding ores and being fed earns XP. |

![A Kumpel pointing at buried diamond ore](../.github/media/kumpel/ore-sensing.jpg)

## Controls

| Action | Effect |
|---|---|
| Right-click (empty hand) | Sit / follow |
| Sneak + right-click (empty hand) | Show status; it also hands over what it carries |
| Right-click with a copper ingot | Repair 5 health (at full health it is eaten for XP) |
| Right-click with ores, metals or gems | Feed it for XP (coal 1 … diamond 40, netherite ingot 300) |
| Right-click with a compass | Turn ore sensing on/off |

## Levels

| Level | Look | XP | Health | Sense radius | Can sense |
|---|---|---|---|---|---|
| 1 | Copper | 0 | 20 | 8 | coal, copper |
| 2 | Iron | 60 | 30 | 11 | + quartz, iron, redstone |
| 3 | Gold | 180 | 40 | 14 | + lapis, gold |
| 4 | Diamond | 400 | 50 | 17 | + emerald, diamond |
| 5 | Netherite | 800 | 60 | 20 | + ancient debris, fire immune |

Ores are detected through the conventional `c:ores/*` tags, so ores from other mods are found too.

## Building

```sh
cd kumpel
./gradlew build
```

The jar ends up in `kumpel/build/libs/`. Every push also builds the mod on GitHub Actions. You can download the jar from the run's **Artifacts**.

### Tests

- **Server game tests** (`src/gametest`) run as part of `./gradlew build`. They cover picking up items, ore sensing per level, levelling up and handing items to the owner.
- **Client game test** (`./gradlew runClientGameTest`) starts a real client and takes screenshots of every level, the sitting pose and a Kumpel sensing buried diamond ore. On CI the screenshots are uploaded as an artifact.
