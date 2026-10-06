# Changelog

All notable changes to Kumpel are listed here.

## 0.1.0

The first release.

### The Kumpel

- A small mining golem in five levels (copper, iron, gold, diamond, netherite) that follows you, collects items and brings them to you.
- Ore sensing: points at the most valuable ore it can sense and tells you where it is.
- A backpack (Kiepe) that grows with its level.
- Grubenlampe: places torches in dark places underground.
- Schlagwetter warning: makes nearby creepers glow and warns you about lava.
- Henkelmann: hands you food when you get hungry.
- Feierabend: sits down while you sleep.
- Hauer mode: mines exposed ores with a pickaxe you give it.
- Wünschelrute: from level 4 on, sensed ores glow through walls.
- Dances to jukeboxes and celebrates Barbaratag on 4 December.
- Kanarienvogel: carries a canary cage that warns about monsters and running out of air.
- Ore sensing skips chunk sections without matching ores, so it stays cheap even with many Kumpels.

### Items

- Kumpel Core: awakens a Kumpel, and packs it up again with all its experience.
- Cracked Kumpel Core: what a Kumpel leaves behind when it dies; repair it with a copper block.
- Steigerpfeife (Foreman's Whistle): calls your Kumpels, sends them on a break, and marks storage chests.
- Canary Cage for your Kumpel's shoulder.
- Grubenhelm (Miner's Helmet): lets you see in the dark.
- Kumpel spawn egg and an own creative tab.

### For modpacks

- `config/kumpel.json`: levels, ores, food and all behaviour can be changed; `/kumpel reload` applies it without a restart.
- Modded ores are found through `c:ores`, modded storage works through the Fabric Transfer API.
- JEI and REI integration.
- Eleven advancements.
- English and German translations.
