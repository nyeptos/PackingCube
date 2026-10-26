package github.nyeptos.packingcube.event;

import github.nyeptos.packingcube.command.BagCommands;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;

/** Runtime game-bus subscriptions. Do not register these on the mod lifecycle bus. */
public final class ModGameEvents {
    private ModGameEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(BagCommands::register);
        gameBus.addListener(EventPriority.LOWEST, BagPickupHandler::onPickup);
        gameBus.addListener(EventPriority.LOWEST, RefillHandler::onBlock);
        gameBus.addListener(EventPriority.LOWEST, RefillHandler::onItem);
        gameBus.addListener(EventPriority.LOWEST, RefillHandler::onEntity);
        gameBus.addListener(EventPriority.LOWEST, RefillHandler::onFinish);
        gameBus.addListener(EventPriority.LOWEST, RefillHandler::onSpecific);
        gameBus.addListener(EventPriority.LOWEST, RefillHandler::onUseOn);
        gameBus.addListener(RefillHandler::onConsumed);
        gameBus.addListener(EventPriority.LOWEST, RefillHandler::onTotem);
        gameBus.addListener(RefillHandler::onToss);
        gameBus.addListener(RefillHandler::onTick);
    }
}
