# Skyblock Togaether

Skyblock Togaether adds a skyblock dimension to your survival world instead of replacing it. You build a portal, step through, and get your own floating island in an empty sky — while the rest of the server keeps playing normally. Everything your island produces comes from a single block at its center, and making that block grow is the whole game.

I originally built this for my own server because I wanted skyblock progression without forcing everyone into a skyblock world. It grew a bit from there.

## Getting up there

Build a frame out of chiseled stone bricks (any size from 2x3 up to 21x21, same rules as a nether portal) and right-click it with a **Celestial Key** — crafted from 8 gold ingots around an ender pearl. The recipe unlocks in the recipe book once you pick up a pearl.

Each portal generates a floating island at the matching coordinates in the skyblock dimension. Portals built within 250 blocks of an existing island link to that island instead, exactly like nether portals do. Every island comes with a return portal that brings you back to the exact spot you left from, plus a starter chest, a tree, and the Infinite Block.

## The Infinite Block

It never breaks. Every time you mine it, it drops an item and gains XP. Right-click it to open its screen: current level, XP, and a list of **cycles** sorted into categories (Nature, Mining, Nether, End, Combat...). The active cycle decides what the block drops, how hard it is to mine, which tool works best on it, and how much XP each mine gives.

A few things worth knowing:

- Drops are weighted. Building blocks are common; ores are noticeably rarer than caving in vanilla. Anything that unlocks a renewable farm — saplings, seeds, kelp, spawn eggs of breedable mobs, saddles — sits under 0.1%. Getting one is an event.
- Item names in the screen are colored by drop rate, so you can see at a glance what the long shots are.
- Some cycles are training cycles: junk loot, fast mining, big XP. Others are jackpots: slow, almost no XP, only good stuff. Leveling up and getting rich are different jobs.
- The XP curve is exponential. Early levels come fast, level 50 is a long-term goal for a whole team (the block's progress is stored on the block, not per player).
- Tools matter. A wood cycle mines like wood with an axe, a stone cycle like stone with a pickaxe — Efficiency and Haste included. Late cycles get as hard as obsidian.
- Sneak-break the block to pick it up **with its XP, level and cycle** and place it somewhere else.

All of it — cycles, drops, weights, categories, hardness, XP thresholds — lives in `config/togaether-cycles.json`. The default config ships 50 levels and 56 cycles (~620 items), but you can rewrite the entire progression for your server without touching code.

## Nether and End included

The skyblock dimension has its own empty Nether and End, sealed off from the vanilla ones. Nether portals built on your island work normally (1:8 scale, portal linking) but lead to the skyblock Nether. End portals — frames drop from late cycles — lead to a skyblock End with a return portal, one arrival platform per island. You can't leak into the server's real Nether or End from up there, and vice versa.

## For server admins

- Four creative-only **XP Shards** (+1 / +10 / +1000 / +10000) restore a block's XP if someone destroys one by accident.
- Island height, minimum island spacing, portal frame block and key consumption are in `config/togaether-common.toml`.
- Delete `config/togaether-cycles.json` and restart to regenerate the default cycle config after an update.
- Ships with 12 languages (EN, FR, ES, PT-BR, DE, RU, ZH-CN, JA, KO, PL, IT).

## Requirements

- Minecraft **1.21.1**, NeoForge **21.1.x**
- Required on **both server and client** (custom blocks, items and GUI)

Found a bug or have a balance suggestion? Open an issue — the drop tables especially are meant to be argued about.
