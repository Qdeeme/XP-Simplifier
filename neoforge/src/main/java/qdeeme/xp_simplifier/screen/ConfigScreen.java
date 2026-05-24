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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import qdeeme.xp_simplifier.network.ConfigUpdate;
import qdeeme.xp_simplifier.util.CropXpMode;
import qdeeme.xp_simplifier.util.OrbMode;
import qdeeme.xp_simplifier.util.XpMode;
import qdeeme.xp_simplifier.util.config.ConfigServerSync;
import qdeeme.xp_simplifier.util.config.XpsConfig;

import java.util.*;

/**
 * In-game config screen.
 *
 * <p><b>Scroll design:</b> each tab's content lives in a scissored panel.
 * Scrollable widgets are added via {@link #addScrollWidget} which stores their
 * "zero-scroll" Y in a parallel list. {@link #syncWidgetPositions()} then sets
 * each widget's real Y to {@code baseY - scrollPx} and hides those that fall
 * outside the panel, so Minecraft's built-in hit-testing just works.</p>
 */
public class ConfigScreen extends Screen {

    // filter search
    private final List<String> allKeys = new ArrayList<>();
    private final List<String> filteredKeys = new ArrayList<>();
    private String currentFilter = "";

    //   Scale
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

    //   Translation keys
    private static final String TITLE = "screen.xp_simplifier.config.title";
    private static final String APPLY = "screen.xp_simplifier.config.apply";
    private static final String CANCEL = "gui.cancel";
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

    private static final String[] MAP_KEYS =
            {"Blocks", "Crops", "Entities", "Smelting", "Trading", "Breeding", "Fishing", "Grindstone"};
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
    private static final String COUNT_KEY_LBL = "screen.xp_simplifier.config.maps.label.countKey";

    private static final String SEARCH = "screen.xp_simplifier.config.maps.search_box";
    private static final String SEARCH_BOX = "screen.xp_simplifier.config.maps.search_box_tooltip";
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

    // Maps detail-view layout (virtual px, relative to vcx unless noted)
    private static final int TEXT_X_OFFSET = -160;
    private static final int KEYS_OFFSET_X = 50;   // from text to key edit box
    private static final int CAT_H = 26;   // category header height
    private static final int DETAIL_FIRST_ROW_V = 107;  // FIRST_ROW_V(83) + 24 — below back button
    // XpValue entry row column offsets (vcx-relative) — shifted +100 so key label fits 35+ chars
    private static final int ER_TYPE_X = 20;  // type toggle btn
    private static final int ER_TYPE_W = 70;
    private static final int ER_VAL1_X = 105;  // fixed value OR min
    private static final int ER_VAL2_X = 165;  // max (RANDOM only)
    private static final int ER_VAL_W = 50;  // width of each value edit box
    private static final int ER_DEL_X = 250;  // delete "×" button
    private static final int ER_DEL_W = 24;
    // Trading row column offsets (vcx-relative).
    // Layout uses worst-case (both Random) positions so columns never shift:
    //   pType(-115→-45) pMin(-43→+2) pMax(+4→+49) | mType(+52→+122) mMin(+124→+169) mMax(+171→+216) | del(ER_DEL_X)
    private static final int TR_P_TYPE_X = -100;
    private static final int TR_P_MIN_X = -30;
    private static final int TR_P_MAX_X = 19;
    private static final int TR_M_TYPE_X = 67;
    private static final int TR_M_MIN_X = 137;
    private static final int TR_M_MAX_X = 187;
    private static final int TR_VAL_W = 45;

    //   Pages
    private static final int PAGE_MODES = 0;
    private static final int PAGE_MULTIPLIERS = 1;
    private static final int PAGE_MENDING = 2;
    private static final int PAGE_MAPS = 3;
    private int currentPage = PAGE_MODES;

    /**
     * Per-page scroll position in virtual pixels. Survives tab switches.
     */
    private final int[] pageScrollV = {0, 0, 0, 0};

    //   Layout (virtual pixels)  
    private static final int TAB_Y = 40;
    private static final int TAB_W = 120;
    private static final int TAB_H = 25;

    /**
     * Virtual Y where the scrollable panel box begins.
     */
    private static final int PANEL_TOP_V = 75;
    /**
     * Virtual distance from the bottom of the screen to the bottom of the panel.
     */
    private static final int PANEL_BOTTOM_END_V = 38;
    private static final int PANEL_X_MARGIN = 50;

    private static final int ROW_H = 40;
    /**
     * Padding inside the panel before the first row (virtual).
     */
    private static final int INNER_PAD_V = 8;
    private static final int FIRST_ROW_V = PANEL_TOP_V + INNER_PAD_V;


    private static final int WIDGET_X_OFFSET = 20;
    private static final int WIDGET_W = 120;
    private static final int WIDGET_H = 22;
    private static final int BTN_W = 90;
    private static final int BTN_H = 22;
    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_GAP = 3;

    //   Panel geometry (screen pixels)  
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

    //   Scroll state  
    /**
     * Total virtual height of the current page's content rows. Set by initXxxPage().
     */
    private int contentVirtualHeight = 0;

    private int maxScrollV() {
        int visibleV = (int) (panelHeight() / SCALE);
        return Math.max(0, contentVirtualHeight - visibleV + INNER_PAD_V);
    }

    private int scrollV() {
        return pageScrollV[currentPage];
    }

    private void scrollV(int v) {
        pageScrollV[currentPage] = Math.max(0, Math.min(v, maxScrollV()));
        syncWidgetPositions();
    }

    //   Scroll widget tracking  
    /**
     * Widgets inside the scrollable panel.
     */
    private final List<AbstractWidget> scrollWidgets = new ArrayList<>();
    /**
     * Their Y positions when scrollV == 0.
     */
    private final List<Integer> baseYList = new ArrayList<>();

    /**
     * Register a widget as part of the scroll panel.
     * Must be called before {@link #addRenderableWidget} for that widget.
     */
    private void addScrollWidget(AbstractWidget w) {
        baseYList.add(w.getY());
        scrollWidgets.add(w);
    }

    /**
     * Move every scroll widget to {@code baseY - scrollPx} and hide those
     * that are fully outside the panel.
     */
    private void syncWidgetPositions() {
        int scrollPx = s(scrollV());
        int top = panelTop();
        int bottom = panelBottom();
        for (int i = 0; i < scrollWidgets.size(); i++) {
            AbstractWidget w = scrollWidgets.get(i);
            int ny = baseYList.get(i) - scrollPx;
            w.setY(ny);
            w.visible = ny + w.getHeight() > top && ny < bottom;
        }
    }

    // Fields
    private final Screen parent;
    private final Map<String, Object> pending = new LinkedHashMap<>();
    /**
     * Which map is open in the detail view; -1 = overview.
     */
    private int selectedMapIdx = -1;
    /**
     * True while the "add new entry" sub-form is showing.
     */
    private boolean addingEntry = false;
    private int addCatIdx = 0;
    private boolean addIsRandom = false;
    /**
     * Trading add-form: type toggles for playerXp and merchantXp independently.
     */
    private boolean addPRandom = false;
    private boolean addMRandom = false;
    private final int[] addVals = {0, 0, 0, 0}; // v1,v2  OR  pVal/pMin,pMax,mVal/mMin,mMax
    private float addFloatVal = 0f;
    private String addKeyStr = "";

    // Constructor
    public ConfigScreen(Screen parent) {
        super(Component.translatable(TITLE));
        this.parent = parent;
        pending.putAll(ConfigServerSync.getClientShadow());
    }

    // Permission helper
    private boolean isReadOnly() {
        if (minecraft == null || minecraft.player == null) return false;
        if (minecraft.player.hasPermissions(2) && Boolean.TRUE.equals(ConfigServerSync.clientExplicit())) {
            return false;
        } else {
            return !ConfigServerSync.clientCanEdit();
        }
    }

    /**
     * Cached value from last {@link #init()} — compared every tick to detect server-pushed permission changes.
     */
    private boolean lastReadOnly = false;
    /**
     * Cached mapView value from last {@link #init()} — compared every tick to detect server-pushed mapView changes.
     */
    private boolean lastMapViewEnabled = true;

    // Background
    @Override
    public void renderBackground(@NotNull GuiGraphics gfx, int mx, int my, float pt) {
        // Dim background
        gfx.fill(0, 0, width, height, 0xC8101010);
        // Panel fill + border
        int pl = panelLeft(), pt2 = panelTop(), pr = panelRight() + s(SCROLLBAR_W) + s(SCROLLBAR_GAP), pb = panelBottom();
        gfx.fill(pl, pt2, pr, pb, 0xFF181818);
        gfx.renderOutline(pl, pt2, pr - pl, pb - pt2, 0xFF3A3A3A);
    }

    //  Init
    @Override
    protected void init() {
        scrollWidgets.clear();
        baseYList.clear();

        boolean readOnly = isReadOnly();
        lastReadOnly = readOnly;
        lastMapViewEnabled = XpsConfig.isMapViewEnabled();
        // Maps tab: disabled when player has no edit permission OR server locked mapView.
        // If no world is loaded (player == null) both flags default to editable.
        int vcx = vcx();

        //   Tabs
        addTabBtn(TAB_MODES, PAGE_MODES, vcx - 2 * TAB_W - 10);
        addTabBtn(TAB_MULTS, PAGE_MULTIPLIERS, vcx - TAB_W - 5);
        addTabBtn(TAB_MENDING, PAGE_MENDING, vcx);
        addTabBtn(TAB_MAPS, PAGE_MAPS, vcx + TAB_W + 5);
        currentFilter = "";
        addKeyStr = "";

        //   Page content
        switch (currentPage) {
            case PAGE_MODES -> initModesPage(vcx);
            case PAGE_MULTIPLIERS -> initMultipliersPage(vcx);
            case PAGE_MENDING -> initMendingPage();
            case PAGE_MAPS -> {
                if (selectedMapIdx >= 0) {
                    if (addingEntry) {
                        // Back → detail view
                        addRenderableWidget(Button.builder(
                                        Component.translatable(MAPS_BACK_BTN),
                                        btn -> {
                                            addingEntry = false;
                                            rebuildWidgets();
                                        })
                                .bounds(panelLeft() + 10, panelTop() + 2, s(BTN_W), s(18))
                                .build());
                        initMapsAddPage(vcx);
                    } else {
                        addRenderableWidget(SearchBox());
                        // Back → overview
                        addRenderableWidget(Button.builder(
                                        Component.translatable(MAPS_BACK_BTN),
                                        btn -> {
                                            selectedMapIdx = -1;
                                            rebuildWidgets();
                                        })
                                .bounds(panelLeft() + 10, panelTop() + 2, s(BTN_W), s(18))
                                .build());
                        // Add button — top-right corner of panel
                        Button addBtn = Button.builder(
                                        Component.translatable(MAPS_ADD_BTN),
                                        btn -> {
                                            addingEntry = true;
                                            addCatIdx = 0;
                                            addIsRandom = false;
                                            addPRandom = false;
                                            addMRandom = false;
                                            addVals[0] = addVals[1] = addVals[2] = addVals[3] = 0;
                                            addFloatVal = 0f;
                                            addKeyStr = "";
                                            rebuildWidgets();
                                        })
                                .bounds(panelRight() - s(BTN_W) - 5, panelTop() + 2, s(BTN_W), s(18))
                                .build();

                        addBtn.active = !readOnly;
                        addRenderableWidget(addBtn);
                        initMapsDetailPage(vcx);
                    }
                } else {
                    initMapsOverviewPage(vcx);
                }
            }
        }

        // Register scroll widgets with Minecraft
        for (AbstractWidget w : scrollWidgets) addRenderableWidget(w);

        // Disable scroll widgets according to page + permission state:
        //   • Maps overview  + mapView=false → Edit buttons inactive (no map access at all)
        //   • Maps overview  + mapView=true  → Edit buttons always active (navigation)
        //   • Maps detail / add-form         → disable editable widgets if mapsReadOnly
        //   • All other tabs                 → disable if readOnly
        if (currentPage == PAGE_MAPS && selectedMapIdx < 0) {
            if (!XpsConfig.isMapViewEnabled() && readOnly) scrollWidgets.forEach(w -> w.active = false);
        } else if (currentPage == PAGE_MAPS) {
            if (readOnly) scrollWidgets.forEach(w -> w.active = false);
        } else {
            if (readOnly) scrollWidgets.forEach(w -> w.active = false);
        }

        // Apply saved scroll, clamps to new maxScrollV
        syncWidgetPositions();

        //   Apply / Cancel (fixed, below panel)  
        Button applyBtn = Button.builder(
                        Component.translatable(APPLY), btn -> applyChanges())
                .bounds(s(vcx - 250), height - s(28), s(BTN_W), s(BTN_H))
                .build();
        applyBtn.active = !readOnly;
        addRenderableWidget(applyBtn);
        addRenderableWidget(Button.builder(Component.translatable(CANCEL), btn -> onClose())
                .bounds(s(vcx + 150), height - s(28), s(BTN_W), s(BTN_H))
                .build());
    }

    private void rebuildMapsPage() {

        boolean readOnly = isReadOnly();
        lastReadOnly = readOnly;

        for (AbstractWidget w : scrollWidgets) removeWidget(w);
        scrollWidgets.clear();
        baseYList.clear();

        int vcx = vcx();
        if (selectedMapIdx >= 0 && !addingEntry) initMapsDetailPage(vcx);
        else if (selectedMapIdx >= 0) initMapsAddPage(vcx);
        else initMapsOverviewPage(vcx);

        for (AbstractWidget w : scrollWidgets) addRenderableWidget(w);
        if (currentPage == PAGE_MAPS) {
            if (readOnly) scrollWidgets.forEach(w -> w.active = false);
        } else {
            if (readOnly) scrollWidgets.forEach(w -> w.active = false);
        }
        syncWidgetPositions();
    }

    private EditBox SearchBox() {
        EditBox searchBox = new EditBox(font, (width / 2) - (3 * s(BTN_W) / 2), panelTop() + 2, s(BTN_W) * 3, s(18), Component.empty());
        searchBox.setValue(currentFilter);
        searchBox.setMaxLength(128);
        searchBox.setHint(Component.translatable(SEARCH));
        searchBox.setTooltip(Tooltip.create(Component.translatable(SEARCH_BOX)));
        searchBox.setResponder(text -> {
            currentFilter = text.toLowerCase(Locale.ROOT);
            filterKeys(currentFilter);
            scrollV(0);
            rebuildMapsPage();
        });
        return searchBox;
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

    // gui builder

    private void addTabBtn(String labelKey, int page, int vx) {
        Button btn = Button.builder(Component.translatable(labelKey), b -> {
            currentPage = page;
            if (page != PAGE_MAPS) {
                addingEntry = false;
            }
            rebuildWidgets();
        }).bounds(s(vx), s(ConfigScreen.TAB_Y), s(TAB_W), s(TAB_H)).build();
        if (currentPage == page) btn.active = false;
        addRenderableWidget(btn);
    }

    //   Pages

    private void initModesPage(int vcx) {
        String[] cfgKeys = {
                "orbMode", "entityXpMode", "blockXpMode", "cropXpMode",
                "breedingXpMode", "fishingXpMode", "tradingPlayerXp",
                "merchantXp", "grindstoneXpMode", "smeltingXpMode"
        };
        Enum<?>[][] valSets = {
                OrbMode.values(), XpMode.values(), XpMode.values(), CropXpMode.values(),
                XpMode.values(), XpMode.values(), XpMode.values(),
                XpMode.values(), XpMode.values(), XpMode.values()
        };
        for (int i = 0; i < cfgKeys.length; i++)
            addEnumRow(vcx + KEYS_OFFSET_X, FIRST_ROW_V + i * ROW_H, cfgKeys[i], MODE_KEYS[i], valSets[i]);
        contentVirtualHeight = cfgKeys.length * ROW_H + INNER_PAD_V * 2;
    }

    private void initMultipliersPage(int vcx) {
        String[] cfgKeys = {
                "entityXpMultiplier", "blockXpMultiplier", "cropXpMultiplier",
                "smeltingXpMultiplier", "tradingXpMultiplier", "merchantXpMultiplier",
                "breedingXpMultiplier", "fishingXpMultiplier", "grindstoneXpMultiplier"
        };
        for (int i = 0; i < cfgKeys.length; i++)
            addFloatRow(vcx + KEYS_OFFSET_X, FIRST_ROW_V + i * ROW_H, cfgKeys[i], MULTS_KEYS[i]);
        contentVirtualHeight = cfgKeys.length * ROW_H + INNER_PAD_V * 2;
    }

    private void initMendingPage() {
        int vy = FIRST_ROW_V;
        boolean enabled = pending.get("xpRepairEnabled") instanceof Boolean b ? b : true;
        final boolean[] flag = {enabled};
        Button toggleBtn = Button.builder(
                        Component.translatable(MENDING_BTN,
                                Component.translatable(flag[0] ? MENDING_ON : MENDING_OFF)),
                        btn -> {
                            flag[0] = !flag[0];
                            pending.put("xpRepairEnabled", flag[0]);
                            btn.setMessage(Component.translatable(MENDING_BTN,
                                    Component.translatable(flag[0] ? MENDING_ON : MENDING_OFF)));
                        })
                .bounds(s(vcx() + WIDGET_X_OFFSET) + KEYS_OFFSET_X, s(vy), s(WIDGET_W), s(WIDGET_H))
                .tooltip(Tooltip.create(Component.translatable(MENDING_TOOLTIP)))
                .build();
        addScrollWidget(toggleBtn);
        addFloatRow(vcx() + KEYS_OFFSET_X, vy + ROW_H, MENDING_KEYS[1], 0.01f, 100000f);
        addIntRow(vcx() + KEYS_OFFSET_X, vy + 2 * ROW_H, MENDING_KEYS[2], 0, 9999);
        contentVirtualHeight = 3 * ROW_H + INNER_PAD_V * 2;
    }

    //   Row builders  

    private void addEnumRow(int vcx, int vy, String cfgKey, String labelKey, Enum<?>[] values) {
        String cur = pending.get(cfgKey) instanceof String sv ? sv : (pending.get(cfgKey) != null ? pending.get(cfgKey).toString() : values[0].name());
        int startIdx = 0;
        for (int i = 0; i < values.length; i++)
            if (values[i].name().equals(cur)) {
                startIdx = i;
                break;
            }
        final int[] idx = {startIdx};
        Button btn = Button.builder(Component.literal(values[idx[0]].name()), b -> {
                    idx[0] = (idx[0] + 1) % values.length;
                    b.setMessage(Component.literal(values[idx[0]].name()));
                    pending.put(cfgKey, values[idx[0]].name());
                }).bounds(s(vcx + (WIDGET_X_OFFSET * 2)), s(vy), s(WIDGET_W), s(WIDGET_H))
                .tooltip(Tooltip.create(Component.translatable(labelKey + ".tooltip"))).build();
        addScrollWidget(btn);
    }

    private void addFloatRow(int vcx, int vy, String cfgKey, String labelKey) {
        Object val = pending.get(cfgKey);
        String strV = val instanceof Number n ? String.format("%.2f", n.floatValue()) : "1.0000";
        EditBox box = new EditBox(font, s(vcx + (WIDGET_X_OFFSET * 2)), s(vy), s(WIDGET_W), s(WIDGET_H), Component.translatable(labelKey));
        box.setValue(strV);
        box.setMaxLength(16);
        box.setTooltip(Tooltip.create(Component.translatable(K_MULT_RANGE, Component.translatable(labelKey))));
        box.setResponder(sv -> {
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
        EditBox box = new EditBox(font, s(vcx + (WIDGET_X_OFFSET * 2)), s(vy), s(WIDGET_W), s(WIDGET_H), Component.translatable(labelKey));
        box.setValue(strV);
        box.setMaxLength(8);
        box.setTooltip(Tooltip.create(Component.translatable(K_INT_RANGE, Component.translatable(labelKey), String.valueOf(min), String.valueOf(max))));
        box.setResponder(sv -> {
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
        EditBox box = new EditBox(font, s(vcx + (WIDGET_X_OFFSET * 2)), s(vy), s(WIDGET_W), s(WIDGET_H), Component.translatable(labelKey));
        box.setValue(strV);
        box.setMaxLength(6);
        box.setTooltip(Tooltip.create(Component.translatable(K_INT_RANGE, Component.translatable(labelKey), String.valueOf(min), String.valueOf(max))));
        box.setResponder(sv -> {
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
            Button editBtn = Button.builder(Component.translatable(MAPS_EDIT_BTN),
                            b -> {
                                selectedMapIdx = idx;
                                rebuildWidgets();
                            })
                    .bounds(s(vcx + 100), s(FIRST_ROW_V + i * ROW_H + (ROW_H - BTN_H) / 2) - 4,
                            s(BTN_W), s(BTN_H))
                    .build();
            addScrollWidget(editBtn);
        }
        contentVirtualHeight = MAP_KEYS.length * ROW_H + INNER_PAD_V * 2;
    }

    // ── Maps detail (per-entry editing) ──────────────────────────────────────

    private void initMapsDetailPage(int vcx) {
        String mapName = MAP_KEYS[selectedMapIdx];
        Object mapData = pending.get(mapName);
        if (!(mapData instanceof Map<?, ?> outerMap)) {
            contentVirtualHeight = DETAIL_FIRST_ROW_V + ROW_H;
            return;
        }

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
            String t = m.get("type") instanceof String s ? s : "Fixed";
            isRandom = "Random".equalsIgnoreCase(t);
            v1 = m.get(isRandom ? "min" : "fixed") instanceof Number n ? n.intValue() : 0;
            v2 = isRandom ? (m.get("max") instanceof Number n ? n.intValue() : 0) : v1;
        }
        final boolean[] rand = {isRandom};
        final int[] vals = {v1, v2};

        Button typeBtn = Button.builder(Component.translatable(TYPE_BTN, Component.translatable(rand[0] ? RANDOM_BTN : FIXED_BTN)), btn -> {
            rand[0] = !rand[0];
            btn.setMessage(Component.translatable(TYPE_BTN, Component.translatable(rand[0] ? RANDOM_BTN : FIXED_BTN)));
            accumulateXpEdit(mapName, catName, entryKey, rand[0], vals[0], vals[1]);
            rebuildWidgets();
        }).bounds(s(vcx + ER_TYPE_X), s(vy), s(ER_TYPE_W), s(WIDGET_H)).build();

        addScrollWidget(typeBtn);

        // Val1: fixed value (Fixed) or min (Random)
        EditBox val1Box = new EditBox(font, s(vcx + ER_VAL1_X), s(vy), s(ER_VAL_W - 5), s(WIDGET_H), Component.empty());
        val1Box.setValue(String.valueOf(vals[0]));
        val1Box.setMaxLength(6);
        val1Box.setResponder(sv -> {
            try {
                vals[0] = Integer.parseInt(sv.trim());
                accumulateXpEdit(mapName, catName, entryKey, rand[0], vals[0], vals[1]);
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(val1Box);

        // Val2: max (Random only — omit widget when Fixed to avoid confusion)
        if (isRandom) {
            EditBox val2Box = new EditBox(font, s(vcx + ER_VAL2_X), s(vy), s(ER_VAL_W - 5), s(WIDGET_H), Component.empty());
            val2Box.setValue(String.valueOf(vals[1]));
            val2Box.setMaxLength(6);
            val2Box.setResponder(sv -> {
                try {
                    vals[1] = Integer.parseInt(sv.trim());
                    accumulateXpEdit(mapName, catName, entryKey, rand[0], vals[0], vals[1]);
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(val2Box);
        }

        Button delBtn = Button.builder(Component.literal("×"), btn -> {
            accumulateMapEdit(mapName, catName, entryKey, null);
            rebuildWidgets();
        }).bounds(s(vcx + ER_DEL_X), s(vy), s(ER_DEL_W), s(WIDGET_H)).build();
        addScrollWidget(delBtn);
    }

    private void addSmeltingEntryRow(int vcx, int vy, String mapName, String catName, String entryKey, Object value) {
        float xp = value instanceof Number n ? n.floatValue() : 0f;
        EditBox xpBox = new EditBox(font, s(vcx + ER_VAL1_X), s(vy), s(ER_VAL_W + 15), s(WIDGET_H), Component.empty());
        xpBox.setValue(String.format("%.2f", xp));
        xpBox.setMaxLength(8);
        xpBox.setResponder(sv -> {
            try {
                float f = Float.parseFloat(sv.trim());
                accumulateMapEdit(mapName, catName, entryKey, f);
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(xpBox);
        Button delBtn = Button.builder(Component.literal("×"), btn -> {
            accumulateMapEdit(mapName, catName, entryKey, null);
            rebuildWidgets();
        }).bounds(s(vcx + ER_DEL_X), s(vy), s(ER_DEL_W), s(WIDGET_H)).build();
        addScrollWidget(delBtn);
    }

    private void addTradingEntryRow(int vcx, int vy, String mapName, String catName, String entryKey, Object value) {
        boolean pRand = false, mRand = false;
        int pMin = 0, pMax = 0, mMin = 0, mMax = 0;
        if (value instanceof Map<?, ?> tv) {
            if (tv.get("playerXp") instanceof Map<?, ?> p) {
                String t = p.get("type") instanceof String s ? s : "Fixed";
                pRand = "Random".equalsIgnoreCase(t);
                pMin = p.get(pRand ? "min" : "fixed") instanceof Number n ? n.intValue() : 0;
                pMax = pRand ? (p.get("max") instanceof Number n ? n.intValue() : 0) : pMin;
            }
            if (tv.get("merchantXp") instanceof Map<?, ?> m) {
                String t = m.get("type") instanceof String s ? s : "Fixed";
                mRand = "Random".equalsIgnoreCase(t);
                mMin = m.get(mRand ? "min" : "fixed") instanceof Number n ? n.intValue() : 0;
                mMax = mRand ? (m.get("max") instanceof Number n ? n.intValue() : 0) : mMin;
            }
        }
        final boolean[] pr = {pRand};
        final boolean[] mr = {mRand};
        final int[] pv = {pMin, pMax};
        final int[] mv = {mMin, mMax};

        // ── Player type toggle ────────────────────────────────────────────────
        Button pTypeBtn = Button.builder(Component.translatable(TYPE_BTN,
                Component.translatable(pr[0] ? RANDOM_BTN : FIXED_BTN)), btn -> {
            pr[0] = !pr[0];
            btn.setMessage(Component.translatable(TYPE_BTN, Component.translatable(pr[0] ? RANDOM_BTN : FIXED_BTN)));
            accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
            rebuildWidgets();
        }).bounds(s(vcx + TR_P_TYPE_X), s(vy), s(ER_TYPE_W), s(WIDGET_H)).build();
        addScrollWidget(pTypeBtn);

        // Player val / min
        EditBox pMinBox = new EditBox(font, s(vcx + TR_P_MIN_X), s(vy), s(TR_VAL_W), s(WIDGET_H), Component.empty());
        pMinBox.setValue(String.valueOf(pv[0]));
        pMinBox.setMaxLength(6);
        pMinBox.setResponder(sv -> {
            try {
                pv[0] = Integer.parseInt(sv.trim());
                accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(pMinBox);

        // Player max (Random only)
        if (pRand) {
            EditBox pMaxBox = new EditBox(font, s(vcx + TR_P_MAX_X), s(vy), s(TR_VAL_W), s(WIDGET_H), Component.empty());
            pMaxBox.setValue(String.valueOf(pv[1]));
            pMaxBox.setMaxLength(6);
            pMaxBox.setResponder(sv -> {
                try {
                    pv[1] = Integer.parseInt(sv.trim());
                    accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(pMaxBox);
        }

        // ── Merchant type toggle ──────────────────────────────────────────────
        Button mTypeBtn = Button.builder(Component.translatable(TYPE_BTN,
                Component.translatable(mr[0] ? RANDOM_BTN : FIXED_BTN)), btn -> {
            mr[0] = !mr[0];
            btn.setMessage(Component.translatable(TYPE_BTN, Component.translatable(mr[0] ? RANDOM_BTN : FIXED_BTN)));
            accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
            rebuildWidgets();
        }).bounds(s(vcx + TR_M_TYPE_X), s(vy), s(ER_TYPE_W), s(WIDGET_H)).build();
        addScrollWidget(mTypeBtn);

        // Merchant val / min
        EditBox mMinBox = new EditBox(font, s(vcx + TR_M_MIN_X), s(vy), s(TR_VAL_W), s(WIDGET_H), Component.empty());
        mMinBox.setValue(String.valueOf(mv[0]));
        mMinBox.setMaxLength(6);
        mMinBox.setResponder(sv -> {
            try {
                mv[0] = Integer.parseInt(sv.trim());
                accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
            } catch (NumberFormatException ignored) {
            }
        });
        addScrollWidget(mMinBox);

        // Merchant max (Random only)
        if (mRand) {
            EditBox mMaxBox = new EditBox(font, s(vcx + TR_M_MAX_X), s(vy), s(TR_VAL_W), s(WIDGET_H), Component.empty());
            mMaxBox.setValue(String.valueOf(mv[1]));
            mMaxBox.setMaxLength(6);
            mMaxBox.setResponder(sv -> {
                try {
                    mv[1] = Integer.parseInt(sv.trim());
                    accumulateMapEdit(mapName, catName, entryKey, buildTradingValTyped(pr[0], pv[0], pv[1], mr[0], mv[0], mv[1]));
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(mMaxBox);
        }

        // Delete
        Button delBtn = Button.builder(Component.literal("×"), btn -> {
            accumulateMapEdit(mapName, catName, entryKey, null);
            rebuildWidgets();
        }).bounds(s(vcx + ER_DEL_X), s(vy), s(ER_DEL_W), s(WIDGET_H)).build();
        addScrollWidget(delBtn);
    }

    // ── Maps add-entry form ───────────────────────────────────────────────────

    private void initMapsAddPage(int vcx) {
        String mapName = MAP_KEYS[selectedMapIdx];
        boolean isSmelting = "Smelting".equals(mapName);
        boolean isTrading = "Trading".equals(mapName);

        // Collect existing category names from the shadow
        List<String> cats = new ArrayList<>();
        if (pending.get(mapName) instanceof Map<?, ?> outer)
            for (Object k : outer.keySet()) cats.add((String) k);
        if (cats.isEmpty()) cats.add("Blank");
        if (addCatIdx >= cats.size()) addCatIdx = 0;
        final List<String> catList = List.copyOf(cats);

        int vy = DETAIL_FIRST_ROW_V;

        // Row 0 — Category cycle button
        Button catBtn = Button.builder(Component.literal(catList.get(addCatIdx)), btn -> {
            addCatIdx = (addCatIdx + 1) % catList.size();
            btn.setMessage(Component.literal(catList.get(addCatIdx)));
        }).bounds(s(vcx + WIDGET_X_OFFSET), s(vy), s(WIDGET_W + 40), s(WIDGET_H)).build();
        addScrollWidget(catBtn);
        vy += ROW_H;

        // Row 1 — Key EditBox (modid:identifier)
        EditBox keyBox = new EditBox(font, s(vcx + WIDGET_X_OFFSET), s(vy), s(WIDGET_W + 60), s(WIDGET_H), Component.empty());
        keyBox.setValue(addKeyStr);
        keyBox.setMaxLength(128);
        keyBox.setHint(Component.literal("modid:id"));
        // Only allow characters valid in a ResourceLocation; colon required before confirm
        keyBox.setFilter(sv -> sv.isEmpty() || sv.chars().allMatch(c ->
                (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                        || c == '_' || c == '-' || c == ':'));
        keyBox.setResponder(sv -> addKeyStr = sv.trim());
        addScrollWidget(keyBox);
        vy += ROW_H;

        if (isSmelting) {
            // Row 2 — float XP value
            EditBox xpBox = new EditBox(font, s(vcx + ER_VAL1_X), s(vy) + 40, s(ER_VAL_W + 15), s(WIDGET_H), Component.empty());
            xpBox.setValue(String.format("%.2f", addFloatVal));
            xpBox.setMaxLength(8);
            xpBox.setResponder(sv -> {
                try {
                    addFloatVal = Float.parseFloat(sv.trim());
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(xpBox);
        } else if (isTrading) {
            // Row 2 — playerXp type toggle + val(/min) + max, then merchantXp same pattern
            Button pTypeBtn = Button.builder(Component.translatable(TYPE_BTN,
                    Component.translatable(addPRandom ? RANDOM_BTN : FIXED_BTN)), btn -> {
                addPRandom = !addPRandom;
                btn.setMessage(Component.translatable(TYPE_BTN, Component.translatable(addPRandom ? RANDOM_BTN : FIXED_BTN)));
                rebuildWidgets();
            }).bounds(s(vcx + TR_P_TYPE_X), s(vy) + 40, s(ER_TYPE_W), s(WIDGET_H)).build();
            addScrollWidget(pTypeBtn);

            EditBox pValBox = new EditBox(font, s(vcx + TR_P_MIN_X), s(vy) + 40, s(TR_VAL_W), s(WIDGET_H), Component.empty());
            pValBox.setValue(String.valueOf(addVals[0]));
            pValBox.setMaxLength(6);
            pValBox.setResponder(sv -> {
                try {
                    addVals[0] = Integer.parseInt(sv.trim());
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(pValBox);

            if (addPRandom) {
                EditBox pMaxBox = new EditBox(font, s(vcx + TR_P_MAX_X), s(vy) + 40, s(TR_VAL_W), s(WIDGET_H), Component.empty());
                pMaxBox.setValue(String.valueOf(addVals[1]));
                pMaxBox.setMaxLength(6);
                pMaxBox.setResponder(sv -> {
                    try {
                        addVals[1] = Integer.parseInt(sv.trim());
                    } catch (NumberFormatException ignored) {
                    }
                });
                addScrollWidget(pMaxBox);
            }

            Button mTypeBtn = Button.builder(Component.translatable(TYPE_BTN,
                    Component.translatable(addMRandom ? RANDOM_BTN : FIXED_BTN)), btn -> {
                addMRandom = !addMRandom;
                btn.setMessage(Component.translatable(TYPE_BTN, Component.translatable(addMRandom ? RANDOM_BTN : FIXED_BTN)));
                rebuildWidgets();
            }).bounds(s(vcx + TR_M_TYPE_X), s(vy) + 40, s(ER_TYPE_W), s(WIDGET_H)).build();
            addScrollWidget(mTypeBtn);

            EditBox mValBox = new EditBox(font, s(vcx + TR_M_MIN_X), s(vy) + 40, s(TR_VAL_W), s(WIDGET_H), Component.empty());
            mValBox.setValue(String.valueOf(addVals[2]));
            mValBox.setMaxLength(6);
            mValBox.setResponder(sv -> {
                try {
                    addVals[2] = Integer.parseInt(sv.trim());
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(mValBox);

            if (addMRandom) {
                EditBox mMaxBox = new EditBox(font, s(vcx + TR_M_MAX_X), s(vy) + 40, s(TR_VAL_W), s(WIDGET_H), Component.empty());
                mMaxBox.setValue(String.valueOf(addVals[3]));
                mMaxBox.setMaxLength(6);
                mMaxBox.setResponder(sv -> {
                    try {
                        addVals[3] = Integer.parseInt(sv.trim());
                    } catch (NumberFormatException ignored) {
                    }
                });
                addScrollWidget(mMaxBox);
            }
        } else {
            // Row 2 — Type toggle + value field(s)
            Button typeBtn = Button.builder(Component.translatable(TYPE_BTN, Component.translatable(addIsRandom ? RANDOM_BTN : FIXED_BTN)), btn -> {
                addIsRandom = !addIsRandom;
                btn.setMessage(Component.translatable(TYPE_BTN, Component.translatable(addIsRandom ? RANDOM_BTN : FIXED_BTN)));
                rebuildWidgets();
            }).bounds(s(vcx + ER_TYPE_X), s(vy + 57), s(ER_TYPE_W), s(WIDGET_H)).build();
            addScrollWidget(typeBtn);

            EditBox val1Box = new EditBox(font, s(vcx + ER_VAL1_X), s(vy) + 40, s(ER_VAL_W - 5), s(WIDGET_H), Component.empty());
            val1Box.setValue(String.valueOf(addVals[0]));
            val1Box.setMaxLength(6);
            val1Box.setResponder(sv -> {
                try {
                    addVals[0] = Integer.parseInt(sv.trim());
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollWidget(val1Box);

            if (addIsRandom) {
                EditBox val2Box = new EditBox(font, s(vcx + ER_VAL2_X), s(vy) + 40, s(ER_VAL_W - 5), s(WIDGET_H), Component.empty());
                val2Box.setValue(String.valueOf(addVals[1]));
                val2Box.setMaxLength(6);
                val2Box.setResponder(sv -> {
                    try {
                        addVals[1] = Integer.parseInt(sv.trim());
                    } catch (NumberFormatException ignored) {
                    }
                });
                addScrollWidget(val2Box);
            }
        }
        vy += ROW_H;

        // Row 3 — Confirm "Add Entry" button
        Button confirmBtn = Button.builder(Component.translatable(MAPS_ADD_CONFIRM), btn -> {
            if (addKeyStr.isBlank()) return;
            // Require valid "namespace:path" — exactly one colon, both sides non-empty
            int colonIdx = addKeyStr.indexOf(':');
            if (colonIdx <= 0 || colonIdx == addKeyStr.length() - 1) return;
            String cat = catList.get(addCatIdx);
            Object value;
            if (isSmelting) {
                value = addFloatVal;
            } else if (isTrading) {
                value = buildTradingValTyped(addPRandom, addVals[0], addVals[1], addMRandom, addVals[2], addVals[3]);
            } else {
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
            rebuildWidgets();
        }).bounds(s(vcx + WIDGET_X_OFFSET) + KEYS_OFFSET_X, s(vy) + 40, s(WIDGET_W), s(WIDGET_H)).build();
        addScrollWidget(confirmBtn);
        vy += ROW_H;

        contentVirtualHeight = vy - FIRST_ROW_V + INNER_PAD_V;
    }

    // ── Maps add-entry labels ─────────────────────────────────────────────────

    private void drawMapsAddLabels(GuiGraphics gfx, int vcx) {
        String mapName = MAP_KEYS[selectedMapIdx];
        boolean isSmelting = "Smelting".equals(mapName);
        boolean isTrading = "Trading".equals(mapName);
        int vy = DETAIL_FIRST_ROW_V;

        // Row 0: Category
        gfx.drawString(font, Component.translatable(MAPS_ADD_CAT_LBL),
                s(PANEL_X_MARGIN + 5), s(vy + (WIDGET_H - font.lineHeight) / 2), 0xE0E0E0);
        vy += ROW_H;

        // Row 1: Key
        gfx.drawString(font, Component.translatable(MAPS_ADD_KEY_LBL),
                s(PANEL_X_MARGIN + 5), s(vy + (WIDGET_H - font.lineHeight) / 2), 0xE0E0E0);
        vy += ROW_H;

        // Row 2: value column headers / type label
        int sy = s(vy + (WIDGET_H - font.lineHeight) / 2) + 27;
        if (isSmelting) {
            gfx.drawString(font, Component.translatable(FLOAT), s(vcx + ER_VAL1_X + 4), sy, 0x888888);
        } else if (isTrading) {
            gfx.drawString(font, Component.translatable(TR_P_LABEL), s(vcx + TR_P_TYPE_X - 5), sy - 20, 0x888888);
            gfx.drawString(font, Component.translatable(TYPE), s(vcx + TR_P_TYPE_X + 18), sy, 0x888888);
            gfx.drawString(font, addPRandom ? Component.translatable(MIN) : Component.translatable(FIXED_BTN), s(vcx + TR_P_MIN_X) + 6, sy, 0x888888);
            if (addPRandom)
                gfx.drawString(font, Component.translatable(MAX), s(vcx + TR_P_MAX_X) + 8, sy, 0x888888);
            gfx.drawString(font, Component.translatable(TR_M_LABEL), s(vcx + TR_M_TYPE_X + 20), sy - 20, 0x888888);
            gfx.drawString(font, Component.translatable(TYPE), s(vcx + TR_M_TYPE_X + 16), sy, 0x888888);
            gfx.drawString(font, addMRandom ? Component.translatable(MIN) : Component.translatable(FIXED_BTN), s(vcx + TR_M_MIN_X) + 6, sy, 0x888888);
            if (addMRandom)
                gfx.drawString(font, Component.translatable(MAX), s(vcx + TR_M_MAX_X) + 8, sy, 0x888888);
        } else {
            gfx.drawString(font, addIsRandom ? Component.translatable(MIN) : Component.translatable(FIXED_BTN),
                    s(vcx + ER_VAL1_X + 4), sy, 0x888888);
            if (addIsRandom)
                gfx.drawString(font, Component.translatable(MAX), s(vcx + ER_VAL2_X + 4), sy, 0x888888);
        }
        gfx.drawString(font, Component.translatable(MAPS_ADD_TYPE_LBL), s(PANEL_X_MARGIN + 5), sy + 15, 0xE0E0E0);
    }


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

    /**
     * Shadow entries for {@code catName} overlaid with any accumulated edits (null = deleted).
     */
    private List<Map.Entry<String, Object>> getEffectiveEntries(String mapName, String catName) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (pending.get(mapName) instanceof Map<?, ?> outer &&
                outer.get(catName) instanceof Map<?, ?> cat)
            for (Map.Entry<?, ?> e : cat.entrySet()) result.put((String) e.getKey(), e.getValue());
        // Overlay pending edits
        if (pending.get("map_entries") instanceof Map<?, ?> me &&
                me.get(mapName) instanceof Map<?, ?> bm &&
                bm.get(catName) instanceof Map<?, ?> bc)
            for (Map.Entry<?, ?> e : bc.entrySet()) {
                if (e.getValue() == null) result.remove((String) e.getKey());
                else result.put((String) e.getKey(), e.getValue());
            }
        return new ArrayList<>(result.entrySet());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        boolean inPanel = mx >= panelLeft() && mx <= panelRight() + s(SCROLLBAR_W) + s(SCROLLBAR_GAP) && my >= panelTop() && my <= panelBottom();
        if (inPanel) {
            scrollV(scrollV() - (int) (dy * ROW_H * 0.75));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    // Render

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);

        // ── Fixed header (above panel, never clipped) ─────────────────────────
        gfx.drawCenteredString(font, Component.translatable(TITLE), width / 2, s(8), 0xFFFFFF);
        if (minecraft != null && minecraft.player != null) {
            boolean op = minecraft.player.hasPermissions(2);
            boolean op2 = Boolean.TRUE.equals(ConfigServerSync.clientExplicit());
            if (op && op2) {
                gfx.drawCenteredString(font, Component.translatable(HINT_OP), width / 2, s(25), 0xAAAAAA);
            } else {
                gfx.drawCenteredString(font, Component.translatable(HINT_NON_OP), width / 2, s(25), 0xAAAAAA);
            }
        } else {
            gfx.drawCenteredString(font, Component.translatable(HINT_MAIN), width / 2, s(25), 0xAAAAAA);
        }

        // ── Panel contents — scissored so nothing leaks outside ───────────────
        if (currentPage == PAGE_MAPS && selectedMapIdx >= 0) {
            gfx.enableScissor(panelLeft(), panelTop() + s(23), panelRight(), panelBottom() - s(1));
        } else {
            gfx.enableScissor(panelLeft(), panelTop() + s(1), panelRight(), panelBottom() - s(1));
        }

        // Labels translated by scroll
        gfx.pose().pushPose();
        gfx.pose().translate(0, -s(scrollV()), 0);
        renderPageLabels(gfx);
        gfx.pose().popPose();

        // Scroll widgets: render manually inside scissor so they can't escape the panel
        for (AbstractWidget w : scrollWidgets) {
            if (w.visible) w.render(gfx, mouseX, mouseY, partialTick);
        }

        gfx.disableScissor();
        if (currentPage == PAGE_MAPS && selectedMapIdx >= 0) {
            int stripBottom = panelTop() + s(22);
            int pr = panelRight() + s(SCROLLBAR_W) + s(SCROLLBAR_GAP);
            gfx.fill(panelLeft() + 1, panelTop() + 1, pr - 1, stripBottom, 0xFF181818);
            gfx.fill(panelLeft() + 1, stripBottom, pr - 1, stripBottom + 1, 0xFF2C2C2C);
        }

        // ── Fixed widgets (tabs, Apply/Cancel, back/add buttons) — no scissor ─
        for (var r : this.renderables) {
            if (!scrollWidgets.contains(r)) r.render(gfx, mouseX, mouseY, partialTick);
        }

        // ── Scrollbar — drawn on top of panel edge ────────────────────────────
        renderScrollbar(gfx);
    }

    private void renderPageLabels(GuiGraphics gfx) {
        int vcx = vcx();
        switch (currentPage) {
            case PAGE_MODES -> drawRowLabels(gfx, vcx, MODE_KEYS);
            case PAGE_MULTIPLIERS -> drawRowLabels(gfx, vcx, MULTS_KEYS);
            case PAGE_MENDING -> drawRowLabels(gfx, vcx, MENDING_KEYS);
            case PAGE_MAPS -> {
                if (selectedMapIdx >= 0) {
                    if (addingEntry) drawMapsAddLabels(gfx, vcx);
                    else drawMapsDetailLabels(gfx, vcx);
                } else drawMapsLabels(gfx, vcx);
            }
        }
    }

    private void drawRowLabels(GuiGraphics gfx, int vcx, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            int sy = s(FIRST_ROW_V + i * ROW_H + (WIDGET_H - font.lineHeight) / 2);
            gfx.drawString(font, Component.translatable(keys[i]), s(vcx + TEXT_X_OFFSET) - KEYS_OFFSET_X, sy, 0xE0E0E0);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawMapsLabels(GuiGraphics gfx, int vcx) {
        for (int i = 0; i < MAP_KEYS.length; i++) {
            Object raw = pending.get(MAP_KEYS[i]);

            // Count total entries across all categories in this map
            int count = 0;
            if (raw instanceof Map<?, ?> outer)
                for (Object cat : ((Map<String, Object>) outer).values())
                    if (cat instanceof Map<?, ?> inner) count += inner.size();

            int sy = s(FIRST_ROW_V + i * ROW_H + (WIDGET_H - font.lineHeight) / 2);
            gfx.drawString(font, Component.translatable(MAP_KEYS_LBL[i]), s(vcx + TEXT_X_OFFSET), sy, 0xE0E0E0);
            gfx.drawString(font, Component.translatable(COUNT_KEY_LBL).append(String.valueOf(count)), s(vcx + WIDGET_X_OFFSET) - KEYS_OFFSET_X + 10, sy, 0xAAAAAA);
        }
    }

    private void drawMapsDetailLabels(GuiGraphics gfx, int vcx) {
        String mapName = MAP_KEYS[selectedMapIdx];
        Object mapData = pending.get(mapName);
        if (!(mapData instanceof Map<?, ?> outerMap)) return;
        boolean isSmelting = "Smelting".equals(mapName);
        boolean isTrading = "Trading".equals(mapName);
        int vy = DETAIL_FIRST_ROW_V;
        for (Map.Entry<?, ?> catEntry : outerMap.entrySet()) {
            String catName = (String) catEntry.getKey();
            int catSy = s(vy + (CAT_H - font.lineHeight) / 2);
            for (Map.Entry<String, Object> entry : getEffectiveEntries(mapName, catName)
                    .stream()
                    .filter(e -> matchesSearch(e.getKey()))
                    .toList()) {
                // Category name
                gfx.drawString(font, "▶ " + catName, s(PANEL_X_MARGIN + 5), catSy, 0xFF8888FF);
                // Static column headers — only TYPE (value-column labels are per-entry below)
                if (isTrading) {
                    gfx.drawString(font, Component.translatable(TR_P_LABEL), s(vcx + TR_P_TYPE_X + 15), catSy - 10, 0x6666AA);
                    gfx.drawString(font, Component.translatable(TYPE), s(vcx + TR_P_TYPE_X + 7), catSy, 0x888888);
                    gfx.drawString(font, Component.translatable(TR_M_LABEL), s(vcx + TR_M_TYPE_X + 15), catSy - 10, 0x6666AA);
                    gfx.drawString(font, Component.translatable(TYPE), s(vcx + TR_M_TYPE_X + 7), catSy, 0x888888);
                } else if (isSmelting) {
                    gfx.drawString(font, Component.translatable(FLOAT), s(vcx + ER_VAL1_X + 4), catSy, 0x888888);
                } else {
                    if (catName != null && !getEffectiveEntries(mapName, catName).isEmpty()) {
                        gfx.drawString(font, Component.translatable(TYPE), s(vcx + ER_TYPE_X + 4), catSy, 0x888888);
                    }
                }
            }
            List<Map.Entry<String, Object>> entries = getEffectiveEntries(mapName, catName)
                    .stream().filter(e -> matchesSearch(e.getKey())).toList();
            if (entries.isEmpty()) {
                // don't draw category header or advance vy for it
                continue; // (this is inside a for loop — use label+continue or restructure)
            }

            vy += CAT_H;

            for (Map.Entry<String, Object> entry : entries) {
                int sy = s(vy + (WIDGET_H - font.lineHeight) / 2);
                int maxKeyVW = vcx + ER_TYPE_X - PANEL_X_MARGIN - 17;
                gfx.drawString(font, textLabelWrap(entry.getKey(), maxKeyVW), s(PANEL_X_MARGIN + 12), sy, 0xE0E0E0);
                // Per-entry value-column labels rendered below the widget row
                int lblY = s(vy) - s(WIDGET_H) + 7;
                if (!isSmelting && !isTrading && entry.getValue() instanceof Map<?, ?> vm) {
                    String t = vm.get("type") instanceof String st ? st : "Fixed";
                    boolean rand = "Random".equalsIgnoreCase(t);
                    gfx.drawString(font,
                            rand ? Component.translatable(MIN) : Component.translatable(FIXED_BTN),
                            s(vcx + ER_VAL1_X + 4), lblY, 0x666666);
                    if (rand)
                        gfx.drawString(font, Component.translatable(MAX), s(vcx + ER_VAL2_X + 4), lblY, 0x666666);
                } else if (isTrading && entry.getValue() instanceof Map<?, ?> tv) {
                    if (tv.get("playerXp") instanceof Map<?, ?> pm) {
                        String t = pm.get("type") instanceof String st ? st : "Fixed";
                        boolean rand = "Random".equalsIgnoreCase(t);
                        gfx.drawString(font,
                                rand ? Component.translatable(MIN) : Component.translatable(FIXED_BTN),
                                s(vcx + TR_P_MIN_X + 2), lblY, 0x666666);
                        if (rand)
                            gfx.drawString(font, Component.translatable(MAX), s(vcx + TR_P_MAX_X + 5), lblY, 0x666666);
                    }
                    if (tv.get("merchantXp") instanceof Map<?, ?> mm) {
                        String t = mm.get("type") instanceof String st ? st : "Fixed";
                        boolean rand = "Random".equalsIgnoreCase(t);
                        gfx.drawString(font,
                                rand ? Component.translatable(MIN) : Component.translatable(FIXED_BTN),
                                s(vcx + TR_M_MIN_X + 2), lblY, 0x666666);
                        if (rand)
                            gfx.drawString(font, Component.translatable(MAX), s(vcx + TR_M_MAX_X + 5), lblY, 0x666666);
                    }
                }
                vy += ROW_H;
            }
        }
    }

    private String textLabelWrap(String key, int maxVirtualWidth) {
        int maxScreenW = s(maxVirtualWidth);
        if (font.width(key) <= maxScreenW) return key;
        String sfx = "…";
        while (!key.isEmpty() && font.width(key + sfx) > maxScreenW)
            key = key.substring(0, key.length() - 1);
        return key + sfx;
    }

    // Scrollbar 

    private void renderScrollbar(GuiGraphics gfx) {
        if (maxScrollV() <= 0) return;
        int trackX = panelRight() + s(SCROLLBAR_GAP);
        int trackT = panelTop() + 2;
        int trackH = panelHeight() - 4;
        int barW = s(SCROLLBAR_W);

        // Track
        gfx.fill(trackX, trackT, trackX + barW, trackT + trackH, 0xFF222222);

        // Thumb
        float visRatio = (float) panelHeight() / s(contentVirtualHeight);
        int thumbH = Math.max(s(14), (int) (trackH * Math.min(1f, visRatio)));
        float frac = (float) scrollV() / maxScrollV();
        int thumbY = trackT + (int) ((trackH - thumbH) * frac);

        gfx.fill(trackX + 1, thumbY, trackX + barW - 1, thumbY + thumbH, 0xFF777777);
        gfx.fill(trackX + 1, thumbY, trackX + barW - 1, thumbY + 1, 0xFF999999); // top hi
        gfx.fill(trackX + 1, thumbY + thumbH - 1, trackX + barW - 1, thumbY + thumbH, 0xFF555555);
    }

    //  Misc
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // Rebuild widgets if the server pushed a permission change while the screen is open
        boolean ro = isReadOnly();
        if (ro != lastReadOnly) {
            lastReadOnly = ro;
            rebuildWidgets();
            return; // rebuildWidgets resets lastMapViewEnabled too
        }
        // Rebuild if mapView enabled state changed (e.g. /xps mapview broadcast)
        boolean mv = XpsConfig.isMapViewEnabled();
        if (mv != lastMapViewEnabled) {
            lastMapViewEnabled = mv;
            rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    // Apply 
    private void applyChanges() {
        if (isReadOnly()) return;
        Map<String, Object> serverMap = ConfigServerSync.getClientShadow();
        Map<String, Object> delta = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : pending.entrySet())
            if (!Objects.equals(e.getValue(), serverMap.get(e.getKey()))) delta.put(e.getKey(), e.getValue());
        if (delta.isEmpty()) {
            onClose();
            return;
        }

        if (minecraft == null || minecraft.player == null) {
            // No live server — write directly to the master config files
            applyToMainConfig(delta);
        } else {
            PacketDistributor.sendToServer(new ConfigUpdate(delta));
        }
        onClose();
    }

    @SuppressWarnings("unchecked")
    private void applyToMainConfig(Map<String, Object> delta) {
        Map<String, Object> flagDelta = new LinkedHashMap<>(delta);
        Object me = flagDelta.remove("map_entries");
        if (!flagDelta.isEmpty())
            XpsConfig.fromMapAll(flagDelta);
        if (me instanceof Map<?, ?> mapEntries)
            XpsConfig.applyEntryDelta((Map<String, Object>) mapEntries); // → CONFIG_DIR
        XpsConfig.saveToMainConfig(); // maps + flags.json → CONFIG_DIR
        ConfigServerSync.setClientShadow(XpsConfig.toMapAll());
    }
}