package net.aufdemrand.denizen.scripts.commands.core;

import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.debugging.dB.Messages;

/**
 * <p>Announces a message to the server.</p>
 * 
 * 
 * <br><b>dScript Usage:</b><br>
 * <pre>ANNOUNCE ['message to announce']</pre>
 * 
 * <ol><tt>Arguments: [] - Required</ol></tt>
 * 
 * <ol><tt>['message to announce']</tt><br> 
 *         The message to send to the server. This will be seen by all Players.</ol>
 * 
 * 
 * <br><b>Example Usage:</b><br>
 * <ol><tt>
 *  - ANNOUNCE 'Today is Christmas!' <br>
 *  - ANNOUNCE "<PLAYER.NAME> has completed '<FLAG.P:currentQuest>'!" <br>
 *  - ANNOUNCE "<GOLD>$$$ <WHITE>- Make some quick cash at our <RED>MINEA-SINO<WHITE>!" 
 * </ol></tt>

 * @author Jeremy Schroeder
 * 
 */
public class AnnounceCommand extends AbstractCommand {

	// the 'text' to announce
	String text;

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

    	// Reset fields
    	text = null;
    	
    	// Users tend to forget quotes sometimes on commands like this, so
    	// let's check if there are more argument than usual.
        if (scriptEntry.getArguments().size() > 3) 
            throw new InvalidArgumentsException(Messages.ERROR_LOTS_OF_ARGUMENTS);

        // Should only be one argument, since PlAYER: and NPCID: are handled
        // internally. Let's get that argument and set it as the text.
        for (String arg : scriptEntry.getArguments()) {
                text = aH.getStringFrom(arg);
                dB.echoDebug(Messages.DEBUG_SET_TEXT, aH.getStringFrom(arg));
            }

        // If text is missing, alert the console.
        if (text == null) throw new InvalidArgumentsException(Messages.ERROR_NO_TEXT);
    }

    @Override
    public void execute(String commandName) throws CommandExecutionException {
        
    	// Use Bukkit to broadcast the message to everybody in the server.
    	denizen.getServer().broadcastMessage(text);
    }

}
