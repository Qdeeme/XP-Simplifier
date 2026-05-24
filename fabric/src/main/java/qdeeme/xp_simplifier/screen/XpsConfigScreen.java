/*
 * Copyright (C) 2026 Qdeeme.
 *
 * This file is part of "Xp Simplifier".
 *
 * "Xp Simplifier" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */



package qdeeme.xp_simplifier.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import qdeeme.xp_simplifier.network.ConfigUpdate;
import qdeeme.xp_simplifier.util.Config;
import qdeeme.xp_simplifier.util.ConfigServerSync;
import qdeeme.xp_simplifier.util.CropXpMode;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;

import java.util.*;

/**
 * In-game config screen (Fabric port of NeoForge ConfigScreen).
 *
 * <p>Uses the same virtual-pixel / scale design as the NeoForge version.
 * Sends {@code ConfigUpdate} C2S packets when in-game; writes directly to
 * master config files when on the main menu.</p>
 */
public class XpsConfigScreen extends Screen {

    // filter searchbox
    private final List<String> allKeys = new ArrayList<>();
    private final List<String> filteredKeys = new ArrayList<>();
    private String currentFilter = "";

    // ── Scale ─────────────────────────────────────────────────────────────────
    private static final float SCALE = 0.7f;

    private int vw() {
        return (int) (width / SCALE);
    }

    private int vcx() {
        return vw() / 2;
    }

    private int s(int v) {
        return Math.round(v * SCALE);
    }

    // ── Translation keys ──────────────────────────────────────────────────────
    private static final String TITLE = "screen.xp_simplifier.config.title";
    private static final String APPLY = "screen.xp_simplifier.config.apply";
    private static final String CANCEL = "screen.xp_simplifier.config.cancel";
    private static final String HINT_OP = "screen.xp_simplifier.config.hint.op";
    private static final String HINT_NON_OP = "screen.xp_simplifier.config.hint.readonly";
    private static final String HINT_MAIN = "screen.xp_simplifier.config.hint.main";

    private static final String TAB_MODES = "screen.xp_simplifier.config.tab.modes";
    private static final String TAB_MULTS = "screen.xp_simplifier.config.tab.multipliers";
    private static final String TAB_MENDING = "screen.xp_simplifier.config.tab.mending";
    private static final String TAB_MAPS = "screen.xp_simplifier.config.tab.maps";

    private static final String[] MODE_KEYS = {
            "screen.xp_simplifier.config.row.orbMode",
            "screen.xp_simplifier.config.row.entityXpMode",
            "screen.xp_simplifier.config.row.blockXpMode",
            "screen.xp_simplifier.config.row.cropXpMode",
            "screen.xp_simplifier.config.row.breedingXpMode",
            "screen.xp_simplifier.config.row.fishingXpMode",
            "screen.xp_simplifier.config.row.tradingPlayerXp",
            "screen.xp_simplifier.config.row.merchantXp",
            "screen.xp_simplifier.config.row.grindstoneXpMode",
            "screen.xp_simplifier.config.row.smeltingXpMode"
    };
    private static final String[] MULTS_KEYS = {
            "screen.xp_simplifier.config.row.entityXpMultiplier",
            "screen.xp_simplifier.config.row.blockXpMultiplier",
            "screen.xp_simplifier.config.row.cropXpMultiplier",
            "screen.xp_simplifier.config.row.smeltingXpMultiplier",
            "screen.xp_simplifier.config.row.tradingXpMultiplier",
            "screen.xp_simplifier.config.row.merchantXpMultiplier",
            "screen.xp_simplifier.config.row.breedingXpMultiplier",
            "screen.xp_simplifier.config.row.fishingXpMultiplier",
            "screen.xp_simplifier.config.row.grindstoneXpMultiplier"
    };
    private static final String[] MENDING_KEYS = {
            "screen.xp_simplifier.config.row.xpRepairEnabled",
            "screen.xp_simplifier.config.row.durabilityPerPoint",
            "screen.xp_simplifier.config.row.maxAnvilRepairCost"
    };
    private static final String MENDING_BTN = "screen.xp_simplifier.config.mending.button";
    private static final String MENDING_ON = "screen.xp_simplifier.config.mending.on";
    private static final String MENDING_OFF = "screen.xp_simplifier.config.mending.off";
    private static final String MENDING_TOOLTIP = "screen.xp_simplifier.config.mending.tooltip";
    private static final String K_MULT_RANGE = "screen.xp_simplifier.config.multiplier.range";
    private static final String K_INT_RANGE = "screen.xp_simplifier.config.int.range";

    private static final String[] MAP_KEYS = {"Blocks", "Crops", "Entities", "Smelting", "Trading", "Breeding", "Fishing", "Grindstone"};
    private static final String[] MAP_KEYS_LBL = {
            "screen.xp_simplifier.config.maps.label.blocks",
            "screen.xp_simplifier.config.maps.label.crops",
            "screen.xp_simplifier.config.maps.label.entities",
            "screen.xp_simplifier.config.maps.label.smelting",
            "screen.xp_simplifier.config.maps.label.trading",
            "screen.xp_simplifier.config.maps.label.breeding",
            "screen.xp_simplifier.config.maps.label.fishing",
            "screen.xp_simplifier.config.maps.label.grindstone"
    };
    private static final String SEARCH = "screen.xp_simplifier.config.maps.search_box";
    private static final String SEARCH_BOX = "screen.xp_simplifier.config.maps.search_box_tooltip";
    private static final String COUNT_KEY_LBL = "screen.xp_simplifier.config.maps.label.countKey";
    private static final String MAPS_EDIT_BTN = "screen.xp_simplifier.config.maps.edit";
    private static final String MAPS_ADD_BTN = "screen.xp_simplifier.config.maps.add";
    private static final String MAPS_ADD_CONFIRM = "screen.xp_simplifier.config.maps.add.confirm";
    private static final String MAPS_BACK_BTN = "screen.xp_simplifier.config.maps.back";
    private static final String MAPS_ADD_CAT_LBL = "screen.xp_simplifier.config.maps.add.category";
    private static final String MAPS_ADD_KEY_LBL = "screen.xp_simplifier.config.maps.add.key";
    private static final String MAPS_ADD_TYPE_LBL = "screen.xp_simplifier.config.maps.add.type";
    private static final String TYPE_BTN = "screen.xp_simplifier.config.maps.type.button";
    private static final String TYPE = "screen.xp_simplifier.config.maps.label.type";
    private static final String MIN = "screen.xp_simplifier.config.maps.label.min";
    private static final String MAX = "screen.xp_simplifier.config.maps.label.max";
    private static final String FLOAT = "screen.xp_simplifier.config.maps.label.float";
    private static final String RANDOM_BTN = "screen.xp_simplifier.config.maps.add.type.random";
    private static final String FIXED_BTN = "screen.xp_simplifier.config.maps.add.type.fixed";
    private static final String TR_P_LABEL = "screen.xp_simplifier.config.maps.label.tradingPlayerXp";
    private static final String TR_M_LABEL = "screen.xp_simplifier.config.maps.label.merchantXp";

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int TEXT_X_OFFSET = -160;
    private static final int KEYS_OFFSET_X = 50;
    private static final int CAT_H = 26;
    private static final int DETAIL_FIRST_ROW_V = 107;
    private static final int ER_TYPE_X = 20;
    private static final int ER_TYPE_W = 70;
    private static final int ER_VAL1_X = 105;
    private static final int ER_VAL2_X = 165;
    private static final int ER_VAL_W = 50;
    private static final int ER_DEL_X = 250;
    private static final int ER_DEL_W = 24;
    private static final int TR_P_TYPE_X = -100;
    private static final int TR_P_MIN_X = -30;
    private static final int TR_P_MAX_X = 19;
    private static final int TR_M_TYPE_X = 67;
    private static final int TR_M_MIN_X = 137;
    private static final int TR_M_MAX_X = 187;
    private static final int TR_VAL_W = 45;

    private static final int PAGE_MODES = 0, PAGE_MULTIPLIERS = 1, PAGE_MENDING = 2, PAGE_MAPS = 3;
    private int currentPage = PAGE_MODES;
    private final int[] pageScrollV = {0, 0, 0, 0};

    private static final int TAB_Y = 40;
    private static final int TAB_W = 120;
    private static final int TAB_H = 25;
    private static final int PANEL_TOP_V = 75;
    private static final int PANEL_BOTTOM_END_V = 38;
    private static final int PANEL_X_MARGIN = 50;
    private static final int ROW_H = 40;
    private static final int INNER_PAD_V = 8;
    private static final int FIRST_ROW_V = PANEL_TOP_V + INNER_PAD_V;
    private static final int WIDGET_X_OFFSET = 20;
    private static final int WIDGET_W = 120;
    private static final int WIDGET_H = 22;
    private static final int BTN_W = 90;
    private static final int BTN_H = 22;
    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_GAP = 3;

    private int panelLeft() {
        return s(PANEL_X_MARGIN);
    }

    private int panelRight() {
        return width - s(PANEL_X_MARGIN) - s(SCROLLBAR_W) - s(SCROLLBAR_GAP);
    }

    private int panelTop() {
        return s(PANEL_TOP_V);
    }

    private int panelBottom() {
        return height - s(PANEL_BOTTOM_END_V);
    }

    private int panelHeight() {
        return panelBottom() - panelTop();
    }

    private int contentVirtualHeight = 0;

    private int maxScrollV() {
        int v = (int) (panelHeight() / SCALE);
        return Math.max(0, contentVirtualHeight - v + INNER_PAD_V);
    }

    private int scrollV() {
        return pageScrollV[currentPage];
    }

    private void scrollV(int v) {
        pageScrollV[currentPage] = Math.max(0, Math.min(v, maxScrollV()));
        syncWidgetPositions();
    }

    // ── Widget tracking ───────────────────────────────────────────────────────
    /**
     * Scroll-panel widgets.
     */
    private final List<ClickableWidget> scrollWidgets = new ArrayList<>();
    private final List<Integer> baseYList = new ArrayList<>();
    /**
     * Fixed (non-scroll) widgets rendered outside the scissored panel.
     */
    private final List<ClickableWidget> fixedWidgets = new ArrayList<>();

    private void addScrollWidget(ClickableWidget w) {
        baseYList.add(w.getY());
        scrollWidgets.add(w);
    }

    private <W extends ClickableWidget> void addFixed(W w) {
        fixedWidgets.add(w);
        addDrawableChild(w);
    }

    private void syncWidgetPositions() {
        int scrollPx = s(scrollV()), top = panelTop(), bottom = panelBottom();
        for (int i = 0; i < scrollWidgets.size(); i++) {
            ClickableWidget w = scrollWidgets.get(i);
            int ny = baseYList.get(i) - scrollPx;
            w.setY(ny);
            w.visible = ny + w.getHeight() > top && ny < bottom;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private final Map<String, Object> pending = new LinkedHashMap<>();
    private int selectedMapIdx = -1;
    private boolean addingEntry = false;
    private int addCatIdx = 0;
    private boolean addIsRandom = false, addPRandom = false, addMRandom = false;
    private final int[] addVals = {0, 0, 0, 0};
    private float addFloatVal = 0f;
    private String addKeyStr = "";

    private boolean lastReadOnly = false;
    private boolean lastMapViewEnabled = true;

    public XpsConfigScreen(Screen parent) {
        super(Text.translatable(TITLE));
        this.parent = parent;
        pending.putAll(ConfigServerSync.getClientShadow());
    }

    private boolean isReadOnly() {
        if (client == null || client.player == null) return false;
        if (client.player.hasPermissionLevel(2) && Boolean.TRUE.equals(ConfigServerSync.clientExplicit())) {
            return false;
        } else {
            return !ConfigServerSync.clientCanEdit();
        }
    }

    // ── Background ────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float pt) {
        ctx.fill(0, 0, width, height, 0xC8101010);
        int pl = panelLeft();
        int pt2 = panelTop();
        int pr = panelRight() + s(SCROLLBAR_W) + s(SCROLLBAR_GAP);
        int pb = panelBottom();
        ctx.fill(pl, pt2, pr, pb, 0xFF181818);
        ctx.drawBorder(pl, pt2, pr - pl, pb - pt2, 0xFF3A3A3A);
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        scrollWidgets.clear();
        baseYList.clear();
        fixedWidgets.clear();

        boolean readOnly = isReadOnly();
        lastReadOnly = readOnly;
        lastMapViewEnabled = Config.isMapViewEnabled();
        int vcx = vcx();

        // Tabs
        addTabBtn(TAB_MODES, PAGE_MODES, vcx - 2 * TAB_W - 10);
        addTabBtn(TAB_MULTS, PAGE_MULTIPLIERS, vcx - TAB_W - 5);
        addTabBtn(TAB_MENDING, PAGE_MENDING, vcx);
        addTabBtn(TAB_MAPS, PAGE_MAPS, vcx + TAB_W + 5);

        switch (currentPage) {
            case PAGE_MODES -> initModesPage(vcx);
            case PAGE_MULTIPLIERS -> initMultipliersPage(vcx);
            case PAGE_MENDING -> initMendingPage();
            case PAGE_MAPS -> {
                if (selectedMapIdx >= 0) {
                    if (addingEntry) {
                        addFixed(ButtonWidget.builder(Text.translatable(MAPS_BACK_BTN),
                                        btn -> {
                                            addingEntry = false;
                                            clearAndInit();
                                        })
                                .dimensions(panelLeft() + 10, panelTop() + 2, s(BTN_W), s(18)).build());
                        initMapsAddPage(vcx);
                    } else {
                        addFixed(SearchBox());
                        addFixed(ButtonWidget.builder(Text.translatable(MAPS_BACK_BTN),
                                        btn -> {
                                            selectedMapIdx = -1;
                                            clearAndInit();
                                        })
                                .dimensions(panelLeft() + 10, panelTop() + 2, s(BTN_W), s(18)).build());
                        ButtonWidget addBtn = ButtonWidget.builder(Text.translatable(MAPS_ADD_BTN),
                                btn -> {
                                    addingEntry = true;
                                    addCatIdx = 0;
                                    addIsRandom = false;
                                    addPRandom = false;
                                    addMRandom = false;
                                    addVals[0] = addVals[1] = addVals[2] = addVals[3] = 0;
                                    addFloatVal = 0f;
                                    addKeyStr = "";
                                    clearAndInit();
                                }).dimensions(panelRight() - s(BTN_W) - 5, panelTop() + 2, s(BTN_W), s(18)).build();
                        addBtn.active = !readOnly;
                        addFixed(addBtn);
                        initMapsDetailPage(vcx);
                    }
                } else {
                    initMapsOverviewPage(vcx);
                }
            }
        }

        // Register  widgets with Minecraft
        for (ClickableWidget w : scrollWidgets) addDrawableChild(w);

        // Disable widgets per permission state
        if (currentPage == PAGE_MAPS && selectedMapIdx < 0) {
            if (!Config.isMapViewEnabled() && readOnly) scrollWidgets.forEach(w -> w.active = false);
        } else if (currentPage == PAGE_MAPS) {
            if (readOnly) scrollWidgets.forEach(w -> w.active = false);
        } else {
            if (readOnly) scrollWidgets.forEach(w -> w.active = false);
        }

        syncWidgetPositions();

        // Apply / Cancel (fixed, below panel)
        ButtonWidget applyBtn = ButtonWidget.builder(Text.translatable(APPLY), btn -> applyChanges())
                .dimensions(s(vcx - 250), height - s(28), s(BTN_W), s(BTN_H)).build();
        applyBtn.active = !readOnly;
        addFixed(applyBtn);
        addFixed(ButtonWidget.builder(Text.translatable(CANCEL), btn -> close())
                .dimensions(s(vcx + 150), height - s(28), s(BTN_W), s(BTN_H)).build());
    }

    private void addTabBtn(String labelKey, int page, int vx) {
        ButtonWidget btn = ButtonWidget.builder(Text.translatable(labelKey), b -> {
            currentPage = page;
            if (page != PAGE_MAPS) addingEntry = false;
            clearAndInit();
        }).dimensions(s(vx), s(TAB_Y), s(TAB_W), s(TAB_H)).build();
        if (currentPage == page) btn.active = false;
        addFixed(btn);
    }

    // searchbox
    private TextFieldWidget SearchBox() {
        TextFieldWidget searchBox = new TextFieldWidget(textRenderer, (width / 2) - (3 * s(BTN_W) / 2), panelTop() + 2, s(BTN_W) * 3, s(18), Text.empty());
        searchBox.setText(currentFilter);
        searchBox.setMaxLength(128);
        searchBox.setPlaceholder(Text.translatable(SEARCH));
        searchBox.setTooltip(Tooltip.of(Text.translatable(SEARCH_BOX)));
        searchBox.setChangedListener(text -> {
            currentFilter = text.toLowerCase(Locale.ROOT);
            filterKeys(currentFilter);
            scrollV(0);
            rebuildMapsPage();
        });
        return searchBox;
    }

    // filter helper
    private void rebuildMapsPage() {
        boolean readOnly = isReadOnly();
        lastReadOnly = readOnly;


        for (ClickableWidget w : scrollWidgets) remove(w);
        scrollWidgets.clear();
        baseYList.clear();

        int vcx = vcx();
        if (selectedMapIdx >= 0 && !addingEntry) initMapsDetailPage(vcx);
        else if (selectedMapIdx >= 0) initMapsAddPage(vcx);
        else initMapsOverviewPage(vcx);

        for (ClickableWidget w : scrollWidgets) addDrawableChild(w);

        if (currentPage == PAGE_MAPS) {
            if (readOnly) scrollWidgets.forEach(w -> w.active = false);
        } else {
            if (readOnly) scrollWidgets.forEach(w -> w.active = false);
        }

        syncWidgetPositions();
    }

    // search box logic: filter allKeys into filteredKeys based on query, with an optimization for incremental typing
    private void filterKeys(String query) {
        query = query.toLowerCase(Locale.ROOT);

        filteredKeys.clear();

        List<String> startsWith = new ArrayList<>();
        List<String> contains = new ArrayList<>();

        for (String key : allKeys) {
            String lower = key.toLowerCase(Locale.ROOT);

            if (lower.startsWith(query)) {
                startsWith.add(key);
            } else if (lower.contains(query)) {
                contains.add(key);
            }
        }

        filteredKeys.addAll(startsWith);
        filteredKeys.addAll(contains);
    }

    private boolean matchesSearch(String key) {
        if (currentFilter.isBlank()) {
            return true;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains(currentFilter);
    }

    // ── Page builders ─────────────────────────────────────────────────────────

    private void initModesPage(int vcx) {
        String[] cfgKeys = {"orbMode", "entityXpMode", "blockXpMode", "cropXpMode",
                "breedingXpMode", "fishingXpMode", "tradingPlayerXp", "merchantXp", "grindstoneXpMode", "smeltingXpMode"};
        Enum<?>[][] valSets = {
                OrbMode.values(), XpMode.values(), XpMode.values(), CropXpMode.values(),
                XpMode.values(), XpMode.values(), XpMode.values(), XpMode.values(), XpMode.values(), XpMode.values()
        };
        for (int i = 0; i < cfgKeys.length; i++)
            addEnumRow(vcx + KEYS_OFFSET_X, FIRST_ROW_V + i * ROW_H, cfgKeys[i], MODE_KEYS[i], valSets[i]);
        contentVirtualHeight = cfgKeys.length * ROW_H + INNER_PAD_V * 2;
    }

    private void initMultipliersPage(int vcx) {
        String[] cfgKeys = {"entityXpMultiplier", "blockXpMultiplier", "cropXpMultiplier",
                "smeltingXpMultiplier", "tradingXpMultiplier", "merchantXpMultiplier",
                "breedingXpMultiplier", "fishingXpMultiplier", "grindstoneXpMultiplier"};
        for (int i = 0; i < cfgKeys.length; i++)
            addFloatRow(vcx + KEYS_OFFSET_X, FIRST_ROW_V + i * ROW_H, cfgKeys[i], MULTS_KEYS[i]);
        contentVirtualHeight = cfgKeys.length * ROW_H + INNER_PAD_V * 2;
    }

    private void initMendingPage() {
        int vy = FIRST_ROW_V;
        boolean enabled = pending.get("xpRepairEnabled") instanceof Boolean b ? b : true;
        final boolean[] flag = {enabled};
        ButtonWidget toggleBtn = ButtonWidget.builder(
                Text.translatable(MENDING_BTN, Text.translatable(flag[0] ? MENDING_ON : MENDING_OFF)),
                btn -> {
                    flag[0] = !flag[0];
                    pending.put("xpRepairEnabled", flag[0]);
                    btn.setMessage(Text.translatable(MENDING_BTN, Text.translatable(flag[0] ? MENDING_ON : MENDING_OFF)));
                }).dimensions(s(vcx() + WIDGET_X_OFFSET) + KEYS_OFFSET_X, s(vy), s(WIDGET_W), s(WIDGET_H)).build();
        toggleBtn.setTooltip(Tooltip.of(Text.translatable(MENDING_TOOLTIP)));
        addScrollWidget(toggleBtn);
        addFloatRow(vcx() + KEYS_OFFSET_X, vy + ROW_H, MENDING_KEYS[1], 0.01f, 100000f);
        addIntRow(vcx() + KEYS_OFFSET_X, vy + 2 * ROW_H, MENDING_KEYS[2], 0, 9999);
        contentVirtualHeight = 3 * ROW_H + INNER_PAD_V * 2;
    }

    // ── Row builders ──────────────────────────────────────────────────────────

    private void addEnumRow(int vcx, int vy, String cfgKey, String labelKey, Enum<?>[] values) {
        String cur = pending.get(cfgKey) instanceof String sv ? sv
                : (pending.get(cfgKey) != null ? pending.get(cfgKey).toString() : values[0].name());
        int startIdx = 0;
        for (int i = 0; i < values.length; i++)
            if (values[i].name().equals(cur)) {
                startIdx = i;
                break;
            }
        final int[] idx = {startIdx};
        ButtonWidget btn = ButtonWidget.builder(Text.literal(values[idx[0]].name()), b -> {
            idx[0] = (idx[0] + 1) % values.length;
            b.setMessage(Text.literal(values[idx[0]].name()));
            pending.put(cfgKey, values[idx[0]].name());
        }).dimensions(s(vcx + (WIDGET_X_OFFSET * 2)), s(vy), s(WIDGET_W), s(WIDGET_H)).build();
        btn.setTooltip(Tooltip.of(Text.translatable(labelKey + ".tooltip")));
        addScrollWidget(btn);
    }

    private void addFloatRow(int vcx, int vy, String cfgKey, String labelKey) {
        Object val = pending.get(cfgKey);
        String strV = val instanceof Number n ? String.format("%.2f", n.floatValue()) : "1.0000";
        TextFieldWidget box = new TextFieldWidget(textRenderer, s(vcx + (WIDGET_X_OFFSET * 2)), s(vy), s(WIDGET_W), s(WIDGET_H), Text.translatable(labelKey));
        box.setText(strV);
        box.setMaxLength(16);
        box.setTooltip(Tooltip.of(Text.translatable(K_MULT_RANGE, Text.translatable(labelKey))));
        box.setChangedListener(sv -> {
            try {
                float f = Float.parseFloat(sv);
                if (f >= 0.01f && f <= 1000f) pending.put(cfgKey, f);
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(box);
    }

    private void addIntRow(int vcx, int vy, String labelKey, int min, int max) {
        Object val = pending.get("maxAnvilRepairCost");
        String strV = val instanceof Number n ? String.valueOf(n.intValue()) : "0";
        TextFieldWidget box = new TextFieldWidget(textRenderer, s(vcx + (WIDGET_X_OFFSET * 2)), s(vy), s(WIDGET_W), s(WIDGET_H), Text.translatable(labelKey));
        box.setText(strV);
        box.setMaxLength(8);
        box.setTooltip(Tooltip.of(Text.translatable(K_INT_RANGE, Text.translatable(labelKey), String.valueOf(min), String.valueOf(max))));
        box.setChangedListener(sv -> {
            try {
                int v = Integer.parseInt(sv);
                if (v >= min && v <= max) pending.put("maxAnvilRepairCost", v);
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(box);
    }

    private void addFloatRow(int vcx, int vy, String labelKey, float min, float max) {
        Object val = pending.get("durabilityPerPoint");
        String strV = val instanceof Number n ? String.valueOf(n.floatValue()) : "1.0";
        TextFieldWidget box = new TextFieldWidget(textRenderer, s(vcx + (WIDGET_X_OFFSET * 2)), s(vy), s(WIDGET_W), s(WIDGET_H), Text.translatable(labelKey));
        box.setText(strV);
        box.setMaxLength(6);
        box.setTooltip(Tooltip.of(Text.translatable(K_INT_RANGE, Text.translatable(labelKey), String.valueOf(min), String.valueOf(max))));
        box.setChangedListener(sv -> {
            try {
                float v = Float.parseFloat(sv);
                if (v >= min && v <= max) pending.put("durabilityPerPoint", v);
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(box);
    }

    // ── Maps overview ─────────────────────────────────────────────────────────

    private void initMapsOverviewPage(int vcx) {
        for (int i = 0; i < MAP_KEYS.length; i++) {
            final int idx = i;
            addScrollWidget(ButtonWidget.builder(Text.translatable(MAPS_EDIT_BTN),
                            b -> {
                                selectedMapIdx = idx;
                                clearAndInit();
                            })
                    .dimensions(s(vcx + 100), s(FIRST_ROW_V + i * ROW_H + (ROW_H - BTN_H) / 2) - 4, s(BTN_W), s(BTN_H)).build());
        }
        contentVirtualHeight = MAP_KEYS.length * ROW_H + INNER_PAD_V * 2;
    }

    // ── Maps detail ───────────────────────────────────────────────────────────

    private void initMapsDetailPage(int vcx) {
        String mapName = MAP_KEYS[selectedMapIdx];
        Object mapData = pending.get(mapName);
        if (!(mapData instanceof Map<?, ?> outerMap)) {
            contentVirtualHeight = DETAIL_FIRST_ROW_V + ROW_H;
            return;
        }

        // searchbox
        if (currentFilter.isEmpty()) {
            filteredKeys.clear();
        }
        // Populate allKeys for search filtering
        allKeys.clear();
        for (Map.Entry<?, ?> catEntry : outerMap.entrySet()) {
            for (Map.Entry<String, Object> entry :
                    getEffectiveEntries(mapName, (String) catEntry.getKey())) {
                allKeys.add(entry.getKey());
            }
        }


        int vy = DETAIL_FIRST_ROW_V;
        for (Map.Entry<?, ?> catEntry : outerMap.entrySet()) {
            String catName = (String) catEntry.getKey();

            // Filter once, reuse for BOTH the skip-check AND the widget loop
            List<Map.Entry<String, Object>> entries = getEffectiveEntries(mapName, catName)
                    .stream()
                    .filter(e -> matchesSearch(e.getKey()))
                    .toList();
            if (entries.isEmpty()) continue;

            vy += CAT_H;

            // ✅ Use filtered `entries`, not getEffectiveEntries() again
            for (Map.Entry<String, Object> entry : entries) {
                switch (mapName) {
                    case "Smelting" -> addSmeltingEntryRow(vcx, vy, mapName, catName, entry.getKey(), entry.getValue());
                    case "Trading" -> addTradingEntryRow(vcx, vy, mapName, catName, entry.getKey(), entry.getValue());
                    default -> addXpValueEntryRow(vcx, vy, mapName, catName, entry.getKey(), entry.getValue());
                }
                vy += ROW_H;
            }
        }
        contentVirtualHeight = vy - FIRST_ROW_V + INNER_PAD_V;
    }

    private void addXpValueEntryRow(int vcx, int vy, String mapName, String catName, String entryKey, Object value) {
        boolean isRandom = false;
        int v1 = 0, v2 = 0;
        if (value instanceof Map<?, ?> m) {
            String t = m.get("type") instanceof String s2 ? s2 : "Fixed";
            isRandom = "Random".equalsIgnoreCase(t) || "RANDOM".equalsIgnoreCase(t);
            v1 = isRandom ? (m.get("min") instanceof Number n ? n.intValue() : 0) : (m.get("fixed") instanceof Number n ? n.intValue() : 0);
            v2 = m.get("max") instanceof Number n ? n.intValue() : 0;
        }
        final boolean[] rand = {isRandom};
        final int[] vals = {v1, v2};
        ButtonWidget typeBtn = ButtonWidget.builder(Text.translatable(TYPE_BTN, Text.translatable(rand[0] ? RANDOM_BTN : FIXED_BTN)), btn -> {
            rand[0] = !rand[0];
            btn.setMessage(Text.translatable(TYPE_BTN, Text.translatable(rand[0] ? RANDOM_BTN : FIXED_BTN)));
            accumulateXpEdit(mapName, catName, entryKey, rand[0], vals[0], vals[1]);
            clearAndInit();
        }).dimensions(s(vcx + ER_TYPE_X), s(vy), s(ER_TYPE_W), s(WIDGET_H)).build();
        addScrollWidget(typeBtn);
        TextFieldWidget val1Box = new TextFieldWidget(textRenderer, s(vcx + ER_VAL1_X), s(vy), s(ER_VAL_W - 5), s(WIDGET_H), Text.empty());
        val1Box.setText(String.valueOf(vals[0]));
        val1Box.setMaxLength(6);
        val1Box.setChangedListener(sv -> {
            try {
                vals[0] = Integer.parseInt(sv.trim());
                accumulateXpEdit(mapName, catName, entryKey, rand[0], vals[0], vals[1]);
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(val1Box);
        if (isRandom) {
            TextFieldWidget val2Box = new TextFieldWidget(textRenderer, s(vcx + ER_VAL2_X), s(vy), s(ER_VAL_W - 5), s(WIDGET_H), Text.empty());
            val2Box.setText(String.valueOf(vals[1]));
            val2Box.setMaxLength(6);
            val2Box.setChangedListener(sv -> {
                try {
                    vals[1] = Integer.parseInt(sv.trim());
                    accumulateXpEdit(mapName, catName, entryKey, rand[0], vals[0], vals[1]);
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(val2Box);
        }
        ButtonWidget delBtn = ButtonWidget.builder(Text.literal("×"), btn -> {
                    accumulateMapEdit(mapName, catName, entryKey, null);
                    clearAndInit();
                })
                .dimensions(s(vcx + ER_DEL_X), s(vy), s(ER_DEL_W), s(WIDGET_H)).build();
        addScrollWidget(delBtn);
    }

    private void addSmeltingEntryRow(int vcx, int vy, String mapName, String catName, String entryKey, Object value) {
        float xp = value instanceof Number n ? n.floatValue() : 0f;
        TextFieldWidget xpBox = new TextFieldWidget(textRenderer, s(vcx + ER_VAL1_X), s(vy), s(ER_VAL_W + 15), s(WIDGET_H), Text.empty());
        xpBox.setText(String.format("%.2f", xp));
        xpBox.setMaxLength(8);
        xpBox.setChangedListener(sv -> {
            try {
                float f = Float.parseFloat(sv.trim());
                accumulateMapEdit(mapName, catName, entryKey, f);
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(xpBox);
        addScrollWidget(ButtonWidget.builder(Text.literal("×"), btn -> {
                    accumulateMapEdit(mapName, catName, entryKey, null);
                    clearAndInit();
                })
                .dimensions(s(vcx + ER_DEL_X), s(vy), s(ER_DEL_W), s(WIDGET_H)).build());
    }

    private void addTradingEntryRow(int vcx, int vy, String mapName, String catName, String entryKey, Object value) {
        boolean pRand = false, mRand = false;
        int pMin = 0, pMax = 0, mMin = 0, mMax = 0;
        if (value instanceof Map<?, ?> tv) {
            if (tv.get("playerXp") instanceof Map<?, ?> p) {
                String t = p.get("type") instanceof String s2 ? s2 : "Fixed";
                pRand = "Random".equalsIgnoreCase(t);
                pMin = p.get(pRand ? "min" : "fixed") instanceof Number n ? n.intValue() : 0;
                pMax = pRand ? (p.get("max") instanceof Number n ? n.intValue() : 0) : pMin;
            }
            if (tv.get("merchantXp") instanceof Map<?, ?> m) {
                String t = m.get("type") instanceof String s2 ? s2 : "Fixed";
                mRand = "Random".equalsIgnoreCase(t);
                mMin = m.get(mRand ? "min" : "fixed") instanceof Number n ? n.intValue() : 0;
                mMax = mRand ? (m.get("max") instanceof Number n ? n.intValue() : 0) : mMin;
            }
        }
        final boolean[] pr = {pRand};
        final boolean[] mr = {mRand};
        final int[] pv = {pMin, pMax};
        final int[] mv = {mMin, mMax};
        ButtonWidget pTypeBtn = ButtonWidget.builder(Text.translatable(TYPE_BTN, Text.translatable(pr[0] ? RANDOM_BTN : FIXED_BTN)), btn -> {
            pr[0] = !pr[0];
            btn.setMessage(Text.translatable(TYPE_BTN, Text.translatable(pr[0] ? RANDOM_BTN : FIXED_BTN)));
            accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
            clearAndInit();
        }).dimensions(s(vcx + TR_P_TYPE_X), s(vy), s(ER_TYPE_W), s(WIDGET_H)).build();
        addScrollWidget(pTypeBtn);
        TextFieldWidget pMinBox = new TextFieldWidget(textRenderer, s(vcx + TR_P_MIN_X), s(vy), s(TR_VAL_W), s(WIDGET_H), Text.empty());
        pMinBox.setText(String.valueOf(pv[0]));
        pMinBox.setMaxLength(6);
        pMinBox.setChangedListener(sv -> {
            try {
                pv[0] = Integer.parseInt(sv.trim());
                accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(pMinBox);
        if (pRand) {
            TextFieldWidget pMaxBox = new TextFieldWidget(textRenderer, s(vcx + TR_P_MAX_X), s(vy), s(TR_VAL_W), s(WIDGET_H), Text.empty());
            pMaxBox.setText(String.valueOf(pv[1]));
            pMaxBox.setMaxLength(6);
            pMaxBox.setChangedListener(sv -> {
                try {
                    pv[1] = Integer.parseInt(sv.trim());
                    accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(pMaxBox);
        }
        ButtonWidget mTypeBtn = ButtonWidget.builder(Text.translatable(TYPE_BTN, Text.translatable(mr[0] ? RANDOM_BTN : FIXED_BTN)), btn -> {
            mr[0] = !mr[0];
            btn.setMessage(Text.translatable(TYPE_BTN, Text.translatable(mr[0] ? RANDOM_BTN : FIXED_BTN)));
            accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
            clearAndInit();
        }).dimensions(s(vcx + TR_M_TYPE_X), s(vy), s(ER_TYPE_W), s(WIDGET_H)).build();
        addScrollWidget(mTypeBtn);
        TextFieldWidget mMinBox = new TextFieldWidget(textRenderer, s(vcx + TR_M_MIN_X), s(vy), s(TR_VAL_W), s(WIDGET_H), Text.empty());
        mMinBox.setText(String.valueOf(mv[0]));
        mMinBox.setMaxLength(6);
        mMinBox.setChangedListener(sv -> {
            try {
                mv[0] = Integer.parseInt(sv.trim());
                accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(mMinBox);
        if (mRand) {
            TextFieldWidget mMaxBox = new TextFieldWidget(textRenderer, s(vcx + TR_M_MAX_X), s(vy), s(TR_VAL_W), s(WIDGET_H), Text.empty());
            mMaxBox.setText(String.valueOf(mv[1]));
            mMaxBox.setMaxLength(6);
            mMaxBox.setChangedListener(sv -> {
                try {
                    mv[1] = Integer.parseInt(sv.trim());
                    accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(mMaxBox);
        }
        addScrollWidget(ButtonWidget.builder(Text.literal("×"), btn -> {
            accumulateMapEdit(mapName, catName, entryKey, null);
            clearAndInit();
        }).dimensions(s(vcx + ER_DEL_X), s(vy), s(ER_DEL_W), s(WIDGET_H)).build());
    }

    // ── Maps add-entry form ───────────────────────────────────────────────────

    private void initMapsAddPage(int vcx) {
        String mapName = MAP_KEYS[selectedMapIdx];
        boolean isSmelting = "Smelting".equals(mapName), isTrading = "Trading".equals(mapName);
        List<String> cats = new ArrayList<>();
        if (pending.get(mapName) instanceof Map<?, ?> outer) for (Object k : outer.keySet()) cats.add((String) k);
        if (cats.isEmpty()) cats.add("Blank");
        if (addCatIdx >= cats.size()) addCatIdx = 0;
        final List<String> catList = List.copyOf(cats);
        int vy = DETAIL_FIRST_ROW_V;
        ButtonWidget catBtn = ButtonWidget.builder(Text.literal(catList.get(addCatIdx)), btn -> {
            addCatIdx = (addCatIdx + 1) % catList.size();
            btn.setMessage(Text.literal(catList.get(addCatIdx)));
        }).dimensions(s(vcx + WIDGET_X_OFFSET), s(vy), s(WIDGET_W + 40), s(WIDGET_H)).build();
        addScrollWidget(catBtn);
        vy += ROW_H;
        TextFieldWidget keyBox = new TextFieldWidget(textRenderer, s(vcx + WIDGET_X_OFFSET), s(vy), s(WIDGET_W + 60), s(WIDGET_H), Text.empty());
        keyBox.setText(addKeyStr);
        keyBox.setMaxLength(128);
        keyBox.setPlaceholder(Text.literal("modid:id"));
        keyBox.setTextPredicate(sv -> sv.isEmpty() || sv.chars().allMatch(c ->
                (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == ':'));
        keyBox.setChangedListener(sv -> addKeyStr = sv.trim());
        addScrollWidget(keyBox);
        vy += ROW_H;
        if (isSmelting) {
            TextFieldWidget xpBox = new TextFieldWidget(textRenderer, s(vcx + ER_VAL1_X), s(vy) + 40, s(ER_VAL_W + 15), s(WIDGET_H), Text.empty());
            xpBox.setText(String.format("%.2f", addFloatVal));
            xpBox.setMaxLength(8);
            xpBox.setChangedListener(sv -> {
                try {
                    addFloatVal = Float.parseFloat(sv.trim());
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(xpBox);
        } else if (isTrading) {
            ButtonWidget pTypeBtn = ButtonWidget.builder(Text.translatable(TYPE_BTN, Text.translatable(addPRandom ? RANDOM_BTN : FIXED_BTN)), btn -> {
                addPRandom = !addPRandom;
                btn.setMessage(Text.translatable(TYPE_BTN, Text.translatable(addPRandom ? RANDOM_BTN : FIXED_BTN)));
                clearAndInit();
            }).dimensions(s(vcx + TR_P_TYPE_X), s(vy) + 40, s(ER_TYPE_W), s(WIDGET_H)).build();
            addScrollWidget(pTypeBtn);
            TextFieldWidget pValBox = new TextFieldWidget(textRenderer, s(vcx + TR_P_MIN_X), s(vy) + 40, s(TR_VAL_W), s(WIDGET_H), Text.empty());
            pValBox.setText(String.valueOf(addVals[0]));
            pValBox.setMaxLength(6);
            pValBox.setChangedListener(sv -> {
                try {
                    addVals[0] = Integer.parseInt(sv.trim());
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(pValBox);
            if (addPRandom) {
                TextFieldWidget pMaxBox = new TextFieldWidget(textRenderer, s(vcx + TR_P_MAX_X), s(vy) + 40, s(TR_VAL_W), s(WIDGET_H), Text.empty());
                pMaxBox.setText(String.valueOf(addVals[1]));
                pMaxBox.setMaxLength(6);
                pMaxBox.setChangedListener(sv -> {
                    try {
                        addVals[1] = Integer.parseInt(sv.trim());
                    } catch (NumberFormatException ignored) {
                    }
                });
                addScrollWidget(pMaxBox);
            }
            ButtonWidget mTypeBtn = ButtonWidget.builder(Text.translatable(TYPE_BTN, Text.translatable(addMRandom ? RANDOM_BTN : FIXED_BTN)), btn -> {
                addMRandom = !addMRandom;
                btn.setMessage(Text.translatable(TYPE_BTN, Text.translatable(addMRandom ? RANDOM_BTN : FIXED_BTN)));
                clearAndInit();
            }).dimensions(s(vcx + TR_M_TYPE_X), s(vy) + 40, s(ER_TYPE_W), s(WIDGET_H)).build();
            addScrollWidget(mTypeBtn);
            TextFieldWidget mValBox = new TextFieldWidget(textRenderer, s(vcx + TR_M_MIN_X), s(vy) + 40, s(TR_VAL_W), s(WIDGET_H), Text.empty());
            mValBox.setText(String.valueOf(addVals[2]));
            mValBox.setMaxLength(6);
            mValBox.setChangedListener(sv -> {
                try {
                    addVals[2] = Integer.parseInt(sv.trim());
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(mValBox);
            if (addMRandom) {
                TextFieldWidget mMaxBox = new TextFieldWidget(textRenderer, s(vcx + TR_M_MAX_X), s(vy) + 40, s(TR_VAL_W), s(WIDGET_H), Text.empty());
                mMaxBox.setText(String.valueOf(addVals[3]));
                mMaxBox.setMaxLength(6);
                mMaxBox.setChangedListener(sv -> {
                    try {
                        addVals[3] = Integer.parseInt(sv.trim());
                    } catch (NumberFormatException ignored) {
                    }
                });
                addScrollWidget(mMaxBox);
            }
        } else {
            ButtonWidget typeBtn = ButtonWidget.builder(Text.translatable(TYPE_BTN, Text.translatable(addIsRandom ? RANDOM_BTN : FIXED_BTN)), btn -> {
                addIsRandom = !addIsRandom;
                btn.setMessage(Text.translatable(TYPE_BTN, Text.translatable(addIsRandom ? RANDOM_BTN : FIXED_BTN)));
                clearAndInit();
            }).dimensions(s(vcx + ER_TYPE_X), s(vy + 57), s(ER_TYPE_W), s(WIDGET_H)).build();
            addScrollWidget(typeBtn);
            TextFieldWidget val1Box = new TextFieldWidget(textRenderer, s(vcx + ER_VAL1_X), s(vy) + 40, s(ER_VAL_W - 5), s(WIDGET_H), Text.empty());
            val1Box.setText(String.valueOf(addVals[0]));
            val1Box.setMaxLength(6);
            val1Box.setChangedListener(sv -> {
                try {
                    addVals[0] = Integer.parseInt(sv.trim());
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(val1Box);
            if (addIsRandom) {
                TextFieldWidget val2Box = new TextFieldWidget(textRenderer, s(vcx + ER_VAL2_X), s(vy) + 40, s(ER_VAL_W - 5), s(WIDGET_H), Text.empty());
                val2Box.setText(String.valueOf(addVals[1]));
                val2Box.setMaxLength(6);
                val2Box.setChangedListener(sv -> {
                    try {
                        addVals[1] = Integer.parseInt(sv.trim());
                    } catch (NumberFormatException ignored) {
                    }
                });
                addScrollWidget(val2Box);
            }
        }
        vy += ROW_H;
        ButtonWidget confirmBtn = ButtonWidget.builder(Text.translatable(MAPS_ADD_CONFIRM), btn -> {
            if (addKeyStr.isBlank()) return;
            int ci = addKeyStr.indexOf(':');
            if (ci <= 0 || ci == addKeyStr.length() - 1) return;
            String cat = catList.get(addCatIdx);
            Object value;
            if (isSmelting) value = addFloatVal;
            else if (isTrading)
                value = buildTradingValTyped(addPRandom, addVals[0], addVals[1], addMRandom, addVals[2], addVals[3]);
            else {
                Map<String, Object> xv = new LinkedHashMap<>();
                if (addIsRandom) {
                    xv.put("type", "Random");
                    xv.put("min", addVals[0]);
                    xv.put("max", addVals[1]);
                } else {
                    xv.put("type", "Fixed");
                    xv.put("fixed", addVals[0]);
                }
                value = xv;
            }
            accumulateMapEdit(mapName, cat, addKeyStr, value);
            addingEntry = false;
            clearAndInit();
        }).dimensions(s(vcx + WIDGET_X_OFFSET) + KEYS_OFFSET_X, s(vy) + 40, s(WIDGET_W), s(WIDGET_H)).build();
        addScrollWidget(confirmBtn);
        vy += ROW_H;
        contentVirtualHeight = vy - FIRST_ROW_V + INNER_PAD_V;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void accumulateXpEdit(String mapName, String catName, String entryKey, boolean random, int v1, int v2) {
        Map<String, Object> xv = new LinkedHashMap<>();
        if (random) {
            xv.put("type", "Random");
            xv.put("min", v1);
            xv.put("max", v2);
        } else {
            xv.put("type", "Fixed");
            xv.put("fixed", v1);
        }
        accumulateMapEdit(mapName, catName, entryKey, xv);
    }

    private static Map<String, Object> buildTradingValTyped(boolean pRand, int pV1, int pV2, boolean mRand, int mV1, int mV2) {
        Map<String, Object> out = new LinkedHashMap<>(), p = new LinkedHashMap<>(), m = new LinkedHashMap<>();
        if (pRand) {
            p.put("type", "Random");
            p.put("min", pV1);
            p.put("max", pV2);
        } else {
            p.put("type", "Fixed");
            p.put("fixed", pV1);
        }
        if (mRand) {
            m.put("type", "Random");
            m.put("min", mV1);
            m.put("max", mV2);
        } else {
            m.put("type", "Fixed");
            m.put("fixed", mV1);
        }
        out.put("playerXp", p);
        out.put("merchantXp", m);
        return out;
    }

    @SuppressWarnings("unchecked")
    private void accumulateMapEdit(String mapName, String catName, String entryKey, Object value) {
        Map<String, Object> me = (Map<String, Object>) pending.computeIfAbsent("map_entries", k -> new LinkedHashMap<>());
        Map<String, Object> byM = (Map<String, Object>) me.computeIfAbsent(mapName, k -> new LinkedHashMap<>());
        Map<String, Object> byC = (Map<String, Object>) byM.computeIfAbsent(catName, k -> new LinkedHashMap<>());
        byC.put(entryKey, value);
    }

    private List<Map.Entry<String, Object>> getEffectiveEntries(String mapName, String catName) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (pending.get(mapName) instanceof Map<?, ?> outer && outer.get(catName) instanceof Map<?, ?> cat)
            for (Map.Entry<?, ?> e : cat.entrySet()) result.put((String) e.getKey(), e.getValue());
        if (pending.get("map_entries") instanceof Map<?, ?> me && me.get(mapName) instanceof Map<?, ?> bm && bm.get(catName) instanceof Map<?, ?> bc)
            for (Map.Entry<?, ?> e : bc.entrySet()) {
                if (e.getValue() == null) result.remove((String) e.getKey());
                else result.put((String) e.getKey(), e.getValue());
            }
        return new ArrayList<>(result.entrySet());
    }

    // ── Scroll ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        boolean in = mx >= panelLeft() && mx <= panelRight() + s(SCROLLBAR_W) + s(SCROLLBAR_GAP) && my >= panelTop() && my <= panelBottom();
        if (in) {
            scrollV(scrollV() - (int) (dy * ROW_H * 0.75));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        // Fixed header
        ctx.drawTextWithShadow(textRenderer, Text.translatable(TITLE), (width / 2) - (textRenderer.getWidth(Text.translatable(TITLE)) / 2), s(8), 0xFFFFFF);
        if (client != null && client.player != null) {
            boolean op2 = Boolean.TRUE.equals(ConfigServerSync.clientExplicit());
            boolean op = client.player.hasPermissionLevel(2);

            if (op && op2) {
                ctx.drawTextWithShadow(textRenderer, Text.translatable(HINT_OP), (width / 2) - (textRenderer.getWidth(Text.translatable(HINT_OP)) / 2), s(25), 0xAAAAAA);
            } else {
                ctx.drawTextWithShadow(textRenderer, Text.translatable(HINT_NON_OP), (width / 2) - (textRenderer.getWidth(Text.translatable(HINT_NON_OP)) / 2), s(25), 0xAAAAAA);
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.translatable(HINT_MAIN), (width / 2) - (textRenderer.getWidth(Text.translatable(HINT_MAIN)) / 2), s(25), 0xAAAAAA);
        }

        // Scissored panel
        if (currentPage == PAGE_MAPS && selectedMapIdx >= 0)
            ctx.enableScissor(panelLeft(), panelTop() + s(23), panelRight(), panelBottom() - s(1));
        else
            ctx.enableScissor(panelLeft(), panelTop() + s(1), panelRight(), panelBottom() - s(1));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, -s(scrollV()), 0);
        renderPageLabels(ctx);
        ctx.getMatrices().pop();

        for (ClickableWidget w : scrollWidgets) if (w.visible) w.render(ctx, mouseX, mouseY, delta);
        ctx.disableScissor();

        if (currentPage == PAGE_MAPS && selectedMapIdx >= 0) {
            int stripBottom = panelTop() + s(22), pr2 = panelRight() + s(SCROLLBAR_W) + s(SCROLLBAR_GAP);
            ctx.fill(panelLeft() + 1, panelTop() + 1, pr2 - 1, stripBottom, 0xFF181818);
            ctx.fill(panelLeft() + 1, stripBottom, pr2 - 1, stripBottom + 1, 0xFF2C2C2C);
        }

        // Fixed widgets (tabs, Apply, Cancel, back/add buttons)
        for (ClickableWidget w : fixedWidgets) w.render(ctx, mouseX, mouseY, delta);

        renderScrollbar(ctx);
    }

    private void renderPageLabels(DrawContext ctx) {
        int vcx = vcx();
        switch (currentPage) {
            case PAGE_MODES -> drawRowLabels(ctx, vcx, MODE_KEYS);
            case PAGE_MULTIPLIERS -> drawRowLabels(ctx, vcx, MULTS_KEYS);
            case PAGE_MENDING -> drawRowLabels(ctx, vcx, MENDING_KEYS);
            case PAGE_MAPS -> {
                if (selectedMapIdx >= 0) {
                    if (addingEntry) drawMapsAddLabels(ctx, vcx);
                    else drawMapsDetailLabels(ctx, vcx);
                } else drawMapsLabels(ctx, vcx);
            }
        }
    }

    private void drawRowLabels(DrawContext ctx, int vcx, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            int sy = s(FIRST_ROW_V + i * ROW_H + (WIDGET_H - textRenderer.fontHeight) / 2);
            ctx.drawText(textRenderer, Text.translatable(keys[i]), s(vcx + TEXT_X_OFFSET) - KEYS_OFFSET_X, sy, 0xE0E0E0, false);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawMapsLabels(DrawContext ctx, int vcx) {
        for (int i = 0; i < MAP_KEYS.length; i++) {
            Object raw = pending.get(MAP_KEYS[i]);
            int count = 0;
            if (raw instanceof Map<?, ?> outer)
                for (Object cat : ((Map<String, Object>) outer).values())
                    if (cat instanceof Map<?, ?> inner) count += inner.size();
            int sy = s(FIRST_ROW_V + i * ROW_H + (WIDGET_H - textRenderer.fontHeight) / 2);
            ctx.drawText(textRenderer, Text.translatable(MAP_KEYS_LBL[i]), s(vcx + TEXT_X_OFFSET), sy, 0xE0E0E0, false);
            ctx.drawText(textRenderer, Text.translatable(COUNT_KEY_LBL).append(String.valueOf(count)), s(vcx + WIDGET_X_OFFSET) - KEYS_OFFSET_X + 10, sy, 0xAAAAAA, false);
        }
    }

    private void drawMapsDetailLabels(DrawContext ctx, int vcx) {
        String mapName = MAP_KEYS[selectedMapIdx];
        Object mapData = pending.get(mapName);
        if (!(mapData instanceof Map<?, ?> outerMap)) return;
        boolean isSmelting = "Smelting".equals(mapName), isTrading = "Trading".equals(mapName);
        int vy = DETAIL_FIRST_ROW_V;
        for (Map.Entry<?, ?> catEntry : outerMap.entrySet()) {
            String catName = (String) catEntry.getKey();
            int catSy = s(vy + (CAT_H - textRenderer.fontHeight) / 2);
            for (Map.Entry<String, Object> entry : getEffectiveEntries(mapName, catName)
                    .stream()
                    .filter(e -> matchesSearch(e.getKey()))
                    .toList()) {

                ctx.drawText(textRenderer, Text.literal("▶ " + catName), s(PANEL_X_MARGIN + 5), catSy, 0xFF8888FF, false);
                if (isTrading) {
                    ctx.drawText(textRenderer, Text.translatable(TR_P_LABEL), s(vcx + TR_P_TYPE_X + 15), catSy - 10, 0x6666AA, false);
                    ctx.drawText(textRenderer, Text.translatable(TYPE), s(vcx + TR_P_TYPE_X + 7), catSy, 0x888888, false);
                    ctx.drawText(textRenderer, Text.translatable(TR_M_LABEL), s(vcx + TR_M_TYPE_X + 15), catSy - 10, 0x6666AA, false);
                    ctx.drawText(textRenderer, Text.translatable(TYPE), s(vcx + TR_M_TYPE_X + 7), catSy, 0x888888, false);
                } else if (isSmelting) {
                    ctx.drawText(textRenderer, Text.translatable(FLOAT), s(vcx + ER_VAL1_X + 4), catSy, 0x888888, false);
                } else {
                    if (!getEffectiveEntries(mapName, catName).isEmpty()) {
                        ctx.drawText(textRenderer, Text.translatable(TYPE), s(vcx + ER_TYPE_X + 4), catSy, 0x888888, false);
                    }
                }
            }

            // searchbox
            List<Map.Entry<String, Object>> entries = getEffectiveEntries(mapName, catName)
                    .stream()
                    .filter(e -> matchesSearch(e.getKey()))
                    .toList();
            if (entries.isEmpty()) {
                continue;
            }


            vy += CAT_H;
            for (Map.Entry<String, Object> entry : entries) {
                int sy = s(vy + (WIDGET_H - textRenderer.fontHeight) / 2);
                int maxKeyVW = vcx + ER_TYPE_X - PANEL_X_MARGIN - 17;
                ctx.drawText(textRenderer, Text.literal(textLabelWrap(entry.getKey(), maxKeyVW)), s(PANEL_X_MARGIN + 12), sy, 0xE0E0E0, false);
                int lblY = s(vy) - s(WIDGET_H) + 7;
                if (!isSmelting && !isTrading && entry.getValue() instanceof Map<?, ?> vm) {
                    String t = vm.get("type") instanceof String st ? st : "Fixed";
                    boolean rand = "Random".equalsIgnoreCase(t);
                    ctx.drawText(textRenderer, rand ? Text.translatable(MIN) : Text.translatable(FIXED_BTN), s(vcx + ER_VAL1_X + 4), lblY, 0x666666, false);
                    if (rand)
                        ctx.drawText(textRenderer, Text.translatable(MAX), s(vcx + ER_VAL2_X + 4), lblY, 0x666666, false);
                } else if (isTrading && entry.getValue() instanceof Map<?, ?> tv) {
                    if (tv.get("playerXp") instanceof Map<?, ?> pm) {
                        String t = pm.get("type") instanceof String st ? st : "Fixed";
                        boolean r = "Random".equalsIgnoreCase(t);
                        ctx.drawText(textRenderer, r ? Text.translatable(MIN) : Text.translatable(FIXED_BTN), s(vcx + TR_P_MIN_X + 2), lblY, 0x666666, false);
                        if (r)
                            ctx.drawText(textRenderer, Text.translatable(MAX), s(vcx + TR_P_MAX_X + 5), lblY, 0x666666, false);
                    }
                    if (tv.get("merchantXp") instanceof Map<?, ?> mm) {
                        String t = mm.get("type") instanceof String st ? st : "Fixed";
                        boolean r = "Random".equalsIgnoreCase(t);
                        ctx.drawText(textRenderer, r ? Text.translatable(MIN) : Text.translatable(FIXED_BTN), s(vcx + TR_M_MIN_X + 2), lblY, 0x666666, false);
                        if (r)
                            ctx.drawText(textRenderer, Text.translatable(MAX), s(vcx + TR_M_MAX_X + 5), lblY, 0x666666, false);
                    }
                }
                vy += ROW_H;
            }
        }
    }

    private void drawMapsAddLabels(DrawContext ctx, int vcx) {
        String mapName = MAP_KEYS[selectedMapIdx];
        boolean isSmelting = "Smelting".equals(mapName), isTrading = "Trading".equals(mapName);
        int vy = DETAIL_FIRST_ROW_V;
        ctx.drawText(textRenderer, Text.translatable(MAPS_ADD_CAT_LBL), s(PANEL_X_MARGIN + 5), s(vy + (WIDGET_H - textRenderer.fontHeight) / 2), 0xE0E0E0, false);
        vy += ROW_H;
        ctx.drawText(textRenderer, Text.translatable(MAPS_ADD_KEY_LBL), s(PANEL_X_MARGIN + 5), s(vy + (WIDGET_H - textRenderer.fontHeight) / 2), 0xE0E0E0, false);
        vy += ROW_H;
        int sy = s(vy + (WIDGET_H - textRenderer.fontHeight) / 2) + 27;
        if (isSmelting)
            ctx.drawText(textRenderer, Text.translatable(FLOAT), s(vcx + ER_VAL1_X + 4), sy, 0x888888, false);
        else if (isTrading) {
            ctx.drawText(textRenderer, Text.translatable(TR_P_LABEL), s(vcx + TR_P_TYPE_X - 5), sy - 20, 0x888888, false);
            ctx.drawText(textRenderer, Text.translatable(TYPE), s(vcx + TR_P_TYPE_X + 18), sy, 0x888888, false);
            ctx.drawText(textRenderer, addPRandom ? Text.translatable(MIN) : Text.translatable(FIXED_BTN), s(vcx + TR_P_MIN_X) + 6, sy, 0x888888, false);
            if (addPRandom)
                ctx.drawText(textRenderer, Text.translatable(MAX), s(vcx + TR_P_MAX_X) + 8, sy, 0x888888, false);
            ctx.drawText(textRenderer, Text.translatable(TR_M_LABEL), s(vcx + TR_M_TYPE_X + 20), sy - 20, 0x888888, false);
            ctx.drawText(textRenderer, Text.translatable(TYPE), s(vcx + TR_M_TYPE_X + 16), sy, 0x888888, false);
            ctx.drawText(textRenderer, addMRandom ? Text.translatable(MIN) : Text.translatable(FIXED_BTN), s(vcx + TR_M_MIN_X) + 6, sy, 0x888888, false);
            if (addMRandom)
                ctx.drawText(textRenderer, Text.translatable(MAX), s(vcx + TR_M_MAX_X) + 8, sy, 0x888888, false);
        } else {
            ctx.drawText(textRenderer, addIsRandom ? Text.translatable(MIN) : Text.translatable(FIXED_BTN), s(vcx + ER_VAL1_X + 4), sy, 0x888888, false);
            if (addIsRandom)
                ctx.drawText(textRenderer, Text.translatable(MAX), s(vcx + ER_VAL2_X + 4), sy, 0x888888, false);
        }
        ctx.drawText(textRenderer, Text.translatable(MAPS_ADD_TYPE_LBL), s(PANEL_X_MARGIN + 5), sy + 15, 0xE0E0E0, false);
    }

    // ── Scrollbar ─────────────────────────────────────────────────────────────
    private void renderScrollbar(DrawContext ctx) {
        if (maxScrollV() <= 0) return;
        int trackX = panelRight() + s(SCROLLBAR_GAP), trackT = panelTop() + 2, trackH = panelHeight() - 4, barW = s(SCROLLBAR_W);
        ctx.fill(trackX, trackT, trackX + barW, trackT + trackH, 0xFF222222);
        float visRatio = (float) panelHeight() / s(contentVirtualHeight);
        int thumbH = Math.max(s(14), (int) (trackH * Math.min(1f, visRatio)));
        float frac = (float) scrollV() / maxScrollV();
        int thumbY = trackT + (int) ((trackH - thumbH) * frac);
        ctx.fill(trackX + 1, thumbY, trackX + barW - 1, thumbY + thumbH, 0xFF777777);
        ctx.fill(trackX + 1, thumbY, trackX + barW - 1, thumbY + 1, 0xFF999999);
        ctx.fill(trackX + 1, thumbY + thumbH - 1, trackX + barW - 1, thumbY + thumbH, 0xFF555555);
    }

    private String textLabelWrap(String key, int maxVirtualWidth) {
        int maxScreenW = s(maxVirtualWidth);
        if (textRenderer.getWidth(key) <= maxScreenW) return key;
        String wrap = "…";
        while (!key.isEmpty() && textRenderer.getWidth(key + wrap) > maxScreenW)
            key = key.substring(0, key.length() - 1);
        return key + wrap;
    }

    // ── Misc ──────────────────────────────────────────────────────────────────
    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        boolean ro = isReadOnly();
        if (ro != lastReadOnly) {
            lastReadOnly = ro;
            clearAndInit();
            return;
        }
        boolean mv = Config.isMapViewEnabled();
        if (mv != lastMapViewEnabled) {
            lastMapViewEnabled = mv;
            clearAndInit();
        }
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    // ── Apply ────────────────────────────────────────────────────────────────
    private void applyChanges() {
        if (isReadOnly()) return;
        Map<String, Object> serverMap = ConfigServerSync.getClientShadow();
        Map<String, Object> delta = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : pending.entrySet())
            if (!Objects.equals(e.getValue(), serverMap.get(e.getKey()))) delta.put(e.getKey(), e.getValue());
        if (delta.isEmpty()) {
            close();
            return;
        }

        if (client == null || client.player == null) {
            applyToMainConfig(delta);
        } else {
            ClientPlayNetworking.send(new ConfigUpdate(delta));
        }
        close();
    }

    @SuppressWarnings("unchecked")
    private void applyToMainConfig(Map<String, Object> delta) {
        Map<String, Object> flagDelta = new LinkedHashMap<>(delta);
        Object me = flagDelta.remove("map_entries");
        if (!flagDelta.isEmpty()) Config.fromMapAll(flagDelta);
        if (me instanceof Map<?, ?> mapEntries) Config.applyEntryDelta((Map<String, Object>) mapEntries);
        Config.saveToMainConfig();
        ConfigServerSync.setClientShadow(Config.toMapAll());
    }
}

