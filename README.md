# PackingCube

A modular bag system for Minecraft **1.21.1** on **NeoForge**. Fit functional pouches into a
backpack, open them from a radial menu, and let the bag keep the tools you reach for stocked.

Instead of digging through chests and shuffling stacks, you configure the bag once: which items
each pouch accepts, which slot gets refilled, and which pouch wins when two of them want the same
item.

## Requirements

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264 or later (21.x) |
| Curios | optional, 9.5.1+ — only needed to wear the backpack in the back slot |

## Install

1. Install NeoForge for 1.21.1.
2. Put `packingcube-<version>.jar` into your `mods/` folder.
3. Launch the game. The mod works without Curios; with Curios the backpack can sit in the back
   slot and render on your character.

## Using the bag

- **Backpack** — the root container. It has nine module slots, and every pouch lives in one of them.
- **Radial menu** (`C` by default) — opens the pouches and carried stations of the backpack you are
  wearing or holding, without opening the bag itself. It also opens while a station GUI is up.
- **Backpack screen** (`B` by default) — the full bag, the pouch settings and the filters.
- **Pouches** — storage, refill and bulk. Each one has its own filter, its own settings and its own
  auto-store switch, so the bag sorts pickups the way you set it up.

## Features

### Storage pouch
- Nine slots with a filter, set from a sample item instead of typing IDs (an ID search box is there
  for the awkward cases).
- Include or exclude mode: keep everything the samples match, or keep everything they do not.
- Per-pouch auto-store toggle and priority, so overlapping filters resolve the same way every time.

### Refill pouch
- Refills the hotbar and the trade input slots from the items it stores.
- Rules per source, so a hotbar slot and a trade slot can pull from different places.
- Waits for a durability threshold before replacing a tool — by percentage or by absolute value.
- Replacement conditions can be combined with AND / OR and negated, and they are edited in game.
- Keeps working while its settings screen is open, and collected slots stay locked until you unlock
  them.

### Bulk pouch
- Holds a single item type up to **4096**, far past the normal stack limit.
- Left click takes one, right click takes a stack; the import and export sides are separate slots.

### Pouch station
- A block that crafts every pouch (and the backpack) from the materials in your inventory — no
  crafting grid, no material slots. It lists the bills, shows held/needed per material, and greys
  out what you cannot afford yet.

### Stations in the bag
- Crafting-related blocks can be carried in a module slot and opened from the radial menu:
  crafting table, furnace, smoker, blast furnace, brewing stand, anvil, smithing table, loom,
  cartography table, stonecutter and grindstone.
- The furnace family and the brewing stand keep their progress while the GUI is closed.
- The enchanting table is not supported.

### Interface
- Artist-drawn widget skins with hover and press feedback, a help pane you can toggle on and off,
  and a settings window that floats above the bag instead of replacing it.

## Crafting

Everything is made at the **pouch station**. The station itself is a normal crafting table recipe:
three leather, two planks, one crafting table and three cobblestone.

The station bills are read from your inventory:

| Result | Materials |
| --- | --- |
| Storage pouch | leather 5, string 2, chest 1 |
| Refill pouch | iron ingot 2, hopper 1, storage pouch 1, redstone 1 |
| Bulk pouch | iron ingot 4, chest 4, storage pouch 1 |
| Backpack | leather 5, storage pouch 2, chest 1 |

## Compatibility

- **Curios** — the backpack can be worn in the back slot and renders on the player. Everything else
  works without it.
- Existing worlds keep working: the backpack was renamed from `packingcube:large_bag` to
  `packingcube:backpack`, and the old id is still resolved through a registry alias.

## Status

0.1.0 is the first release. The mod is covered by 338 in-game tests (game test server) that run on
every build.

## License

MIT. See [LICENSE](LICENSE).
