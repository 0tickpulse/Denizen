package com.denizenscript.denizen.scripts.commands.player;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.objects.ColorTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.Collections;
import java.util.List;

public class DebugBlockCommand extends AbstractCommand {

    public DebugBlockCommand() {
        setName("debugblock");
        setSyntax("debugblock [<location>|.../clear] (color:<color>) (alpha:<#.#>) (name:<name>) (players:<player>|...) (d:<duration>{10s})");
        setRequiredArguments(1, 6);
        isProcedural = false;
    }

    // <--[command]
    // @Name DebugBlock
    // @Syntax debugblock [<location>|.../clear] (color:<color>) (alpha:<#.#>) (name:<name>) (players:<player>|...) (d:<duration>{10s})
    // @Required 1
    // @Maximum 6
    // @Short Shows or clears minecraft debug blocks.
    // @Synonyms GameTestMarker
    // @Group player
    //
    // @Description
    // Shows or clears minecraft debug blocks, AKA "Game Test Markers".
    // These are block-grid-aligned markers that are a perfect cube of a single (specifiable) transparent color, and stay for a specified duration of time or until cleared.
    // Markers can optionally also have simple text names.
    //
    // If arguments are unspecified, the default color is white (in practice: green), the default alpha is 1.0 (most opaque, but not completely opaque),
    // the default player is the linked player, the default name is none, and the default duration is 10 seconds.
    //
    // The underlying color input is a full color value, however the current minecraft client can only render shades between green and gray (ie the red and blue color channels are ignored).
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to show a debug block where the player is looking.
    // - debugblock <player.cursor_on>
    //
    // @Usage
    // Use to show a transparent green debug block in front of the player for five seconds.
    // - debugblock <player.eye_location.forward[2]> color:green alpha:0.5 d:5s
    //
    // @Usage
    // Use to remove all debug blocks,
    // - debugblock clear
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("players")
                    && arg.matchesPrefix("to", "players")) {
                scriptEntry.addObject("players", arg.asType(ListTag.class).filter(PlayerTag.class, scriptEntry));
            }
            else if (arg.matchesPrefix("d", "duration")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("duration", arg.asType(DurationTag.class));
            }
            else if (arg.matchesPrefix("color")
                    && arg.matchesArgumentType(ColorTag.class)) {
                scriptEntry.addObject("color", arg.asType(ColorTag.class));
            }
            else if (arg.matchesPrefix("alpha")
                    && arg.matchesFloat()) {
                scriptEntry.addObject("alpha", arg.asElement());
            }
            else if (arg.matchesPrefix("name")) {
                scriptEntry.addObject("name", arg.asElement());
            }
            else if (arg.matches("clear")) {
                scriptEntry.addObject("clear", new ElementTag(true));
            }
            else if (!scriptEntry.hasObject("locations")
                    && arg.matchesArgumentList(LocationTag.class)) {
                scriptEntry.addObject("locations", arg.asType(ListTag.class).filter(LocationTag.class, scriptEntry));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("players") && Utilities.entryHasPlayer(scriptEntry)) {
            scriptEntry.defaultObject("players", Collections.singletonList(Utilities.getEntryPlayer(scriptEntry)));
        }
        if (!scriptEntry.hasObject("locations") && !scriptEntry.hasObject("clear")) {
            throw new InvalidArgumentsException("Must specify at least one valid location!");
        }
        if (!scriptEntry.hasObject("players")) {
            throw new InvalidArgumentsException("Must have a valid, online player attached!");
        }
        scriptEntry.defaultObject("duration", new DurationTag(10));
        scriptEntry.defaultObject("color", new ColorTag(255, 255, 255));
        scriptEntry.defaultObject("alpha", new ElementTag("1"));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        DurationTag duration = scriptEntry.getObjectTag("duration");
        ElementTag clear = scriptEntry.getElement("clear");
        List<LocationTag> locations = (List<LocationTag>) scriptEntry.getObject("locations");
        List<PlayerTag> players = (List<PlayerTag>) scriptEntry.getObject("players");
        ColorTag color = scriptEntry.getObjectTag("color");
        ElementTag alpha = scriptEntry.getElement("alpha");
        ElementTag name = scriptEntry.getElement("name");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), clear == null ? ArgumentHelper.debugList("locations", locations) : clear, duration, ArgumentHelper.debugList("players", players), color, alpha, name);
        }
        if (clear != null && clear.asBoolean()) {
            for (PlayerTag player : players) {
                NMSHandler.getPacketHelper().clearDebugTestMarker(player.getPlayerEntity());
            }
        }
        else {
            int alphaInt = (int) (alpha.asFloat() * 255);
            for (LocationTag location : locations) {
                for (PlayerTag player : players) {
                    NMSHandler.getPacketHelper().showDebugTestMaker(player.getPlayerEntity(), location, color, alphaInt, name == null ? "" : name.asString(), (int) duration.getMillis());
                }
            }
        }
    }
}
