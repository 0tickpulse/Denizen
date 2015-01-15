package net.aufdemrand.denizen.scripts.commands.world;

import net.aufdemrand.denizen.utilities.blocks.BlockData;
import net.aufdemrand.denizen.utilities.blocks.CuboidBlockSet;
import net.aufdemrand.denizencore.scripts.ScriptHelper;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.objects.*;
import net.aufdemrand.denizencore.objects.*;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizen.utilities.DenizenAPI;
import net.aufdemrand.denizen.utilities.debugging.dB;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class SchematicCommand extends AbstractCommand {

    @Override
    public void onEnable() {
        TagManager.registerTagEvents(this);
        schematics = new HashMap<String, CuboidBlockSet>();
    }


    private enum Type { CREATE, LOAD, UNLOAD, ROTATE, PASTE, SAVE }
    public static Map<String, CuboidBlockSet> schematics;

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        // - schematic create name:Potato cu@x,y,z,world|x,y,z,world origin:x,y,z,world
        // - schematic load name:Potato
        // - schematic unload name:Potato
        // - schematic rotate name:Potato angle:90
        // - schematic paste name:Potato location:x,y,z,world (noair)
        // - schematic save name:Potato
        // - schematic [load/unload/rotate/paste] [name:<name>] (angle:<#>) (<location>) (noair)

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("type")
                    && arg.matchesEnum(Type.values()))
                scriptEntry.addObject("type", new Element(arg.raw_value.toUpperCase()));

            else if (!scriptEntry.hasObject("name")
                    && arg.matchesPrefix("name"))
                scriptEntry.addObject("name", arg.asElement());

            else if (!scriptEntry.hasObject("angle")
                    && arg.matchesPrimitive(aH.PrimitiveType.Integer))
                scriptEntry.addObject("angle", arg.asElement());

            else if (!scriptEntry.hasObject("location")
                    && arg.matchesArgumentType(dLocation.class))
                scriptEntry.addObject("location", arg.asType(dLocation.class));

            else if (!scriptEntry.hasObject("cuboid")
                    && arg.matchesArgumentType(dCuboid.class))
                scriptEntry.addObject("cuboid", arg.asType(dCuboid.class));

            else
                arg.reportUnhandled();
        }

        if (!scriptEntry.hasObject("type"))
            throw new InvalidArgumentsException("Missing type argument!");

        if (!scriptEntry.hasObject("name"))
            throw new InvalidArgumentsException("Missing name argument!");

    }


    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        Element angle = scriptEntry.getElement("angle");
        Element type = scriptEntry.getElement("type");
        Element name = scriptEntry.getElement("name");
        dLocation location = scriptEntry.getdObject("location");
        dCuboid cuboid = scriptEntry.getdObject("cuboid");

        dB.report(scriptEntry, getName(), type.debug()
                + name.debug()
                + (location != null ? location.debug(): "")
                + (cuboid != null ? cuboid.debug(): "")
                + (angle != null ? angle.debug(): ""));

        CuboidBlockSet set;
        switch (Type.valueOf(type.asString())) {
            case CREATE:
                if (schematics.containsKey(name.asString().toUpperCase())) {
                    dB.echoError("Schematic file " + name.asString() + " is already loaded.");
                    return;
                }
                if (cuboid == null) {
                    dB.echoError("Missing cuboid argument!");
                    return;
                }
                if (location == null) {
                    dB.echoError("Missing origin location argument!");
                    return;
                }
                try {
                    set = new CuboidBlockSet(cuboid, location);
                    schematics.put(name.asString().toUpperCase(), set);
                }
                catch (Exception ex) {
                    dB.echoError("Error creating schematic object " + name.asString() + ".");
                    dB.echoError(ex);
                    return;
                }
                break;
            case LOAD:
                if (schematics.containsKey(name.asString().toUpperCase())) {
                    dB.echoError("Schematic file " + name.asString() + " is already loaded.");
                    return;
                }
                try {
                    String directory = URLDecoder.decode(System.getProperty("user.dir"));
                    File f = new File(directory + "/plugins/Denizen/schematics/" + name.asString() + ".schematic");
                    if (!f.exists()) {
                        dB.echoError("Schematic file " + name.asString() + " does not exist.");
                        return;
                    }
                    InputStream fs = new FileInputStream(f);
                    set = CuboidBlockSet.fromCompressedString(ScriptHelper.convertStreamToString(fs));
                    fs.close();
                    schematics.put(name.asString().toUpperCase(), set);
                }
                catch (Exception ex) {
                    dB.echoError("Error loading schematic file " + name.asString() + ".");
                    dB.echoError(ex);
                    return;
                }
                break;
            case UNLOAD:
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    dB.echoError("Schematic file " + name.asString() + " is not loaded.");
                    return;
                }
                schematics.remove(name.asString().toUpperCase());
                break;
            case ROTATE:
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    dB.echoError("Schematic file " + name.asString() + " is not loaded.");
                    return;
                }
                if (angle == null) {
                    dB.echoError("Missing angle argument!");
                    return;
                }
                dB.echoError(scriptEntry.getResidingQueue(), "Schematic rotation is TODO!");
                //schematics.get(name.asString().toUpperCase()).rotate2D(angle.asInt());
                break;
            case PASTE:
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    dB.echoError("Schematic file " + name.asString() + " is not loaded.");
                    return;
                }
                if (location == null) {
                    dB.echoError("Missing location argument!");
                    return;
                }
                try {
                    schematics.get(name.asString().toUpperCase()).setBlocks(location);
                }
                catch (Exception ex) {
                    dB.echoError("Exception pasting schematic file " + name.asString() + ".");
                    dB.echoError(ex);
                    return;
                }
                break;
            case SAVE:
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    dB.echoError("Schematic file " + name.asString() + " is not loaded.");
                    return;
                }
                try {
                    set = schematics.get(name.asString().toUpperCase());
                    String directory = URLDecoder.decode(System.getProperty("user.dir"));
                    File f = new File(directory + "/plugins/Denizen/schematics/" + name.asString() + ".schematic");
                    String output = set.toCompressedFormat();
                    FileOutputStream fs = new FileOutputStream(f);
                    OutputStreamWriter osw = new OutputStreamWriter(fs);
                    osw.write(output);
                    osw.flush();
                    osw.close();
                    fs.flush();
                    fs.close();
                }
                catch (Exception ex) {
                    dB.echoError("Error saving schematic file " + name.asString() + ".");
                    dB.echoError(ex);
                    return;
                }
                break;
        }
    }
    @TagManager.TagEvents
    public void schematicTags(ReplaceableTagEvent event) {

        if (!event.matches("schematic, schem")) return;

        if (!event.hasNameContext()) {
            return;
        }

        String id = event.getNameContext().toUpperCase();

        Attribute attribute = new Attribute(event.raw_tag, event.getScriptEntry()).fulfill(1);

        if (!schematics.containsKey(id)) {
            // Meta below
            if (attribute.startsWith("exists")) {
                event.setReplaced(new Element(false)
                        .getAttribute(attribute.fulfill(1)));
                return;
            }

            dB.echoError("Schematic file " + id + " is not loaded.");
            return;
        }

        CuboidBlockSet set = schematics.get(id);

        //
        // Check attributes
        //

        // <--[tag]
        // @attribute <schematic[<name>].exists>
        // @returns Element(Boolean)
        // @description
        // Returns whether the schematic exists.
        // @plugin WorldEdit
        // -->
        if (attribute.startsWith("exists")) {
            event.setReplaced(new Element(true)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].height>
        // @returns Element(Number)
        // @description
        // Returns the height (Y) of the schematic.
        // @plugin WorldEdit
        // -->
        if (attribute.startsWith("height")) {
            event.setReplaced(new Element(set.y_length)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].length>
        // @returns Element(Number)
        // @description
        // Returns the length (Z) of the schematic.
        // @plugin WorldEdit
        // -->
        if (attribute.startsWith("length")) {
            event.setReplaced(new Element(set.z_height)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].width>
        // @returns Element(Number)
        // @description
        // Returns the width (X) of the schematic.
        // @plugin WorldEdit
        // -->
        if (attribute.startsWith("width")) {
            event.setReplaced(new Element(set.x_width)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].block[<location>]>
        // @returns dMaterial
        // @description
        // Returns the material for the block at the location in the schematic.
        // @plugin WorldEdit
        // -->
        if (attribute.startsWith("block")) {
            if (attribute.hasContext(1) && dLocation.matches(attribute.getContext(1))) {
                dLocation location = dLocation.valueOf(attribute.getContext(1));
                BlockData block = set.blockAt(location.getX(), location.getY(), location.getZ()));
                event.setReplaced(dMaterial.getMaterialFrom(block.material, block.data)
                        .getAttribute(attribute.fulfill(1)));
                return;
            }
        }

        // <--[tag]
        // @attribute <schematic[<name>].origin>
        // @returns dLocation
        // @description
        // Returns the origin location of the schematic.
        // @plugin WorldEdit
        // -->
        if (attribute.startsWith("origin")) {
            event.setReplaced(new dLocation(null, set.center_x, set.center_y, set.center_z)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].blocks>
        // @returns Element(Number)
        // @description
        // Returns the number of blocks in the schematic.
        // @plugin WorldEdit
        // -->
        if (attribute.startsWith("blocks")) {
            event.setReplaced(new Element(set.blocks.size())
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].cuboid[<origin location>]>
        // @returns dCuboid
        // @description
        // Returns a cuboid of where the schematic would be if it was pasted at an origin.
        // @plugin WorldEdit
        // -->
        if (attribute.startsWith("cuboid") && attribute.hasContext(1)) {
            dLocation origin = dLocation.valueOf(attribute.getContext(1));
            event.setReplaced(set.getCuboid(origin)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }
    }
}
