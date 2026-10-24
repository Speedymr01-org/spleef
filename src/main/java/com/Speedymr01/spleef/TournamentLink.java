package com.Speedymr01.spleef;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Points players and admins at TournamentManager, which can run Spleef matches
 * inside brackets and tournaments.
 *
 * <p>The chat message carries a clickable link; the console message is plain
 * text (consoles cannot render click events).</p>
 */
public final class TournamentLink {

    /** Modrinth page for TournamentManager. */
    public static final String URL = "https://modrinth.com/plugin/tournament-manager";

    private TournamentLink() {
    }

    /** Chat message sent to players when a game ends. */
    public static Component chatMessage() {
        HoverEvent<Component> openHint =
                HoverEvent.showText(Component.text("Click to open on Modrinth", NamedTextColor.GRAY));

        Component name = Component.text("TournamentManager", NamedTextColor.AQUA, TextDecoration.BOLD)
                .clickEvent(ClickEvent.openUrl(URL))
                .hoverEvent(openHint);

        Component link = Component.text(URL, NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(URL))
                .hoverEvent(openHint);

        return Component.text("You can create tournaments using this plugin with ", NamedTextColor.GRAY)
                .append(name)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(link);
    }

    /** Console line logged when the plugin enables. */
    public static String consoleMessage() {
        return "You can create tournaments using this plugin with TournamentManager: " + URL;
    }
}
