package qdeeme.xp_simplifier.data.lang;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import qdeeme.xp_simplifier.Xp_Simplifier;

public class XpsLangProviderPL extends LanguageProvider {

    public XpsLangProviderPL(PackOutput output) {
        super(output, Xp_Simplifier.MOD_ID, "pl_pl");
    }

    @Override
    protected void addTranslations() {
        // in-game messages


        //  In-game config screen (ConfigScreen)
        add("screen.xp_simplifier.config.title", "XP Simplifier — Konfiguracja");
        add("screen.xp_simplifier.config.apply", "Zastosuj");
        add("screen.xp_simplifier.config.cancel", "Anuluj");

        add("screen.xp_simplifier.config.hint.main", "§6Centrum dowodzenia XP Simplifier!");
        add("screen.xp_simplifier.config.hint.op", "§2Wszystkie zmiany są globalne. Używaj z rozwagą :)");
        add("screen.xp_simplifier.config.hint.readonly", "§cTylko do odczytu (wymagane uprawnienia operatora)");

        add("screen.xp_simplifier.config.tab.modes", "Tryby XP");
        add("screen.xp_simplifier.config.tab.multipliers", "Mnożniki XP");
        add("screen.xp_simplifier.config.tab.mending", "Naprawa (Mending)");
        add("screen.xp_simplifier.config.tab.maps", "Mapy");

        // Mode row labels
        add("screen.xp_simplifier.config.row.orbMode", "Tryb XP");
        add("screen.xp_simplifier.config.row.entityXpMode", "Tryb mobów");
        add("screen.xp_simplifier.config.row.blockXpMode", "Tryb bloków");
        add("screen.xp_simplifier.config.row.cropXpMode", "Tryb upraw");
        add("screen.xp_simplifier.config.row.breedingXpMode", "Tryb rozmnażania");
        add("screen.xp_simplifier.config.row.fishingXpMode", "Tryb łowienia");
        add("screen.xp_simplifier.config.row.tradingPlayerXp", "Tryb handlu - gracze");
        add("screen.xp_simplifier.config.row.merchantXp", "Tryb handlu - handlarze");
        add("screen.xp_simplifier.config.row.grindstoneXpMode", "Tryb usuwania enchantów (szlifierka)");
        add("screen.xp_simplifier.config.row.smeltingXpMode", "Tryb piecyków");

        // Modes tooltip
        add("screen.xp_simplifier.config.row.orbMode.tooltip", "VANILLA - domyślne zachowanie XP" + System.lineSeparator()
                + "SIMPLE - wszystkie kule XP są usuwane, a XP trafia bezpośrednio do gracza");
        add("screen.xp_simplifier.config.row.entityXpMode.tooltip", "VANILLA - domyślne wartości XP" + System.lineSeparator()
                + "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz drop XP dla mobów całkowicie");
        add("screen.xp_simplifier.config.row.blockXpMode.tooltip", "VANILLA - domyślne wartości XP" + System.lineSeparator()
                + "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz drop XP dla bloków całkowicie");
        add("screen.xp_simplifier.config.row.cropXpMode.tooltip", "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz drop XP dla upraw całkowicie");
        add("screen.xp_simplifier.config.row.breedingXpMode.tooltip", "VANILLA - domyślne wartości XP" + System.lineSeparator()
                + "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz drop XP dla rozmnażania całkowicie");
        add("screen.xp_simplifier.config.row.fishingXpMode.tooltip", "VANILLA - domyślne wartości XP" + System.lineSeparator()
                + "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz XP drop XP łowienia całkowicie");
        add("screen.xp_simplifier.config.row.tradingPlayerXp.tooltip", "VANILLA - domyślne wartości XP" + System.lineSeparator()
                + "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz drop XP z handlu całkowicie");
        add("screen.xp_simplifier.config.row.merchantXp.tooltip", "VANILLA - domyślne wartości XP" + System.lineSeparator()
                + "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz zdobywanie XP dla handlarzy całkowicie");
        add("screen.xp_simplifier.config.row.grindstoneXpMode.tooltip", "VANILLA - domyślne wartości XP" + System.lineSeparator()
                + "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz drop XP z usuwania enchantów całkowicie");
        add("screen.xp_simplifier.config.row.smeltingXpMode.tooltip", "VANILLA - domyślne wartości XP" + System.lineSeparator()
                + "ON - włącz niestandardowe XP (wartości z tabel JSON)" + System.lineSeparator()
                + "OFF - wyłącz drop XP z przepalania/gotowania całkowicie");

        // Multiplier row labels
        add("screen.xp_simplifier.config.row.entityXpMultiplier", "Mnożnik XP - moby");
        add("screen.xp_simplifier.config.row.blockXpMultiplier", "Mnożnik XP - bloki");
        add("screen.xp_simplifier.config.row.cropXpMultiplier", "Mnożnik XP - uprawy");
        add("screen.xp_simplifier.config.row.smeltingXpMultiplier", "Mnożnik XP - piecyki");
        add("screen.xp_simplifier.config.row.tradingXpMultiplier", "Mnożnik XP - handel (gracz)");
        add("screen.xp_simplifier.config.row.merchantXpMultiplier", "Mnożnik XP - handel (handlarz)");
        add("screen.xp_simplifier.config.row.breedingXpMultiplier", "Mnożnik XP - rozmnażanie");
        add("screen.xp_simplifier.config.row.fishingXpMultiplier", "Mnożnik XP - łowienie");
        add("screen.xp_simplifier.config.row.grindstoneXpMultiplier", "Mnożnik XP za enchant (szlifierka)");

        // Tooltip range templates
        add("screen.xp_simplifier.config.multiplier.range", "%s  (0.01 – 1000.0)");
        add("screen.xp_simplifier.config.int.range", "%s  (%s – %s)");

        // Mending page
        add("screen.xp_simplifier.config.row.xpRepairEnabled", "Naprawa za XP");
        add("screen.xp_simplifier.config.row.durabilityPerPoint", "Punkty wytrzymałości na XP punkt");
        add("screen.xp_simplifier.config.row.maxAnvilRepairCost", "Maksymalny koszt naprawy");
        add("screen.xp_simplifier.config.mending.button", "%s");
        add("screen.xp_simplifier.config.mending.on", "WŁ.");
        add("screen.xp_simplifier.config.mending.off", "WYŁ.");
        add("screen.xp_simplifier.config.mending.tooltip", "Włącza naprawę przedmiotów za XP w kowadle, jeśli posiadają Mending.");

        // Maps cat
        add("screen.xp_simplifier.config.maps.label.blocks", "Bloki");
        add("screen.xp_simplifier.config.maps.label.crops", "Uprawy");
        add("screen.xp_simplifier.config.maps.label.entities", "Jednostki");
        add("screen.xp_simplifier.config.maps.label.smelting", "Piecyki");
        add("screen.xp_simplifier.config.maps.label.trading", "Handel");
        add("screen.xp_simplifier.config.maps.label.breeding", "Rozmnażanie");
        add("screen.xp_simplifier.config.maps.label.fishing", "Łowienie");
        add("screen.xp_simplifier.config.maps.label.grindstone", "Szlifierka");

        // Maps page
        add("screen.xp_simplifier.config.maps.search_box", "Wyszukaj");
        add("screen.xp_simplifier.config.maps.search_box_tooltip", "Szukaj po mod_id/id/słowo-klucz");
        add("screen.xp_simplifier.config.maps.label.countKey", "Pozycje: ");
        add("screen.xp_simplifier.config.maps.back", "← Wstecz");
        add("screen.xp_simplifier.config.maps.edit", "Edytuj");
        add("screen.xp_simplifier.config.maps.add.type", "Typ XP");
        add("screen.xp_simplifier.config.maps.add", "+ Dodaj");
        add("screen.xp_simplifier.config.maps.add.confirm", "Potwierdź");
        add("screen.xp_simplifier.config.maps.add.category", "Kategoria");
        add("screen.xp_simplifier.config.maps.add.key", "Nazwa (modid:id)");
        add("screen.xp_simplifier.config.maps.type.button", "%s");
        add("screen.xp_simplifier.config.maps.label.type", "Typ");
        add("screen.xp_simplifier.config.maps.label.min", "Min");
        add("screen.xp_simplifier.config.maps.label.max", "Max");
        add("screen.xp_simplifier.config.maps.label.float", "Liczba dziesiętna");
        add("screen.xp_simplifier.config.maps.add.type.random", "Losowe");
        add("screen.xp_simplifier.config.maps.add.type.fixed", "Stałe");
        add("screen.xp_simplifier.config.maps.label.tradingPlayerXp", "Gracz");
        add("screen.xp_simplifier.config.maps.label.merchantXp", "Handlarz");

        // netwroking
        add("network.xp_simplifier.config.invalid_packet_received", "[XPS] Otrzymano nieprawidłowy pakiet konfiguracyjny. Najprawdopodobniej nie masz uprawnień do edytowania konfigu, jeśli problem się utrzymuje -> skontaktuj się z administracją lub zgłoś problem na GitHubie.");

        // commands
        add("message.xp_simplifier.command.edit_permission.xps_label", "§l[XPS]§r ");
        add("message.xp_simplifier.config.applied", "§aGracz§r %s §azaaktualizował konfig.");
        add("message.xp_simplifier.command.edit_permission.list.empty", "Lista graczy z uprawnieniami jest pusta.");
        add("message.xp_simplifier.command.edit_permission.list", "Lista graczy z uprawnieniami edycji (%s):");
        add("message.xp_simplifier.command.edit_permission.mapview.on", "§aWłączona");
        add("message.xp_simplifier.command.edit_permission.mapview.off", "§cWyłączona");
        add("message.xp_simplifier.command.edit_permission.can.on", "§aNadano uprawnienia edycji graczowi§r %s");
        add("message.xp_simplifier.command.edit_permission.can.off", "§cZabrano uprawnienia edycji graczowi§r %s");
        add("message.xp_simplifier.command.edit_permission.delete", "§cZabrano uprawnienia edycji graczowi§r %s"
                + System.lineSeparator() + "By nadać uprawnienia graczowi,"
                + System.lineSeparator() + "użyj → /xps add <player> <true>");
        add("message.xp_simplifier.command.edit_permission.mapview", "Aktualny status podglądu map: %s");
        add("message.xp_simplifier.command.edit_permission.notfound", "Nie znaleziono gracza %s");
        add("message.xp_simplifier.command.edit_permission.exception", "Błąd podczas próby uzyskania świata. Upewnij się, że świat jest poprawnie załadowany i spróbuj ponownie.");

    }
}