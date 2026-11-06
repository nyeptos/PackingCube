/**
 * Mod entry point and the package map. Each package below owns one responsibility:
 * bag (what a bag holds), gui (client-agnostic UI model), client (rendering and input),
 * menu (containers), network (payloads), registry (deferred registration),
 * datagen (generated data), event (game-bus wiring), compat (optional mods),
 * command (commands), item (items), gametest (in-game tests).
 */
package github.nyeptos.packingcube;
