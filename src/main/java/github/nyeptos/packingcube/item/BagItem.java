package github.nyeptos.packingcube.item;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.BulkStore;
import github.nyeptos.packingcube.bag.refill.RefillPouchSettings;
import github.nyeptos.packingcube.menu.BagMenuOpener;
import github.nyeptos.packingcube.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class BagItem extends Item {
    private final boolean modules;

    public BagItem(boolean modules, Properties properties) {
        super(properties);
        this.modules = modules;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (!modules) BagFilterTooltip.append(stack, lines);
        if (stack.is(ModItems.BULK_POUCH.get())) {
            lines.add(Component.translatable("tooltip.packingcube.bulk_pouch"));
            int held = BulkStore.count(stack);
            if (held > 0)
                lines.add(
                        Component.translatable(
                                "tooltip.packingcube.bulk_count", held, BulkStore.LIMIT));
        }
        if (stack.is(ModItems.REFILL_POUCH.get())) {
            lines.add(Component.translatable("tooltip.packingcube.refill_supply"));
            lines.add(Component.translatable("tooltip.packingcube.refill_trade_note"));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_offhand_"
                                    + (RefillPouchSettings.offhandEnabled(stack) ? "on" : "off")));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_target", RefillPouchSettings.target(stack)));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_threshold", RefillPouchSettings.threshold(stack)));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_hotbar_"
                                    + (RefillPouchSettings.enabled(stack, false) ? "on" : "off")));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_trade_"
                                    + (RefillPouchSettings.enabled(stack, true) ? "on" : "off")));
        }
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack root = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int ownerSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            BagInventory contents =
                    new BagInventory(root, modules, player.getInventory()::setChanged);
            contents.recoverLegacyOverflow(player);
            BagMenuOpener.open(player, contents, root, ownerSlot, modules, root.getHoverName());
        }
        return InteractionResultHolder.sidedSuccess(root, level.isClientSide);
    }
}
