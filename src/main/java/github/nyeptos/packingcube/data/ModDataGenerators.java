package github.nyeptos.packingcube.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Client assets and server recipes can be generated independently using standard runData flags. */
public final class ModDataGenerators {
    private ModDataGenerators() {}

    public static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        generator.addProvider(
                event.includeClient(),
                new ModItemModelProvider(output, event.getExistingFileHelper()));
        for (var locale : ModLanguageProvider.Locale.values()) {
            generator.addProvider(event.includeClient(), new ModLanguageProvider(output, locale));
        }
        generator.addProvider(
                event.includeServer(), new ModRecipeProvider(output, event.getLookupProvider()));
    }
}
