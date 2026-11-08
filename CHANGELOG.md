# Changelog

## 0.1.0 — first release

### Backpack

- Backpack item with nine module slots for pouches.
- Wearing and opening: radial menu (`C`) and backpack screen (`B`) are separate key bindings.
- Optional Curios back slot support, with the backpack rendered on the player.
- The backpack id is `packingcube:backpack`. The old `packingcube:large_bag` is kept as a registry
  alias, so existing worlds keep their bags.

### Storage pouch

- Nine filter slots, configured from a sample item or by searching ids.
- Include / exclude mode on top of the same samples, so one filter can do both jobs.
- Auto-store on pickup with a per-pouch on/off switch and a priority order.
- Manual bulk stow from the normal inventory (shift-click) that follows the same filters and skips
  refill pouches.

### Refill pouch

- Refills hotbar slots and the trade input slots from its own contents.
- Per-source rules: each source slot can be pointed at its own destination.
- Replacement waits for a durability threshold, by percentage or by absolute value.
- Replacement conditions can be combined with AND / OR and negated; the editor is in game.
- Keeps refilling while its settings screen is open.
- Collected slots lock after a refill and stay locked until released.

### Bulk pouch

- Single-item storage up to 4096, well past the vanilla stack limit.
- Separate import and export slots; left click takes one, right click takes a stack.
- Import and export settings are configured in the pouch itself.

### Pouch station

- New block that crafts pouches and the backpack from inventory materials, without a crafting grid.
- Lists every bill with held/needed counts per material, greys out what you cannot afford.
- Scrollable list, selection, and a result slot; leftover results return to the player on close.

### Stations carried in the bag

- Crafting table, furnace, smoker, blast furnace, brewing stand, anvil, smithing table, loom,
  cartography table, stonecutter and grindstone can be carried in a module slot and opened from the
  radial menu.
- The furnace family and the brewing stand keep their progress while the GUI is closed.
- Enchanting table is not supported.

### Interface

- Artist-drawn widget skins for buttons, toggles, sliders, scrollbars and slots.
- Hover and press feedback (sound plus animation) on slots, rows, buttons and the radial menu.
- Toggleable help pane: focus a control and its description appears in a side window instead of a
  tooltip over the panel.
- Floating settings window that can be moved and resized without closing the bag.

### Recipes

- Pouch station: three leather, two planks, one crafting table, three cobblestone.
- Storage pouch: leather 5, string 2, chest 1.
- Refill pouch: iron ingot 2, hopper 1, storage pouch 1, redstone 1.
- Bulk pouch: iron ingot 4, chest 4, storage pouch 1.
- Backpack: leather 5, storage pouch 2, chest 1.
- The backpack and the pouches are no longer crafting table recipes; the station is the only way to
  make them.

### Internal

- Data generation for models, language files, recipes, advancements and loot tables.
- 338 game tests covering storage, filtering, refill rules, stations and rendering metadata.
