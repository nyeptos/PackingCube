package github.nyeptos.packingcube.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import github.nyeptos.packingcube.bag.BagFilter;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Temporary filter editor, usable without cheats; only edits the caller's main-hand filter bag. */
public final class BagCommands {
    private BagCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var filter =
                Commands.literal("filter")
                        .then(Commands.literal("show").executes(ctx -> show(ctx.getSource())))
                        .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())));
        filter.then(
                Commands.literal("mode")
                        .then(
                                Commands.literal("include")
                                        .executes(ctx -> mode(ctx.getSource(), false)))
                        .then(
                                Commands.literal("exclude")
                                        .executes(ctx -> mode(ctx.getSource(), true))));
        for (String field : BagFilter.FIELDS) {
            filter.then(
                    Commands.literal(field)
                            .then(
                                    Commands.argument("id", ResourceLocationArgument.id())
                                            .executes(
                                                    ctx ->
                                                            add(
                                                                    ctx.getSource(),
                                                                    field,
                                                                    ResourceLocationArgument.getId(
                                                                            ctx, "id")))));
        }
        event.getDispatcher()
                .register(
                        Commands.literal("packingcube")
                                .requires(
                                        source ->
                                                source.getEntity()
                                                        instanceof
                                                        net.minecraft.server.level.ServerPlayer)
                                .then(filter));
    }

    private static ItemStack heldBag(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack bag = source.getPlayerOrException().getMainHandItem();
        if (!bag.is(ModItems.FILTER_BAG.get())) {
            source.sendFailure(Component.translatable("message.packingcube.hold_filter"));
            return ItemStack.EMPTY;
        }
        // Do not edit a bag while another live menu view may be using its data.
        if (source.getPlayerOrException().containerMenu
                != source.getPlayerOrException().inventoryMenu) {
            source.sendFailure(Component.translatable("message.packingcube.close_menu"));
            return ItemStack.EMPTY;
        }
        return bag;
    }

    private static int add(CommandSourceStack source, String field, ResourceLocation id)
            throws CommandSyntaxException {
        ItemStack bag = heldBag(source);
        if (bag.isEmpty()) return 0;
        if ((field.equals("items") || field.equals("exclude_items"))
                && !BuiltInRegistries.ITEM.containsKey(id)) {
            source.sendFailure(
                    Component.translatable("message.packingcube.unknown_item", id.toString()));
            return 0;
        }
        if (!BagFilter.add(bag, field, id)) {
            source.sendFailure(
                    Component.translatable(
                            "message.packingcube.filter_limit", BagFilter.MAX_RULES));
            return 0;
        }
        source.getPlayerOrException().getInventory().setChanged();
        source.sendSuccess(
                () ->
                        Component.translatable(
                                "message.packingcube.filter_added", field, id.toString()),
                false);
        return 1;
    }

    private static int clear(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack bag = heldBag(source);
        if (bag.isEmpty()) return 0;
        BagFilter.clear(bag);
        source.getPlayerOrException().getInventory().setChanged();
        source.sendSuccess(
                () -> Component.translatable("message.packingcube.filter_cleared"), false);
        return 1;
    }

    private static int mode(CommandSourceStack source, boolean excluding)
            throws CommandSyntaxException {
        ItemStack bag = heldBag(source);
        if (bag.isEmpty()) return 0;
        BagFilter.setExcluding(bag, excluding);
        source.getPlayerOrException().getInventory().setChanged();
        source.sendSuccess(
                () ->
                        Component.translatable(
                                "message.packingcube.mode_changed",
                                excluding ? "exclude" : "include"),
                false);
        return 1;
    }

    private static int show(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack bag = heldBag(source);
        if (bag.isEmpty()) return 0;
        source.sendSuccess(
                () ->
                        Component.literal(
                                "mode: " + (BagFilter.isExcluding(bag) ? "exclude" : "include")),
                false);
        for (String field : java.util.List.of("tags", "items")) {
            source.sendSuccess(
                    () ->
                            Component.literal(
                                    field
                                            + ": "
                                            + String.join(", ", BagFilter.entries(bag, field))),
                    false);
        }
        return 1;
    }
}
