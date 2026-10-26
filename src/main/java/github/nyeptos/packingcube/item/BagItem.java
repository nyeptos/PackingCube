package github.nyeptos.packingcube.item;

import github.nyeptos.packingcube.bag.BagInventory;
import github.nyeptos.packingcube.bag.filter.BagFilterTooltip;
import github.nyeptos.packingcube.bag.refill.RefillSettings;
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
        if (stack.is(ModItems.REFILL_POUCH.get())) {
            lines.add(Component.translatable("tooltip.packingcube.refill_supply"));
            lines.add(Component.translatable("tooltip.packingcube.refill_trade_note"));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_offhand_"
                                    + (RefillSettings.offhandEnabled(stack) ? "on" : "off")));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_timing",
                            Component.translatable(
                                    "gui.packingcube.refill_timing_"
                                            + RefillSettings.timing(stack))));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_target",
                            RefillSettings.target(stack) == 0
                                    ? Component.translatable("gui.packingcube.refill_full_stack")
                                    : RefillSettings.target(stack)));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_hotbar_"
                                    + (RefillSettings.enabled(stack, false) ? "on" : "off")));
            lines.add(
                    Component.translatable(
                            "gui.packingcube.refill_trade_"
                                    + (RefillSettings.enabled(stack, true) ? "on" : "off")));
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
