package qdeeme.xp_simplifier.data.lang;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import qdeeme.xp_simplifier.Xp_Simplifier;

public class XpsLangProviderEN extends LanguageProvider {

    public XpsLangProviderEN(PackOutput output) {
        super(output, Xp_Simplifier.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {

        //  In-game config screen (ConfigScreen)
        add("screen.xp_simplifier.config.title", "XP Simplifier — In-Game Config");
        add("screen.xp_simplifier.config.apply", "Apply");
        add("screen.xp_simplifier.config.cancel", "Cancel");

        add("screen.xp_simplifier.config.hint.main", "§6XP Simplifier Command Center!");
        add("screen.xp_simplifier.config.hint.op", "§2Changes are global, use with caution!");
        add("screen.xp_simplifier.config.hint.readonly", "§cRead-only (requires operator permissions)");

        add("screen.xp_simplifier.config.tab.modes", "XP Modes");
        add("screen.xp_simplifier.config.tab.multipliers", "XP Multipliers");
        add("screen.xp_simplifier.config.tab.mending", "Mending");
        add("screen.xp_simplifier.config.tab.maps", "Maps");

        // Mode row labels
        add("screen.xp_simplifier.config.row.orbMode", "Orb Mode");
        add("screen.xp_simplifier.config.row.entityXpMode", "Entities Mode");
        add("screen.xp_simplifier.config.row.blockXpMode", "Blocks Mode");
        add("screen.xp_simplifier.config.row.cropXpMode", "Crops Mode");
        add("screen.xp_simplifier.config.row.breedingXpMode", "Breeding Mode");
        add("screen.xp_simplifier.config.row.fishingXpMode", "Fishing Mode");
        add("screen.xp_simplifier.config.row.tradingPlayerXp", "Trading Player Mode");
        add("screen.xp_simplifier.config.row.merchantXp", "Merchants Mode");
        add("screen.xp_simplifier.config.row.grindstoneXpMode", "Grindstone Mode");
        add("screen.xp_simplifier.config.row.smeltingXpMode", "Smelting Mode");

        // Modes tooltip
        add("screen.xp_simplifier.config.row.orbMode.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "SIMPLE - all XP orbs are suppressed and XP is awarded directly to the player");
        add("screen.xp_simplifier.config.row.entityXpMode.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP for entities category entirely");
        add("screen.xp_simplifier.config.row.blockXpMode.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP for blocks category entirely");
        add("screen.xp_simplifier.config.row.cropXpMode.tooltip", "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP for crops category entirely");
        add("screen.xp_simplifier.config.row.breedingXpMode.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP for breeding category entirely");
        add("screen.xp_simplifier.config.row.fishingXpMode.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP for fishing category entirely");
        add("screen.xp_simplifier.config.row.tradingPlayerXp.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP from trading entirely");
        add("screen.xp_simplifier.config.row.merchantXp.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP for merchants entirely");
        add("screen.xp_simplifier.config.row.grindstoneXpMode.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP from grinding enchants entirely");
        add("screen.xp_simplifier.config.row.smeltingXpMode.tooltip", "VANILLA - default vanilla behaviour" + System.lineSeparator()
                + "ON - enable custom XP (uses JSON table values)" + System.lineSeparator()
                + "OFF - disable XP from smelting/cooking entirely");

        // Multiplier row labels
        add("screen.xp_simplifier.config.row.entityXpMultiplier", "Entities XP");
        add("screen.xp_simplifier.config.row.blockXpMultiplier", "Blocks XP");
        add("screen.xp_simplifier.config.row.cropXpMultiplier", "Crops XP");
        add("screen.xp_simplifier.config.row.smeltingXpMultiplier", "Smelting XP");
        add("screen.xp_simplifier.config.row.tradingXpMultiplier", "Trading Player XP");
        add("screen.xp_simplifier.config.row.merchantXpMultiplier", "Merchants XP");
        add("screen.xp_simplifier.config.row.breedingXpMultiplier", "Breeding XP");
        add("screen.xp_simplifier.config.row.fishingXpMultiplier", "Fishing XP");
        add("screen.xp_simplifier.config.row.grindstoneXpMultiplier", "Grindstone XP");

        // Tooltip range templates (%s = label, additional args = bounds)
        add("screen.xp_simplifier.config.multiplier.range", "%s  (0.01 – 1000.0)");
        add("screen.xp_simplifier.config.int.range", "%s  (%s – %s)");

        // Mending page
        add("screen.xp_simplifier.config.row.xpRepairEnabled", "Mending Repair");
        add("screen.xp_simplifier.config.row.durabilityPerPoint", "Durability points per XP Point");
        add("screen.xp_simplifier.config.row.maxAnvilRepairCost", "Max Repair Cost in levels");
        add("screen.xp_simplifier.config.mending.button", "%s");
        add("screen.xp_simplifier.config.mending.on", "ON");
        add("screen.xp_simplifier.config.mending.off", "OFF");
        add("screen.xp_simplifier.config.mending.tooltip", "Enable XP-based item repair at the anvil if mending is present.");

        // Maps Cats
        add("screen.xp_simplifier.config.maps.label.blocks", "Blocks");
        add("screen.xp_simplifier.config.maps.label.crops", "Crops");
        add("screen.xp_simplifier.config.maps.label.entities", "Entities");
        add("screen.xp_simplifier.config.maps.label.smelting", "Smelting/Cooking");
        add("screen.xp_simplifier.config.maps.label.trading", "Trading");
        add("screen.xp_simplifier.config.maps.label.breeding", "Breeding");
        add("screen.xp_simplifier.config.maps.label.fishing", "Fishing");
        add("screen.xp_simplifier.config.maps.label.grindstone", "Grindstone");

        // Maps page
        add("screen.xp_simplifier.config.maps.search_box", "Search");
        add("screen.xp_simplifier.config.maps.search_box_tooltip", "Search by mod_id/id/keyword");
        add("screen.xp_simplifier.config.maps.label.countKey", "Entries: ");
        add("screen.xp_simplifier.config.maps.back", "← Back");
        add("screen.xp_simplifier.config.maps.edit", "Edit");
        add("screen.xp_simplifier.config.maps.add.type", "XP Values");
        add("screen.xp_simplifier.config.maps.add", "+ Add New");
        add("screen.xp_simplifier.config.maps.add.confirm", "Add Entry");
        add("screen.xp_simplifier.config.maps.add.category", "Category");
        add("screen.xp_simplifier.config.maps.add.key", "Key (modid:id)");
        add("screen.xp_simplifier.config.maps.type.button", "%s");
        add("screen.xp_simplifier.config.maps.label.type", "Type");
        add("screen.xp_simplifier.config.maps.label.min", "Min");
        add("screen.xp_simplifier.config.maps.label.max", "Max");
        add("screen.xp_simplifier.config.maps.label.float", "Float");
        add("screen.xp_simplifier.config.maps.add.type.random", "Random");
        add("screen.xp_simplifier.config.maps.add.type.fixed", "Fixed");
        add("screen.xp_simplifier.config.maps.label.tradingPlayerXp", "Per-trade Player XP");
        add("screen.xp_simplifier.config.maps.label.merchantXp", "Per-trade Merchant XP");

        // networking
        add("network.xp_simplifier.config.invalid_packet_received", "[XPS] Invalid packet received due to no edit permission - you have been disconnected for security reasons. If problem persists, contact moderation or report an issue on Github.");

        // commands/ messages
        add("message.xp_simplifier.config.applied", "§aPlayer§r %s §ahas updated the config.");
        add("message.xp_simplifier.command.edit_permission.xps_label", "§l[XPS]§r ");
        add("message.xp_simplifier.command.edit_permission.list.empty", "List of players with edit permission is empty");
        add("message.xp_simplifier.command.edit_permission.list", "List of players with edit permission (%s):");
        add("message.xp_simplifier.command.edit_permission.delete", "§cRemoved player§r %s §cfrom the EditPermission list.§r"
                + System.lineSeparator() + "To allow specific player editing the config,"
                + System.lineSeparator() + "use /xps add <player> <true>");
        add("message.xp_simplifier.command.edit_permission.mapview", "Current mapview status: %s");
        add("message.xp_simplifier.command.edit_permission.mapview.on", "§aEnabled");
        add("message.xp_simplifier.command.edit_permission.mapview.off", "§cDisabled");
        add("message.xp_simplifier.command.edit_permission.can.on", "§aPlayer§r %s §ahas been granted edit permission");
        add("message.xp_simplifier.command.edit_permission.can.off", "§cPlayer§r %s §cno longer has edit permission");
        add("message.xp_simplifier.command.edit_permission.notfound", "Player: %s not found");
        add("message.xp_simplifier.command.edit_permission.exception", "An error occurred while processing the command");


    }
}
