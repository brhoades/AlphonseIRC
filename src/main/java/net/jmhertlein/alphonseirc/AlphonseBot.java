/*
 * Copyright (C) 2013 Joshua Michael Hertlein <jmhertlein@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.jmhertlein.alphonseirc;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

/**
 *
 * @author Joshua Michael Hertlein <jmhertlein@gmail.com>
 */
public class AlphonseBot extends PircBot {
    private List<String> ownerNicks;
    private String nickPassword, nick;
    private Map<String, ConversationState> conversationStates;
    

    public AlphonseBot(List<String> ownerNicks, String nick, String nickPassword) {
        this.ownerNicks = ownerNicks;
        this.nickPassword = nickPassword;
        this.nick = nick;
        conversationStates = new HashMap<>();
        setName(nick);
    }
    
    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        if(sender.equals(nick))
            return;
        if(ownerNicks.contains(sender)) {
            sendMessage(channel, "Welcome back, " + sender + "!");
            return;
        }
        System.out.println(sender + " joined.");
        printInitialGreeting(channel, sender);
        conversationStates.put(sender, ConversationState.WAITING_FOR_MAIN_MENU_SELECTION);
    }

    @Override
    protected void onConnect() {
        this.identify(nickPassword);
        System.out.println("Messaged NickServ");
    }

    private static enum ConversationState {
        WAITING_FOR_MAIN_MENU_SELECTION,
        IDLE;
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        //System.out.printf("Channel: %s, Sender: %s, Message: %s\n", channel, sender, message);
        ConversationState state = conversationStates.get(sender);
        //System.out.println("State:" + (state == null ? "null" : state.name()));
        if(message.contains(nick)) {
            printInitialGreeting(channel, sender);
            conversationStates.put(sender, ConversationState.WAITING_FOR_MAIN_MENU_SELECTION);
        }
        if(state == null)
            return;
        
        switch(state) {
            case WAITING_FOR_MAIN_MENU_SELECTION:
                onMainMenuSelectionMade(sender, channel, message);
                break;
            case IDLE:
            default:
                break;
        }
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        System.out.printf("[PM][%s (Login: %s)]: %s\n", sender, login, message);
    }
    
    
    
    private void printInitialGreeting(String channel, String targetNick) {
        sendMessage(channel, "Welcome to the MCTowns IRC channel, " + targetNick + ". I'm Everdras' trusty IRC bot. How can I help you?");
        sendMessage(channel, "Just say the number of the choice you wish to select from this menu below:");
        sendMessage(channel, "1) I have a question.");
        sendMessage(channel, "2) I found a bug, and want to report it.");
        sendMessage(channel, "3) I'm just here to hang out.");
    }
    
    private void onMainMenuSelectionMade(String sender, String channel, String message) {
        if(message.contains("1")) {
            sendMessage(channel, "If you have a question, let me see what I can do...");
            if(isEverdrasInChannel(channel))
                sendMessage(channel, "Pinging Everdras, and Everdras_...");
            else
                sendMessage(channel, "Everdras isn't in the channel, so I'll skip pinging him.");
            
            sendMessage(channel, "Sending Everdras a PM...");
            
            sendMessage("Everdras", String.format("%s has a question for you in %s. Pay attention.", sender, channel));
            sendMessage("Everdras_", String.format("%s has a question for you in %s. Pay attention.", sender, channel));
            
            sendMessage(channel, "If Everdras is at his computer, he should respond shortly. If he doesn't reply, you should try these things:");
            sendMessage(channel, "1) Check the MCTowns wiki:  https://github.com/jmhertlein/MCTowns/wiki");
            sendMessage(channel, "2) Send Everdras a PM on BukkitDev: http://dev.bukkit.org/home/send-private-message/?to=Everdras");
            sendMessage(channel, "3) Email Everdras at everdras@gmail.com");
            sendMessage(channel, "I urge you to check out the wiki, it's useful. I hope I was able to help you!");
            
            conversationStates.put(sender, ConversationState.IDLE);
        }
        else if(message.contains("2")) {
            sendMessage(channel, "Oh, you found a bug! I'm sorry about that. Everdras would love to chat with you about it,"
                    + "but the first thing you should do is head over to https://github.com/jmhertlein/MCTowns/issues and open a bug report.\n"
                    + "This is really important, because it makes sure your issue gets noticed and keeps all the discussion on it in one place.");
            sendMessage(channel, "I've just pinged Everdras, so if he's available he should be responding soon. I hope I was able to be of some help!");
            sendMessage("Everdras", sender + " found a bug.");
            sendMessage("Everdras_", sender + " found a bug.");
            conversationStates.put(sender, ConversationState.IDLE);
        } else if(message.contains("3")) {
            sendMessage(channel, "Ah, you're just here to hang out. My apologies, enjoy!");
            conversationStates.put(sender, ConversationState.IDLE);
        }
        else {
            sendMessage(channel, "I'm sorry, I don't understand your message: \"" + message + "\". Please enter a valid number: 1, 2, or 3.");
        }
    }
    
    private boolean isEverdrasInChannel(String channel) {
        for(User name : getUsers(channel))
            if(name.getNick().equals("Everdras") || name.getNick().equals("Everdras_"))
                return true;
        return false;
    }

    @Override
    protected void onDisconnect() {
        System.err.println("I got disconnected!");
        System.out.println("I got disconnected!");
        try {
            System.out.println("Trying to reconnect...");
            connect("irc.esper.net");
            System.out.println("Reconnected.");
        } catch (IOException | IrcException ex) {
            System.err.println("Error reconnecting to irc.esper.net.");
        }
        
        System.out.println("Rejoining MCTowns channel.");
        joinChannel("#mctowns");
        System.out.println("Rejoined #mctowns");
    }
    
    
}
