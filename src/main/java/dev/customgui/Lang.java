package dev.customgui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Einfache Übersetzung für das Menü (Deutsch / Englisch), unabhängig von der Minecraft-Sprache. */
public final class Lang {

    public static final int DE = 0;
    public static final int EN = 1;

    private static final Map<String, String[]> T = new HashMap<>();

    private static void a(String key, String de, String en) {
        T.put(key, new String[]{de, en});
    }

    static {
        a("title.menu", "Men\u00FC", "Menu");
        a("hint.shift", "Shift halten f\u00FCr Erkl\u00E4rungen", "Hold Shift for help");
        a("on", "An", "On");
        a("off", "Aus", "Off");

        // Tabs
        a("tab.home", "Hauptmen\u00FC", "Main menu");
        a("tab.islands", "Inseln", "Islands");
        a("tab.pvp", "PvP", "PvP");
        a("tab.settings", "Einstellungen", "Settings");

        // Hauptmenü
        a("home.spawn", "Spawn", "Spawn");
        a("tip.spawn", "F\u00FChrt /spawn aus.", "Runs /spawn.");
        a("home.shop", "Shop", "Shop");
        a("sub.browser", "Im Browser", "In browser");
        a("tip.shop", "\u00D6ffnet den Shop im Standard-Browser.", "Opens the shop in your default browser.");
        a("home.close", "Schlie\u00DFen", "Close");
        a("tip.close", "Schlie\u00DFt dieses Men\u00FC.", "Closes this menu.");

        // Inseln
        a("isl.empty1", "Noch keine Inseln gespeichert.", "No islands saved yet.");
        a("isl.empty2", "F\u00FCge deine erste Insel hinzu!", "Add your first island!");
        a("isl.add", "+ Insel hinzuf\u00FCgen", "+ Add island");
        a("tip.isl.add", "Speichert eine neue Insel mit dem Befehl, der dich dorthin bringt.", "Saves a new island together with the command that takes you there.");
        a("tip.isl.go", "Klicke, um dich zu %s zu bringen.", "Click to travel to %s.");
        a("tip.isl.remove", "Entfernt diese Insel aus der Liste.", "Removes this island from the list.");
        a("tip.page.prev", "Vorherige Seite.", "Previous page.");
        a("tip.page.next", "N\u00E4chste Seite.", "Next page.");
        a("form.name", "Name der Insel", "Island name");
        a("form.cmd", "Befehl (zum Hinkommen)", "Command (to get there)");
        a("form.name.hint", "z. B. Markshop", "e.g. Markshop");
        a("form.cmd.hint", "z. B. /warp Markshop", "e.g. /warp Markshop");
        a("form.save", "Speichern", "Save");
        a("tip.form.save", "Speichert die Insel.", "Saves the island.");
        a("form.cancel", "Abbrechen", "Cancel");
        a("tip.form.cancel", "Zur\u00FCck zur Liste.", "Back to the list.");
        a("form.err.name", "Bitte gib einen Namen ein.", "Please enter a name.");
        a("form.err.cmd", "Bitte gib einen Befehl ein.", "Please enter a command.");
        a("err.shop", "Shop-Link konnte nicht ge\u00F6ffnet werden: %s", "Could not open the shop link: %s");
        a("err.http", "Es sind nur http(s)-Links erlaubt", "Only http(s) links are allowed");

        // Einstellungen
        a("set.keysec", "Tastenbelegung", "Key binding");
        a("set.layoutsec", "Layout", "Layout");
        a("set.key", "Men\u00FC-Taste", "Menu key");
        a("set.key.listen", "Dr\u00FCcke Taste / Maus ...", "Press key / mouse ...");
        a("tip.set.key", "Klicken, dann eine Taste oder Maustaste dr\u00FCcken (links/rechts m\u00F6glich). Esc bricht ab. Achtung: Maustasten k\u00F6nnen mit Angriff/Benutzen kollidieren.",
                "Click, then press a key or mouse button (left/right possible). Esc cancels. Note: mouse buttons can clash with attack/use.");
        a("set.key.reset", "Standard", "Default");
        a("tip.set.key.reset", "Setzt die Men\u00FC-Taste auf H zur\u00FCck.", "Resets the menu key to H.");
        a("set.lang", "Sprache", "Language");
        a("lang.name", "Deutsch", "English");
        a("tip.set.lang", "Wechselt die Sprache des Men\u00FCs zwischen Deutsch und Englisch.", "Switches the menu language between German and English.");
        a("set.accent", "Akzent", "Accent");
        a("tip.set.accent", "Akzentfarbe des Men\u00FCs.", "Accent color of the menu.");
        a("accent.cyan", "Cyan", "Cyan");
        a("accent.green", "Gr\u00FCn", "Green");
        a("accent.purple", "Lila", "Purple");
        a("accent.gold", "Gold", "Gold");
        a("accent.pink", "Rosa", "Pink");
        a("accent.white", "Wei\u00DF", "White");
        a("set.width", "Breite", "Width");
        a("tip.set.width", "Wie breit das Men\u00FC ist.", "How wide the menu is.");
        a("width.narrow", "Schmal", "Narrow");
        a("width.normal", "Normal", "Normal");
        a("width.wide", "Breit", "Wide");
        a("set.rows", "Inseln/Seite", "Islands/page");
        a("tip.set.rows", "Wie viele Inseln pro Seite angezeigt werden (3 bis 5).", "How many islands are shown per page (3 to 5).");
        a("set.layout", "Hauptmen\u00FC", "Main menu");
        a("tip.set.layout", "Zeigt das Hauptmen\u00FC als Liste oder als Kacheln.", "Shows the main menu as a list or as tiles.");
        a("layout.list", "Liste", "List");
        a("layout.tiles", "Kacheln", "Tiles");
        a("set.anim", "Animationen", "Animations");
        a("tip.set.anim", "Gleit- und Hover-Effekte ein- oder ausschalten.", "Turns slide and hover effects on or off.");
        a("set.reset", "Layout zur\u00FCcksetzen", "Reset layout");
        a("tip.set.reset", "Setzt Layout und Akzent auf Standard. Inseln und PvP-Einstellungen bleiben.", "Resets layout and accent. Islands and PvP settings are kept.");

        // PvP
        a("pvp.section", "Hitboxen um andere Spieler", "Hitboxes around other players");
        a("pvp.enabled", "Hitboxen", "Hitboxes");
        a("tip.pvp.enabled", "Zeigt Hitboxen um andere Spieler an. Deine eigene Box siehst du in der Ego-Perspektive nie.", "Shows hitboxes around other players. You never see your own box in first person.");
        a("pvp.color", "Farbe", "Color");
        a("tip.pvp.color", "RGB = wandernder Regenbogen, sonst eine feste Farbe.", "RGB = moving rainbow, otherwise a fixed color.");
        a("col.rgb", "RGB", "RGB");
        a("col.red", "Rot", "Red");
        a("col.green", "Gr\u00FCn", "Green");
        a("col.blue", "Blau", "Blue");
        a("col.white", "Wei\u00DF", "White");
        a("col.gold", "Gold", "Gold");
        a("pvp.speed", "Tempo", "Speed");
        a("tip.pvp.speed", "Wie schnell der Regenbogen wandert.", "How fast the rainbow moves.");
        a("speed.slow", "Langsam", "Slow");
        a("speed.normal", "Normal", "Normal");
        a("speed.fast", "Schnell", "Fast");
        a("pvp.lines", "Linien", "Lines");
        a("tip.pvp.lines", "Dicke der Hitbox-Linien.", "Thickness of the hitbox lines.");
        a("lines.thin", "D\u00FCnn", "Thin");
        a("lines.medium", "Mittel", "Medium");
        a("lines.thick", "Dick", "Thick");
        a("pvp.walls", "Durch W\u00E4nde", "Through walls");
        a("tip.pvp.walls", "Hitboxen auch hinter Bl\u00F6cken sichtbar machen.", "Makes hitboxes visible behind blocks.");
        a("pvp.fill", "F\u00FCllung", "Fill");
        a("tip.pvp.fill", "Zus\u00E4tzlich eine leicht durchsichtige Fl\u00E4che.", "Adds a slightly transparent surface.");
        a("pvp.self", "Eigene Box", "Own box");
        a("tip.pvp.self", "Zeigt deine eigene Hitbox, aber nur in der 3. Person.", "Shows your own hitbox, but only in third person.");
        a("pvp.range", "Reichweite: %s Bl\u00F6cke", "Range: %s blocks");
        a("tip.pvp.range", "Bis zu welcher Entfernung Hitboxen angezeigt werden. Regler ziehen oder anklicken.", "Maximum distance at which hitboxes are shown. Drag or click the slider.");
    }

    private Lang() {
    }

    public static String t(String key) {
        String[] v = T.get(key);
        if (v == null) return key;
        int l = CustomGuiConfig.get().language;
        return v[l == EN ? EN : DE];
    }

    public static String t(String key, Object... args) {
        return String.format(Locale.ROOT, t(key), args);
    }
}
