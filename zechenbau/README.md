# Zechenbau 🏭

*Glück auf!* Building blocks of the Ruhr's collieries: red colliery bricks, the steel framework of Zeche Zollverein, girders, headframe lattice and miner's lamps. Build your own pithead, machine hall or workers' settlement.

A companion to [Kumpel](../kumpel/), but it works on its own.

**Minecraft 26.3 · Fabric Loader ≥ 0.19.5 · Fabric API · Java 25**

![A machine hall with steel-framed windows, girders and lamps, and a headframe behind it](../.github/media/zechenbau/zeche.jpg)

## Blocks

Everything is in its own **Zechenbau** creative tab and in *Building Blocks*.

| Block | Recipe |
| --- | --- |
| **Zechenziegel** (Colliery Bricks) | 2 bricks + 2 coal, diagonally → 4 |
| **Zechenziegeltreppe, -stufe, -mauer** (stairs, slab, wall) | the usual shapes, or the stonecutter |
| **Stahlfachwerk** (Steel-Framed Bricks) | 2 iron ingots + 2 Zechenziegel, diagonally → 4 |
| **Fachwerkfenster** (Steel-Framed Window) | 2 iron ingots + 2 glass, diagonally → 4 |
| **Stahlträger** (Steel Girder) | 3 iron ingots in a column → 3. Place it upright or on its side, like a log. |
| **Fördergerüst** (Headframe Lattice) | 5 iron ingots in an X → 8. You can see through it. |
| **Grubenlampe** (Miner's Lamp) | copper ingot, glass panes and a torch. Stands or hangs like a lantern and shines brightly. |
| **Schlägel und Eisen** (Hammer and Pick Tile) | a Zechenziegel in the stonecutter. The miners' sign, for above the gate. |

All of them are mined with a pickaxe.

![The same colliery at night, lit by its miner's lamps](../.github/media/zechenbau/nacht.jpg)

## Languages

English, German, Polish, Turkish, Dutch, French and Spanish: the languages of the Ruhr's miners.

## Building

```sh
./gradlew build
```

The jar ends up in `build/libs/`. `build` also runs the game tests (`src/gametest`): every block has an item, drops itself, needs a pickaxe and has a name in every language; every recipe loads; the lamp lights up its surroundings.

`./gradlew runClientGameTest` builds the colliery in the pictures above in a real client and takes the screenshots.

## License

MIT
