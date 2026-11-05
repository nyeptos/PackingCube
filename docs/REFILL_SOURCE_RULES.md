# Refill pouch: source-slot rules (PC-47)

## Workflow

1. Open refill pouch settings. The **1–9 buttons select a source slot**, not destinations.
   Merely changing pages never changes item configuration or enters selection mode.
2. Set source ON/OFF, priority0–9, and replacement OFF/ON in Destination; configure timing
   and final count in Method. Each source has exactly one destination, or is unassigned.
3. Press **Select destination** with an empty cursor. The settings window temporarily folds
   away; the screen states which source is being assigned. Inventory/armor/Curios slots become
   selection buttons, not movable item slots. Pouch storage slots cannot be destinations.
4. Matching left press/release confirms a destination and **automatically exits selection mode**.
   Releasing elsewhere commits nothing. Esc or Cancel exits without changing the old destination.
5. Normal mode resumes item pickup, placement, Shift-move, number/offhand swap and drop inputs.
   During selection these item actions are suppressed. Source tabs alone never activate selection.

The currently edited source and its assigned destination use the supplied cyan mark.
Source1 and source2 may both point to the same destination. All normal inventory27,
hotbar9, armor4, offhand1 and currently active functional Curios slots are supported.
The source item itself determines the item/components to supply; no sample registration.

## Automatic supply

- Only installed refill pouches in carried/equipped backpacks supply. Loose pouches do not.
- New pouches start with source rules enabled as a mode, but **all destinations unassigned**.
  They cannot implicitly refill an arbitrary hand slot.
- An empty selected destination receives its assigned source item without a use event.
  Empty-slot initialization also applies to threshold/use timing modes.
- With the same item and exact components, add up to the source rule's final count, clamped
  by actual item and destination limits. Zero configured count means one full legal stack.
- Empty timing: refill only when empty. Threshold: refill at/below configured remaining count.
  Use timing: for occupied hands, refill only after witnessed hand-item use; passive empty fill
  still works. Non-hand targets should normally use empty/threshold timing.
- Insertion never draws from another source slot. Empty source = no supply. The pouch-wide
  optional refill filter still restricts withdrawal, not storage.
- Source priority descends9→0 across candidates. Ties use saved pouch priority, equipped/carried
  pouch order, then source1→9. One candidate owns a destination per dispatch pass. A stocked,
  already-satisfied higher candidate also claims it, preventing lower replacement each tick.
  If higher stock is empty or ineligible, the next candidate is tried. With replacement ON,
  a fallback may replace the previous candidate's item once that higher stock is exhausted.
- Ordinary passive checks occur every5 ticks (up to0.25s). Witnessed hand use also triggers an
  immediate Post tick pass. Mapped supply keeps running while this mod's bag screen (including its
  settings panel) is open, and resends the open screen's slots after a pass. The backpack whose
  screen is open is never a source, and source edits still have no automatic inventory effects.
  Other containers, an occupied cursor, and dead/spectator/creative players pause mapped supply.

## Replacement safety

Replacement defaults OFF. With a different destination item and replacement ON:

- Respect the actual vanilla Slot's mayPlace/mayPickup, armor type, stack limits and binding curse.
- Curios resolves current type+index every pass, uses public validity/unequip checks, and skips
  inactive/removed slots. Cosmetic slots are excluded; normal Curios equipment callbacks/sync
  remain managed by Curios. Slot order or count changes never retarget saved rules.
- Preflight return capacity in ordinary player inventory0–35 **outside every enabled assigned
  destination**. Existing matching stacks first, then empty slots. If the whole displaced stack
  cannot fit, neither destination, source nor return inventory is changed. No item is dropped.
- Backpacks/pouches themselves are never displaced, including a live source backpack, preventing
  a stale nested-storage copy or root movement from duplicating contents.
- Replacement is one switch (`replace`). While it is on, an occupied destination is replaced
  whenever the incoming stack is not identical to the one in place. Item data such as durability
  is never compared, so a worn stack can take the slot of a fresh one.
- Durability limit (`durabilityMode` = `off`/`percent`/`count`, `durability` = 1-100): a
  same-item swap can wait until the stack in place is nearly spent. `percent` compares the share
  left, `count` the remaining points. Anything above the limit is left alone, so a partly worn
  tool keeps its slot. Other items are never delayed, and `off` keeps the plain rule.
- The limit belongs to the destination, not to one source. Every rule aimed at the same
  destination is checked, and a single rule that asks to wait holds the slot for that pass, so a
  second source or a second pouch with the same destination can never swap in ahead of it.
- One destination is handled by one successful/satisfied candidate per pass, not repeated swaps.
- Source count is reduced only after validation and successful destination write; exact components
  are retained. Inventory synchronization corrects predicted item-use counts even if restored to
  the previous server-sent count. Real visual/network multiplayer confirmation remains pending.

## Returned items and slot locks

The displaced item goes back into the **same pouch** by default (`collect` ON) instead of the
inventory, and the slot that receives it is **locked** (`lock` ON) so it never supplies again.
Both are per-source switches shown on the Destination page only while replacement is ON.

- Return slot search runs on the contents **after** this removal: the freed source slot first,
  then a same item/components slot with room, then an empty slot. The whole displaced stack must
  fit one slot, so a returned item is never split or duplicated.
- No suitable pouch slot falls back to the inventory return above. If neither side has room the
  pass aborts and nothing is written.
- The lock is a 9-bit `locked` mask in `packingcube_refill`, not item data on the returned stack,
  so split/merge/manual moves cannot carry a stale marker. Emptying the slot, by hand or by any
  other move, clears its bit; a slot refilled while still occupied stays locked.
- Locked sources are skipped before any destination or source check. `lock` OFF keeps collecting
  into the pouch without blocking, which can swap the same item back and forth by design.
- Slot writes happen in one save: the lock bit is written first, then the slot content, so the
  pouch's parent inventory persists both together.
- The pouch screen dims a locked slot and draws a small lock. The explanation lives in the
  settings help pane, not in an item tooltip, so it cannot cover neighbouring slots.

## Shared settings and migration

Refill filter and merchant supply remain pouch-wide under **Shared**. Source OFF also blocks
that source from merchant withdrawal; active sources use their source-priority order within the
existing pouch-priority order. Merchant transactions remain manually executed, never auto-buy.

PC45/46 saved pouches without `packingcube_refill_sources` retain legacy hand behavior until an
explicit per-source edit writes the new compound. Browsing does not migrate, delete items or
invent destination mappings. Existing legacy hand settings are not erased; after mode transition
source rules exclusively control this pouch's ordinary inventory/equipment supply.

Rules are keyed by source0–8 in item custom data. Destination is canonical `inventory:0..40`
or `curios:<type>:<index>`. All values are strictly bounded. Snapshots/radial previews copy only
configuration, not contents. Operation11 validates source index, current menu ownership, destination
and live Curios availability; destination assignment requires an empty server cursor.
Network version5 requires matching client/server versions.
