package com.denizenscript.denizen.scripts.triggers.core;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.scripts.containers.core.InteractScriptContainer;
import com.denizenscript.denizen.npc.traits.TriggerTrait;
import com.denizenscript.denizen.objects.NPCTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.scripts.triggers.AbstractTrigger;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;

public class ProximityTrigger extends AbstractTrigger implements Listener {

    // <--[language]
    // @name Proximity Triggers
    // @group NPC Interact Scripts
    // @description
    // Proximity Triggers are triggered when when a player moves in the area around the NPC.
    //
    // Proximity triggers must have a sub-key identifying what type of proximity trigger to use.
    // The three types are "entry", "exit", and "move".
    //
    // Entry and exit do exactly as the names imply: Entry fires when the player walks into range of the NPC, and exit fires when the player walks out of range.
    //
    // Move is a bit more subtle: it fires very rapidly so long as a player remains within range of the NPC.
    // This is useful for eg script logic that needs to be constantly updating whenever a player is nearby (eg a combat NPC script needs to constantly update its aim).
    //
    // The radius that the proximity trigger detects at is set by <@link command trigger>.
    //
    // -->

    //
    // Default to 75, but dynamically set by checkMaxProximities().
    // If a Player is further than this distance from an NPC, less
    // logic is run in checking.
    //
    private static int maxProximityDistance = 75; // TODO: is this reasonable to have?

    // <--[action]
    // @Actions
    // enter proximity
    //
    // @Triggers when a player enters the NPC's proximity trigger's radius.
    //
    // @Context
    // None
    //
    // -->
    // <--[action]
    // @Actions
    // exit proximity
    //
    // @Triggers when a player exits the NPC's proximity trigger's radius.
    //
    // @Context
    // None
    //
    // -->
    // <--[action]
    // @Actions
    // move proximity
    //
    // @Triggers when a player moves inside the NPC's proximity trigger's radius.
    //
    // @Context
    // None
    //
    // -->
    // Technically defined in TriggerTrait, but placing here instead.
    // <--[action]
    // @Actions
    // proximity
    //
    // @Triggers when a player moves inside the NPC's proximity trigger's radius.
    //
    // @Context
    // None
    //
    // -->
    int taskID = -1;

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, Denizen.getInstance());
        final ProximityTrigger trigger = this;
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Denizen.getInstance(), () -> {
            Collection<? extends Player> allPlayers = Bukkit.getOnlinePlayers();
            for (NPC citizensNPC : CitizensAPI.getNPCRegistry()) {
                if (citizensNPC == null || !citizensNPC.isSpawned()) {
                    continue;
                }
                if (!citizensNPC.hasTrait(TriggerTrait.class) || !citizensNPC.getOrAddTrait(TriggerTrait.class).isEnabled(name)) {
                    continue;
                }
                NPCTag npc = new NPCTag(citizensNPC);
                TriggerTrait triggerTrait = npc.getTriggerTrait();
                for (Player bukkitPlayer : allPlayers) {
                    if (!npc.getWorld().equals(bukkitPlayer.getWorld()) && hasExitedProximityOf(bukkitPlayer, npc)) {
                        continue;
                    }
                    if (!isCloseEnough(bukkitPlayer, npc) && hasExitedProximityOf(bukkitPlayer, npc)) {
                        continue;
                    }
                    PlayerTag player = PlayerTag.mirrorBukkitPlayer(bukkitPlayer);
                    double entryRadius = triggerTrait.getRadius(name);
                    double exitRadius = triggerTrait.getRadius(name);
                    double moveRadius = triggerTrait.getRadius(name);
                    Location npcLocation = npc.getLocation();
                    boolean playerChangedWorlds = false;
                    if (npcLocation.getWorld() != player.getWorld()) {
                        playerChangedWorlds = true;
                    }
                    boolean exitedProximity = hasExitedProximityOf(bukkitPlayer, npc);
                    double distance = 0;
                    if (!playerChangedWorlds) {
                        distance = npcLocation.distance(player.getLocation());
                    }
                    if (!exitedProximity && (playerChangedWorlds || distance >= exitRadius)) {
                        if (!triggerTrait.triggerCooldownOnly(trigger, player)) {
                            continue;
                        }
                        exitProximityOf(bukkitPlayer, npc);
                        npc.action("exit proximity", player);
                        parseAll(npc, player, "EXIT");
                    }
                    else if (exitedProximity && distance <= entryRadius) {
                        if (!triggerTrait.triggerCooldownOnly(trigger, player)) {
                            continue;
                        }
                        enterProximityOf(bukkitPlayer, npc);
                        npc.action("enter proximity", player);
                        parseAll(npc, player, "ENTRY");
                    }
                    else if (!exitedProximity && distance <= moveRadius) {
                        npc.action("move proximity", player);
                        parseAll(npc, player, "MOVE");
                    }
                }
            }
        }, 5, 5);
    }

    public void parseAll(NPCTag npc, PlayerTag player, String id) {
        List<InteractScriptContainer> scripts = npc.getInteractScriptsQuietly(player, ProximityTrigger.class);
        if (scripts != null) {
            for (InteractScriptContainer container : scripts) {
                parse(npc, player, container, id);
            }
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTask(taskID);
    }

    /**
     * Checks if the Player in Proximity is close enough to be calculated.
     *
     * @param player the Player
     * @param npc    the NPC
     * @return true if within maxProximityDistance in all directions
     */
    private boolean isCloseEnough(Player player, NPCTag npc) {
        Location pLoc = player.getLocation();
        Location nLoc = npc.getLocation();
        if (Math.abs(pLoc.getX() - nLoc.getX()) > maxProximityDistance) {
            return false;
        }
        if (Math.abs(pLoc.getY() - nLoc.getY()) > maxProximityDistance) {
            return false;
        }
        if (Math.abs(pLoc.getZ() - nLoc.getZ()) > maxProximityDistance) {
            return false;
        }
        return true;
    }

    private static Map<UUID, Set<Integer>> proximityTracker = new HashMap<>();

    //
    // Ensures that a Player who has entered proximity of an NPC also fires Exit Proximity.
    //
    private boolean hasExitedProximityOf(Player player, NPCTag npc) {
        // If Player hasn't entered proximity, it's not in the Map. Return true, must be exited.
        Set<Integer> existing = proximityTracker.get(player.getUniqueId());
        if (existing == null) {
            return true;
        }
        // If Player has no entry for this NPC, return true.
        if (!existing.contains(npc.getId())) {
            return true;
        }
        // Entry is present, NPC has not yet triggered exit proximity.
        return false;
    }

    /**
     * Called when a 'Enter Proximity' has been called to make sure an exit
     * proximity will be called.
     *
     * @param player the Player
     * @param npc    the NPC
     */
    private void enterProximityOf(Player player, NPCTag npc) {
        Set<Integer> npcs = proximityTracker.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        npcs.add(npc.getId());
    }

    /**
     * Called when an 'Exit Proximity' has been called. Once successfully exited,
     * a Player can enter proximity again.
     *
     * @param player the Player
     * @param npc    the NPC
     */
    private void exitProximityOf(Player player, NPCTag npc) {
        Set<Integer> npcs = proximityTracker.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        npcs.remove(npc.getId());
    }
}
