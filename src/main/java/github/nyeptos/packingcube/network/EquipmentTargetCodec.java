package github.nyeptos.packingcube.network;

import github.nyeptos.packingcube.bag.refill.RefillTargets;
import github.nyeptos.packingcube.compat.BagEquipment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Wire format for the equipment slot descriptors carried by the menu-open payload. */
public final class EquipmentTargetCodec {
    private EquipmentTargetCodec() {}

    public static void write(RegistryFriendlyByteBuf data, List<BagEquipment.Target> targets) {
        data.writeVarInt(targets.size());
        for (var target : targets) {
            data.writeUtf(target.id(), 256);
            data.writeResourceLocation(target.icon());
        }
    }

    public static List<BagEquipment.Target> read(RegistryFriendlyByteBuf data) {
        int count = data.readVarInt();
        if (count < 0 || count > 4096)
            throw new IllegalArgumentException("Invalid Curios slot count");
        List<BagEquipment.Target> targets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = data.readUtf(256);
            if (!RefillTargets.valid(id)
                    || !id.startsWith("curios:")
                    || targets.stream().anyMatch(t -> t.id().equals(id)))
                throw new IllegalArgumentException("Invalid Curios slot ID");
            targets.add(new BagEquipment.Target(id, data.readResourceLocation(), ItemStack.EMPTY));
        }
        return List.copyOf(targets);
    }
}
