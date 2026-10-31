package dev.customgui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public class CustomGuiScreen extends Screen {

    private static final int TAB_COUNT = 4;
    private static final String[] TAB_KEYS = {"tab.home", "tab.islands", "tab.pvp", "tab.settings"};

    private enum Tab { HOME, ISLANDS, PVP, SETTINGS }

    // Farben (ARGB)
    private static final int BG = 0xFF0C111D;
    private static final int BORDER = 0xFF1F2B45;
    private static final int HEADER = 0xFF111828;
    private static final int BTN = 0xFF141B2E;
    private static final int BTN_BORDER = 0xFF26334F;
    private static final int TEXT = 0xFFE6EAF2;
    private static final int MUTED = 0xFF8A94AB;
    private static final int DIM = 0xFF566079;
    private static final int RED = 0xFF5555;
    private static final int GREEN = 0x55FF55;

    /** Flacher Button. hover läuft weich zwischen 0 und 1. */
    private static final class Btn {
        final String label;
        final String tip;
        final Runnable action;
        String sub = "";        // zweite Zeile (Inselzeile, Kachel)
        String value = "";      // Wert rechts, z. B. "An"
        int valueMode = 0;      // 0 neutral, 1 an (Akzentfarbe), 2 aus (gedimmt)
        int fixed = 0;          // 0 = Akzentfarbe, sonst feste RGB-Farbe (z. B. Rot)
        boolean left = false;
        boolean bold = false;
        boolean active = false; // dauerhaft hervorgehoben
        int x, y, w, h;
        float hover = 0f;
        boolean wasHovered = false;

        Btn(String label, String tip, Runnable action) {
            this.label = label;
            this.tip = tip;
            this.action = action;
        }

        Btn at(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        Btn sub(String s) {
            this.sub = s;
            return this;
        }

        Btn value(String v, int mode) {
            this.value = v;
            this.valueMode = mode;
            return this;
        }

        Btn fixed(int rgb) {
            this.fixed = rgb;
            return this;
        }

        Btn left() {
            this.left = true;
            return this;
        }

        Btn bold() {
            this.bold = true;
            return this;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** Schieberegler für ganze Zahlen. */
    private static final class Slider {
        final String tip;
        final int min, max, step;
        final IntSupplier get;
        final IntConsumer set;
        final IntFunction<String> text;
        int x, y, w, h;
        boolean dragging = false;

        Slider(String tip, int min, int max, int step, IntSupplier get, IntConsumer set, IntFunction<String> text) {
            this.tip = tip;
            this.min = min;
            this.max = max;
            this.step = step;
            this.get = get;
            this.set = set;
            this.text = text;
        }

        Slider at(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        int valueAt(double mx) {
            double frac = Math.max(0.0, Math.min(1.0, (mx - x) / (double) w));
            int raw = (int) Math.round((min + frac * (max - min)) / step) * step;
            return Math.max(min, Math.min(max, raw));
        }
    }

    private final List<Btn> buttons = new ArrayList<>();
    private final List<Slider> sliders = new ArrayList<>();
    private final float[] tabHover = new float[TAB_COUNT];
    private final boolean[] tabWasHovered = new boolean[TAB_COUNT];
    private final int[] tabX = new int[TAB_COUNT];
    private final int[] tabW = new int[TAB_COUNT];

    private Tab tab = Tab.HOME;
    private boolean formMode = false;
    private boolean listening = false;   // wartet auf neue Taste/Maustaste für "Menü öffnen"
    private boolean shiftDown = false;
    private String formName = "";
    private String formCmd = "";
    private String formError = "";
    private int page = 0;

    private EditBox nameBox;
    private EditBox cmdBox;

    // Layout (hängt von den Einstellungen ab)
    private int pw, ph, rowW, rowsPerPage, rowPitch;
    private int px, py;

    private float indicatorX = -1f;
    private float indicatorW = -1f;
    private long openedAt = System.nanoTime();
    private long lastFrame = System.nanoTime();

    public CustomGuiScreen() {
        super(Component.literal("Custom GUI"));
    }

    // ------------------------------------------------------------------ Aufbau

    private void computeLayout() {
        CustomGuiConfig c = CustomGuiConfig.get();
        pw = CustomGuiConfig.PANEL_WIDTHS[c.panelWidth];
        rowsPerPage = c.rowsPerPage;
        rowPitch = rowsPerPage >= 5 ? 32 : 36;
        rowW = pw - 36;
        ph = Math.max(262, 68 + rowsPerPage * rowPitch + 48);
        px = (this.width - pw) / 2;
        py = Math.max(2, (this.height - ph) / 2);
        computeTabs();
    }

    /** Tab-Breiten richten sich nach dem Text. */
    private void computeTabs() {
        int gap = 4;
        int total = gap * (TAB_COUNT - 1);
        for (int i = 0; i < TAB_COUNT; i++) {
            tabW[i] = this.font.width(Component.literal(Lang.t(TAB_KEYS[i])).withStyle(ChatFormatting.BOLD)) + 16;
            total += tabW[i];
        }
        int x = px + (pw - total) / 2;
        for (int i = 0; i < TAB_COUNT; i++) {
            tabX[i] = x;
            x += tabW[i] + gap;
        }
    }

    @Override
    protected void init() {
        computeLayout();

        nameBox = new EditBox(this.font, px + 30, py + 82, pw - 60, 20, Component.literal("Name"));
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.literal(Lang.t("form.name.hint")).withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(formName);
        nameBox.setResponder(s -> formName = s);
        this.addWidget(nameBox);

        cmdBox = new EditBox(this.font, px + 30, py + 128, pw - 60, 20, Component.literal("Command"));
        cmdBox.setMaxLength(100);
        cmdBox.setHint(Component.literal(Lang.t("form.cmd.hint")).withStyle(ChatFormatting.DARK_GRAY));
        cmdBox.setValue(formCmd);
        cmdBox.setResponder(s -> formCmd = s);
        this.addWidget(cmdBox);

        updateBoxes();
        rebuild();
        openedAt = System.nanoTime();
        lastFrame = openedAt;
    }

    /** Nach einer Layout- oder Sprachänderung: alles neu berechnen, ohne das Fenster neu zu öffnen. */
    private void applyLayout() {
        computeLayout();
        nameBox.setX(px + 30);
        nameBox.setY(py + 82);
        nameBox.setWidth(pw - 60);
        nameBox.setHint(Component.literal(Lang.t("form.name.hint")).withStyle(ChatFormatting.DARK_GRAY));
        cmdBox.setX(px + 30);
        cmdBox.setY(py + 128);
        cmdBox.setWidth(pw - 60);
        cmdBox.setHint(Component.literal(Lang.t("form.cmd.hint")).withStyle(ChatFormatting.DARK_GRAY));
        indicatorX = -1f;
        indicatorW = -1f;
        rebuild();
    }

    private void updateBoxes() {
        boolean show = formMode && tab == Tab.ISLANDS;
        nameBox.setVisible(show);
        nameBox.active = show;
        cmdBox.setVisible(show);
        cmdBox.active = show;
        if (show) {
            this.setFocused(nameBox);
            nameBox.setFocused(true);
        }
    }

    private static String onOff(boolean b) {
        return Lang.t(b ? "on" : "off");
    }

    /** Baut die Buttons für den aktuellen Tab neu auf. */
    private void rebuild() {
        buttons.clear();
        sliders.clear();
        CustomGuiConfig cfg = CustomGuiConfig.get();

        switch (tab) {
            case HOME -> buildHome(cfg);
            case ISLANDS -> buildIslands(cfg);
            case PVP -> buildPvp(cfg);
            case SETTINGS -> buildSettings(cfg);
        }
    }

    private void buildHome(CustomGuiConfig cfg) {
        if (cfg.mainLayout == 1) {
            int gap = 8;
            int bx = px + 20;
            int bw = (pw - 40 - gap) / 2;
            buttons.add(new Btn(Lang.t("home.spawn"), Lang.t("tip.spawn"), () -> runCommand("spawn"))
                    .sub("/spawn").bold().at(bx, py + 72, bw, 60));
            buttons.add(new Btn(Lang.t("home.shop"), Lang.t("tip.shop"), this::openShop)
                    .sub(Lang.t("sub.browser")).bold().at(bx + bw + gap, py + 72, bw, 60));
            buttons.add(new Btn(Lang.t("home.close"), Lang.t("tip.close"), this::onClose)
                    .fixed(RED).bold().at(bx, py + 72 + 60 + gap, pw - 40, 30));
        } else {
            int bw = Math.min(220, pw - 80);
            int bx = px + (pw - bw) / 2;
            buttons.add(new Btn(Lang.t("home.spawn"), Lang.t("tip.spawn"), () -> runCommand("spawn"))
                    .bold().at(bx, py + 74, bw, 30));
            buttons.add(new Btn(Lang.t("home.shop"), Lang.t("tip.shop"), this::openShop)
                    .bold().at(bx, py + 114, bw, 30));
            buttons.add(new Btn(Lang.t("home.close"), Lang.t("tip.close"), this::onClose)
                    .fixed(RED).bold().at(bx, py + 154, bw, 30));
        }
    }

    private void buildIslands(CustomGuiConfig cfg) {
        if (formMode) {
            int by = py + 172;
            buttons.add(new Btn(Lang.t("form.save"), Lang.t("tip.form.save"), this::saveIsland)
                    .fixed(GREEN).bold().at(px + pw / 2 - 120, by, 116, 28));
            buttons.add(new Btn(Lang.t("form.cancel"), Lang.t("tip.form.cancel"), () -> {
                formMode = false;
                formError = "";
                updateBoxes();
                rebuild();
            }).fixed(RED).bold().at(px + pw / 2 + 4, by, 116, 28));
            return;
        }

        List<CustomGuiConfig.Island> list = cfg.islands;
        int pages = pageCount();
        page = Math.max(0, Math.min(page, pages - 1));
        int rx = px + (pw - rowW) / 2;
        int rowH = rowPitch - 6;

        for (int i = 0; i < rowsPerPage; i++) {
            int idx = page * rowsPerPage + i;
            if (idx >= list.size()) break;
            CustomGuiConfig.Island is = list.get(idx);
            int ry = py + 68 + i * rowPitch;

            String cmd = is.command;
            buttons.add(new Btn(is.name, Lang.t("tip.isl.go", is.name), () -> runCommand(cmd))
                    .sub("/" + cmd).left().bold().at(rx, ry, rowW - 30, rowH));
            buttons.add(new Btn("x", Lang.t("tip.isl.remove"), () -> {
                list.remove(idx);
                CustomGuiConfig.save();
                rebuild();
            }).fixed(RED).bold().at(rx + rowW - 24, ry, 24, rowH));
        }

        int by = py + 68 + rowsPerPage * rowPitch + 4;
        if (pages > 1) {
            buttons.add(new Btn("<", Lang.t("tip.page.prev"), () -> {
                page = (page - 1 + pageCount()) % pageCount();
                rebuild();
            }).bold().at(rx, by, 24, 26));
            buttons.add(new Btn(Lang.t("isl.add") + " (" + (page + 1) + "/" + pages + ")", Lang.t("tip.isl.add"),
                    this::openForm).bold().at(rx + 30, by, rowW - 60, 26));
            buttons.add(new Btn(">", Lang.t("tip.page.next"), () -> {
                page = (page + 1) % pageCount();
                rebuild();
            }).bold().at(rx + rowW - 24, by, 24, 26));
        } else {
            buttons.add(new Btn(Lang.t("isl.add"), Lang.t("tip.isl.add"), this::openForm)
                    .bold().at(rx, by, rowW, 26));
        }
    }

    /** PvP-Reiter: Hitboxen um andere Spieler. */
    private void buildPvp(CustomGuiConfig cfg) {
        List<Btn> grid = new ArrayList<>();
        grid.add(new Btn(Lang.t("pvp.enabled"), Lang.t("tip.pvp.enabled"), () -> {
            cfg.hitboxEnabled = !cfg.hitboxEnabled;
            changed();
        }).value(onOff(cfg.hitboxEnabled), cfg.hitboxEnabled ? 1 : 2));
        grid.add(new Btn(Lang.t("pvp.color"), Lang.t("tip.pvp.color"), () -> {
            cfg.hitboxColor = (cfg.hitboxColor + 1) % CustomGuiConfig.HITBOX_COLOR_KEYS.length;
            changed();
        }).value(Lang.t(CustomGuiConfig.HITBOX_COLOR_KEYS[cfg.hitboxColor]), 0));
        grid.add(new Btn(Lang.t("pvp.speed"), Lang.t("tip.pvp.speed"), () -> {
            cfg.hitboxSpeed = (cfg.hitboxSpeed + 1) % CustomGuiConfig.SPEEDS.length;
            changed();
        }).value(Lang.t(CustomGuiConfig.SPEED_KEYS[cfg.hitboxSpeed]), 0));
        grid.add(new Btn(Lang.t("pvp.lines"), Lang.t("tip.pvp.lines"), () -> {
            cfg.hitboxThickness = (cfg.hitboxThickness + 1) % CustomGuiConfig.THICKNESSES.length;
            changed();
        }).value(Lang.t(CustomGuiConfig.THICKNESS_KEYS[cfg.hitboxThickness]), 0));
        grid.add(new Btn(Lang.t("pvp.walls"), Lang.t("tip.pvp.walls"), () -> {
            cfg.hitboxThroughWalls = !cfg.hitboxThroughWalls;
            changed();
        }).value(onOff(cfg.hitboxThroughWalls), cfg.hitboxThroughWalls ? 1 : 2));
        grid.add(new Btn(Lang.t("pvp.fill"), Lang.t("tip.pvp.fill"), () -> {
            cfg.hitboxFill = !cfg.hitboxFill;
            changed();
        }).value(onOff(cfg.hitboxFill), cfg.hitboxFill ? 1 : 2));
        grid.add(new Btn(Lang.t("pvp.self"), Lang.t("tip.pvp.self"), () -> {
            cfg.hitboxSelf = !cfg.hitboxSelf;
            changed();
        }).value(onOff(cfg.hitboxSelf), cfg.hitboxSelf ? 1 : 2));
        addGrid(grid, py + 80);

        sliders.add(new Slider(Lang.t("tip.pvp.range"), CustomGuiConfig.RANGE_MIN, CustomGuiConfig.RANGE_MAX, 8,
                () -> cfg.hitboxRangeBlocks,
                v -> cfg.hitboxRangeBlocks = v,
                v -> Lang.t("pvp.range", v))
                .at(px + 20, py + 190, pw - 40, 28));
    }

    /** Einstellungs-Reiter: Taste ändern, Sprache und Layout. */
    private void buildSettings(CustomGuiConfig cfg) {
        int kx = px + 20;
        int kw = pw - 40 - 70;

        Btn key = new Btn(Lang.t("set.key"), Lang.t("tip.set.key"), () -> {
            listening = !listening;
            rebuild();
        }).value(listening ? Lang.t("set.key.listen") : keyName(), listening ? 1 : 0);
        key.active = listening;
        buttons.add(key.at(kx, py + 78, kw, 22));

        buttons.add(new Btn(Lang.t("set.key.reset"), Lang.t("tip.set.key.reset"), this::resetKey)
                .at(kx + kw + 6, py + 78, 64, 22));

        List<Btn> grid = new ArrayList<>();
        grid.add(new Btn(Lang.t("set.lang"), Lang.t("tip.set.lang"), () -> {
            cfg.language = cfg.language == Lang.DE ? Lang.EN : Lang.DE;
            CustomGuiConfig.save();
            applyLayout();
        }).value(Lang.t("lang.name"), 0));
        grid.add(new Btn(Lang.t("set.accent"), Lang.t("tip.set.accent"), () -> {
            cfg.accent = (cfg.accent + 1) % CustomGuiConfig.ACCENTS.length;
            changed();
        }).value(Lang.t(CustomGuiConfig.ACCENT_KEYS[cfg.accent]), 0));
        grid.add(new Btn(Lang.t("set.width"), Lang.t("tip.set.width"), () -> {
            cfg.panelWidth = (cfg.panelWidth + 1) % CustomGuiConfig.PANEL_WIDTHS.length;
            CustomGuiConfig.save();
            applyLayout();
        }).value(Lang.t(CustomGuiConfig.WIDTH_KEYS[cfg.panelWidth]), 0));
        grid.add(new Btn(Lang.t("set.rows"), Lang.t("tip.set.rows"), () -> {
            cfg.rowsPerPage = cfg.rowsPerPage >= 5 ? 3 : cfg.rowsPerPage + 1;
            page = 0;
            CustomGuiConfig.save();
            applyLayout();
        }).value(String.valueOf(cfg.rowsPerPage), 0));
        grid.add(new Btn(Lang.t("set.layout"), Lang.t("tip.set.layout"), () -> {
            cfg.mainLayout = (cfg.mainLayout + 1) % CustomGuiConfig.LAYOUT_KEYS.length;
            changed();
        }).value(Lang.t(CustomGuiConfig.LAYOUT_KEYS[cfg.mainLayout]), 0));
        grid.add(new Btn(Lang.t("set.anim"), Lang.t("tip.set.anim"), () -> {
            cfg.animations = !cfg.animations;
            changed();
        }).value(onOff(cfg.animations), cfg.animations ? 1 : 2));
        addGrid(grid, py + 120);

        buttons.add(new Btn(Lang.t("set.reset"), Lang.t("tip.set.reset"), () -> {
            cfg.resetLayout();
            page = 0;
            CustomGuiConfig.save();
            applyLayout();
        }).fixed(RED).at(kx, py + 206, pw - 40, 22));
    }

    /** Ordnet Buttons in zwei Spalten an. */
    private void addGrid(List<Btn> grid, int gy) {
        int kx = px + 20;
        int gap = 6;
        int gw = (pw - 40 - gap) / 2;
        for (int i = 0; i < grid.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            buttons.add(grid.get(i).at(kx + col * (gw + gap), gy + row * 26, gw, 22));
        }
    }

    /** Speichert die Konfiguration und baut die Buttons mit den neuen Werten neu auf. */
    private void changed() {
        CustomGuiConfig.save();
        rebuild();
    }

    private int pageCount() {
        int n = CustomGuiConfig.get().islands.size();
        return Math.max(1, (n + rowsPerPage - 1) / rowsPerPage);
    }

    private void openForm() {
        formMode = true;
        formError = "";
        updateBoxes();
        rebuild();
    }

    private void saveIsland() {
        String name = formName.trim();
        String cmd = formCmd.trim();
        while (cmd.startsWith("/")) {
            cmd = cmd.substring(1).trim();
        }
        if (name.isEmpty()) {
            formError = Lang.t("form.err.name");
            return;
        }
        if (cmd.isEmpty()) {
            formError = Lang.t("form.err.cmd");
            return;
        }
        CustomGuiConfig.get().islands.add(new CustomGuiConfig.Island(name, cmd));
        CustomGuiConfig.save();

        formName = "";
        formCmd = "";
        nameBox.setValue("");
        cmdBox.setValue("");
        formError = "";
        formMode = false;
        page = pageCount() - 1;
        updateBoxes();
        rebuild();
    }

    // ------------------------------------------------------------------ Taste

    private static String keyName() {
        return CustomGuiMod.OPEN_KEY.getTranslatedKeyMessage().getString();
    }

    private void bindKey(InputConstants.Key key) {
        CustomGuiMod.OPEN_KEY.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = false;
        rebuild();
    }

    private void resetKey() {
        KeyMapping k = CustomGuiMod.OPEN_KEY;
        bindKey(k.getDefaultKey());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int code = event.key();
        if (code == GLFW.GLFW_KEY_LEFT_SHIFT || code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            shiftDown = true;
        }
        if (listening) {
            if (code == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                rebuild();
            } else {
                bindKey(InputConstants.Type.KEYSYM.getOrCreate(code));
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        int code = event.key();
        if (code == GLFW.GLFW_KEY_LEFT_SHIFT || code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            shiftDown = false;
        }
        return super.keyReleased(event);
    }

    // ------------------------------------------------------------------ Aktionen

    private void runCommand(String command) {
        String cmd = command;
        while (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand(cmd);
        }
        this.onClose();
    }

    private void openShop() {
        try {
            openInBrowser(CustomGuiConfig.get().shopUrl);
        } catch (Exception e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.literal(Lang.t("err.shop", e.getMessage())).withStyle(ChatFormatting.RED));
            }
        }
        this.onClose();
    }

    /** Öffnet einen http(s)-Link im Standard-Browser (nur mit Java-Mitteln, ohne Minecraft-API). */
    private static void openInBrowser(String url) throws Exception {
        URI uri = URI.create(url.trim());
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(Lang.t("err.http"));
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri.toString());
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("open", uri.toString());
        } else {
            pb = new ProcessBuilder("xdg-open", uri.toString());
        }
        pb.start();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ Rendern

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gfx, mouseX, mouseY, delta); // Hintergrund

        CustomGuiConfig cfg = CustomGuiConfig.get();
        boolean anim = cfg.animations;
        int accentC = 0xFF000000 | CustomGuiConfig.ACCENTS[cfg.accent];

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        float time = (now - openedAt) / 1_000_000_000f;

        float open = 1f;
        int slide = 0;
        if (anim) {
            open = Math.min(1f, time / 0.25f);
            open = 1f - (float) Math.pow(1f - open, 3);
            slide = Math.round((1f - open) * 12f);
        }
        boolean interactive = open > 0.95f;

        // ---- Panel: flach, mit dünner Akzentlinie unter dem Kopf
        int x1 = px, y1 = py + slide, x2 = px + pw, y2 = py + ph + slide;
        gfx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, withAlpha(0xFF000000, open * 0.45f));
        gfx.fill(x1, y1, x2, y2, withAlpha(BORDER, open));
        gfx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, withAlpha(BG, open * 0.98f));
        gfx.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 28, withAlpha(HEADER, open));
        gfx.fill(x1 + 1, y1 + 28, x2 - 1, y1 + 29, withAlpha(accentC, open));

        int tx = x1 + 14;
        int ty = y1 + 10;
        Component t1 = Component.literal("Cytooxien").withStyle(ChatFormatting.BOLD);
        gfx.text(this.font, t1, tx, ty, withAlpha(TEXT, open));
        tx += this.font.width(t1);
        Component t2 = Component.literal(" \u00BB ").withStyle(ChatFormatting.BOLD);
        gfx.text(this.font, t2, tx, ty, withAlpha(accentC, open));
        tx += this.font.width(t2);
        gfx.text(this.font, Component.literal(Lang.t("title.menu")), tx, ty, withAlpha(MUTED, open));
        Component url = Component.literal("cytooxien.net");
        gfx.text(this.font, url, x2 - 14 - this.font.width(url), ty, withAlpha(DIM, open));

        drawTabs(gfx, mouseX, mouseY - slide, dt, open, slide, interactive, anim, accentC);

        // ---- Inhalt je Tab
        if (tab == Tab.PVP) {
            drawLabel(gfx, Lang.t("pvp.section"), px + 20, py + 66 + slide, open);
        } else if (tab == Tab.SETTINGS) {
            drawLabel(gfx, Lang.t("set.keysec"), px + 20, py + 66 + slide, open);
            drawLabel(gfx, Lang.t("set.layoutsec"), px + 20, py + 108 + slide, open);
        } else if (tab == Tab.ISLANDS) {
            if (formMode) {
                drawLabel(gfx, Lang.t("form.name"), px + 30, py + 70 + slide, open);
                drawLabel(gfx, Lang.t("form.cmd"), px + 30, py + 116 + slide, open);
                nameBox.extractRenderState(gfx, mouseX, mouseY, delta);
                cmdBox.extractRenderState(gfx, mouseX, mouseY, delta);
                if (!formError.isEmpty()) {
                    Component err = Component.literal(formError).withStyle(ChatFormatting.RED);
                    gfx.text(this.font, err, this.width / 2 - this.font.width(err) / 2,
                            py + 156 + slide, withAlpha(0xFFFFFFFF, open));
                }
            } else if (cfg.islands.isEmpty()) {
                Component e1 = Component.literal(Lang.t("isl.empty1"));
                Component e2 = Component.literal(Lang.t("isl.empty2"));
                gfx.text(this.font, e1, this.width / 2 - this.font.width(e1) / 2, py + 110 + slide, withAlpha(MUTED, open));
                gfx.text(this.font, e2, this.width / 2 - this.font.width(e2) / 2, py + 124 + slide, withAlpha(DIM, open));
            }
        }

        // ---- Buttons und Regler
        String hoverTip = null;
        for (Btn b : buttons) {
            boolean hovered = interactive && b.contains(mouseX, mouseY - slide);
            if (hovered) hoverTip = b.tip;
            if (hovered && !b.wasHovered) playUi(1.8f);
            b.wasHovered = hovered;
            float target = hovered ? 1f : 0f;
            b.hover = anim ? b.hover + (target - b.hover) * (1f - (float) Math.exp(-dt * 14f)) : target;
            drawButton(gfx, b, slide, open, accentC);
        }
        for (Slider s : sliders) {
            boolean hovered = interactive && s.contains(mouseX, mouseY - slide);
            if (hovered || s.dragging) hoverTip = s.tip;
            drawSlider(gfx, s, slide, open, accentC, hovered || s.dragging);
        }

        // ---- Fußzeile: Hinweis auf Shift-Erklärungen
        Component hint = Component.literal(Lang.t("hint.shift"));
        gfx.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, py + ph - 14 + slide, withAlpha(DIM, open));

        // ---- Tooltip mit Erklärung (nur mit gedrückter Shift-Taste)
        if (shiftDown && hoverTip != null && !hoverTip.isEmpty()) {
            drawTooltip(gfx, mouseX, mouseY, hoverTip, accentC);
        }
    }

    private void drawTabs(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float dt, float open,
                          int slide, boolean interactive, boolean anim, int accentC) {
        int tabH = 20;
        int ty = py + 34 + slide;

        int selected = tab.ordinal();
        float targetX = tabX[selected];
        float targetW = tabW[selected];
        if (indicatorX < 0 || !anim) {
            indicatorX = targetX;
            indicatorW = targetW;
        }
        float k = 1f - (float) Math.exp(-dt * 12f);
        indicatorX += (targetX - indicatorX) * k;
        indicatorW += (targetW - indicatorW) * k;

        for (int i = 0; i < TAB_COUNT; i++) {
            int tx = tabX[i];
            int tw = tabW[i];
            boolean hovered = interactive && mouseX >= tx && mouseX < tx + tw
                    && mouseY >= py + 34 && mouseY < py + 34 + tabH;
            if (hovered && !tabWasHovered[i] && i != selected) playUi(1.8f);
            tabWasHovered[i] = hovered;
            float target = hovered ? 1f : 0f;
            tabHover[i] = anim ? tabHover[i] + (target - tabHover[i]) * (1f - (float) Math.exp(-dt * 14f)) : target;

            int base = i == selected ? TEXT : MUTED;
            int col = lerpColor(base, TEXT, tabHover[i]);
            Component c = Component.literal(Lang.t(TAB_KEYS[i])).withStyle(ChatFormatting.BOLD);
            gfx.text(this.font, c, tx + tw / 2 - this.font.width(c) / 2,
                    ty + tabH / 2 - this.font.lineHeight / 2, withAlpha(col, open));
        }
        int ux = Math.round(indicatorX);
        gfx.fill(ux + 6, ty + tabH + 1, ux + Math.round(indicatorW) - 6, ty + tabH + 3, withAlpha(accentC, open));
    }

    private void drawLabel(GuiGraphicsExtractor gfx, String text, int x, int y, float open) {
        gfx.text(this.font, Component.literal(text), x, y, withAlpha(MUTED, open));
    }

    private void drawButton(GuiGraphicsExtractor gfx, Btn b, int slide, float open, int themeAccent) {
        float h = b.active ? 1f : b.hover;
        int x1 = b.x;
        int y1 = b.y + slide;
        int x2 = b.x + b.w;
        int y2 = b.y + slide + b.h;
        int accent = b.fixed != 0 ? (0xFF000000 | b.fixed) : themeAccent;

        int border = lerpColor(BTN_BORDER, accent, h);
        int bg = lerpColor(BTN, mixWithBlack(accent, 0.22f), h * 0.9f);
        gfx.fill(x1, y1, x2, y2, withAlpha(border, open));
        gfx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, withAlpha(bg, open));

        int textColor = lerpColor(0xFFC9D0DE, 0xFFFFFFFF, h);
        Component label = b.bold ? Component.literal(b.label).withStyle(ChatFormatting.BOLD) : Component.literal(b.label);
        boolean hasSub = !b.sub.isEmpty();
        int midY = y1 + (b.h - this.font.lineHeight) / 2 + 1;

        if (b.left) {
            int ny = y1 + (b.h >= 30 ? 5 : 3);
            int sy = y1 + (b.h >= 30 ? 17 : 14);
            gfx.text(this.font, label, x1 + 10, ny, withAlpha(textColor, open));
            if (hasSub) {
                gfx.text(this.font, Component.literal(b.sub), x1 + 10, sy, withAlpha(MUTED, open));
            }
        } else if (hasSub) {
            // Kachel: Titel mittig, darunter kleine Zeile
            int cy = (y1 + y2) / 2;
            gfx.text(this.font, label, (x1 + x2) / 2 - this.font.width(label) / 2, cy - this.font.lineHeight, withAlpha(textColor, open));
            Component sub = Component.literal(b.sub);
            gfx.text(this.font, sub, (x1 + x2) / 2 - this.font.width(sub) / 2, cy + 3, withAlpha(MUTED, open));
        } else if (!b.value.isEmpty()) {
            // Beschriftung links, Wert rechts
            gfx.text(this.font, label, x1 + 8, midY, withAlpha(textColor, open));
            int vc = b.valueMode == 1 ? accent : (b.valueMode == 2 ? DIM : TEXT);
            Component val = Component.literal(b.value);
            gfx.text(this.font, val, x2 - 8 - this.font.width(val), midY, withAlpha(vc, open));
        } else {
            gfx.text(this.font, label, (x1 + x2) / 2 - this.font.width(label) / 2, midY, withAlpha(textColor, open));
        }
    }

    private void drawSlider(GuiGraphicsExtractor gfx, Slider s, int slide, float open, int accent, boolean hot) {
        int v = s.get.getAsInt();
        gfx.text(this.font, Component.literal(s.text.apply(v)), s.x, s.y + slide, withAlpha(TEXT, open));

        int ty = s.y + slide + 17;
        float frac = (v - s.min) / (float) (s.max - s.min);
        int kx = s.x + Math.round(frac * s.w);
        gfx.fill(s.x, ty, s.x + s.w, ty + 4, withAlpha(BTN_BORDER, open));
        gfx.fill(s.x, ty, kx, ty + 4, withAlpha(accent, open));
        int knob = hot ? 0xFFFFFFFF : TEXT;
        gfx.fill(kx - 3, ty - 4, kx + 3, ty + 8, withAlpha(knob, open));
    }

    /** Erklärungs-Tooltip neben dem Mauszeiger. */
    private void drawTooltip(GuiGraphicsExtractor gfx, int mouseX, int mouseY, String text, int accent) {
        List<String> lines = wrap(text, 190);
        int textW = 0;
        for (String l : lines) {
            textW = Math.max(textW, this.font.width(l));
        }
        int lh = this.font.lineHeight + 2;
        int w = textW + 14;
        int h = lines.size() * lh + 8;
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + w > this.width - 4) x = mouseX - w - 8;
        if (y + h > this.height - 4) y = this.height - h - 4;
        x = Math.max(4, x);
        y = Math.max(4, y);

        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, accent);
        gfx.fill(x, y, x + w, y + h, 0xFF0A0F1A);
        int ly = y + 5;
        for (String l : lines) {
            gfx.text(this.font, Component.literal(l), x + 7, ly, TEXT);
            ly += lh;
        }
    }

    private List<String> wrap(String text, int maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String attempt = cur.length() == 0 ? word : cur + " " + word;
            if (this.font.width(attempt) > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(attempt);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Beim Tastenwechsel zählt jede Maustaste (links, rechts, Mitte ...) als neue Taste
        if (listening) {
            bindKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true; // Klick auf ein Textfeld
        }
        if (event.button() == 0) {
            // Tabs
            int tabH = 20;
            for (int i = 0; i < TAB_COUNT; i++) {
                if (event.x() >= tabX[i] && event.x() < tabX[i] + tabW[i]
                        && event.y() >= py + 34 && event.y() < py + 34 + tabH) {
                    Tab target = Tab.values()[i];
                    if (target != tab) {
                        tab = target;
                        formMode = false;
                        formError = "";
                        page = 0;
                        updateBoxes();
                        rebuild();
                        playUi(1.0f);
                    }
                    return true;
                }
            }
            // Regler
            for (Slider s : sliders) {
                if (s.contains(event.x(), event.y())) {
                    s.dragging = true;
                    s.set.accept(s.valueAt(event.x()));
                    return true;
                }
            }
            // Buttons
            for (Btn b : buttons) {
                if (b.contains(event.x(), event.y())) {
                    playUi(1.0f);
                    b.action.run();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        for (Slider s : sliders) {
            if (s.dragging) {
                s.set.accept(s.valueAt(event.x()));
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean wasDragging = false;
        for (Slider s : sliders) {
            if (s.dragging) {
                s.dragging = false;
                wasDragging = true;
            }
        }
        if (wasDragging) {
            CustomGuiConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    private void playUi(float pitch) {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    // ------------------------------------------------------------------ Helfer

    private static int withAlpha(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerp((c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF, t);
        int r = lerp((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = lerp((c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF, t);
        int b = lerp(c1 & 0xFF, c2 & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static int mixWithBlack(int argb, float keep) {
        int r = Math.round(((argb >> 16) & 0xFF) * keep);
        int g = Math.round(((argb >> 8) & 0xFF) * keep);
        int b = Math.round((argb & 0xFF) * keep);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
