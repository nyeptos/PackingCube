package github.nyeptos.packingcube.menu.station;

/**
 * A menu that drives a station block carried in a module slot.
 *
 * <p>The radial key is read from screen events because a vanilla screen swallows key mappings, so
 * the handler needs a way to recognise the station panels without listing every screen class.
 */
public interface CarriedStationMenu {}
