# Minecraft-Mods

A collection of Minecraft mods. Every mod lives in its own folder and is a complete, standalone Gradle project. Open the folder in your IDE and build it on its own.

| Mod | Loader | Minecraft | Description |
| --- | --- | --- | --- |
| [Kumpel](kumpel/) | Fabric | 26.3 | A small mining golem that follows you, collects items, senses and mines ores, and levels up from copper to netherite. Configurable, with JEI and REI support. |
| [Zechenbau](zechenbau/) | Fabric | 26.3 | Building blocks of the Ruhr's collieries: colliery bricks, steel framework, girders, headframe lattice, sheave wheels, coal tubs and miner's lamps. |
| [Pottküche](pottkueche/) | Fabric | 26.3 | Food from the Ruhr: Currywurst and Pommes, Bratwurst, Frikadellen, Pfefferpotthast and Malzbier. The Kumpels' favourite food. |

## Layout

```
Minecraft-Mods/
├── .github/
│   ├── workflows/<mod>.yml   one CI workflow per mod, triggered only by changes in its folder
│   └── media/<mod>/          screenshots for the READMEs (not part of the mods)
└── <mod>/                    a normal mod project: build.gradle, gradlew, src/, README, LICENSE, CHANGELOG
```

## Building a mod

```sh
cd kumpel
./gradlew build
```

The jar ends up in `<mod>/build/libs/`. GitHub Actions builds every mod on each push that touches it; the jar can be downloaded from the run's **Artifacts**.

## Adding a mod

1. Create a new folder with a standalone Gradle project (e.g. from the [Fabric example mod](https://github.com/FabricMC/fabric-example-mod)).
2. Copy `.github/workflows/kumpel.yml`, rename it and change the paths to the new folder.
3. Add a row to the table above.
