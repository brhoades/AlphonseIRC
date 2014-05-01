/*
 * Copyright (C) 2013-14 Joshua Michael Hertlein <jmhertlein@gmail.com>
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

    public AlphonseBot(List<String> ownerNicks, String nick, String nickPassword) {
        this.ownerNicks = ownerNicks;
        this.nickPassword = nickPassword;
        this.nick = nick;
        setName(nick);
    }

    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        if (sender.equals(nick)) {
            return;
        }

        voice(channel, sender);
	System.out.println("Voiced " + sender);
        System.out.println(sender + " joined.");
    }

    @Override
    protected void onConnect() {
        this.identify(nickPassword);
        System.out.println("Messaged NickServ");
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        message = message.toLowerCase();
        if(message.contains("voldemort")) {
            sendMessage(channel, "DON'T SAY HIS NAME!");
        } else if(message.matches("(^|.*\\W+)rms(\\W+.*|$)")) {
            sendMessage(channel, "May He live forever.");
        } else if(message.matches("(^|.*\\W+)(java|jvm)(\\W+.*|$)")) {
            sendMessage(channel, "brodes: JAVA DETECTED");
        }

        if(message.contains(nick.toLowerCase()) && message.matches(".*\\W+hi(\\W+.*|$)")) {
            sendMessage(channel, String.format("%s: %s", sender, "What you're referring to as \"Hi\" is in fact GNU/Hi, or as I've recently taken to calling it, GNU+Hi."));
        }
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        System.out.printf("[PM][%s (Login: %s)]: %s\n", sender, login, message);
    }

    @Override
    protected void onPart(String channel, String sender, String login, String hostname) {
        System.out.println(sender + " parted.");
    }

    @Override
    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if(recipientNick.equals(nick)) {
            joinChannel("#mstdeskeng");
        }
    }

    @Override
    protected void onDisconnect() {
        System.err.println("I got disconnected!");
        System.out.println("I got disconnected!");

        int attempt = 0;
        while (!isConnected()) {
            try {
                System.out.printf("Trying to reconnect... (Attempt %s)\n", attempt);
                connect("irc.esper.net");
                System.out.println("Reconnected.");
            } catch (IOException | IrcException ex) {
                System.err.println("Error reconnecting to irc.esper.net.");
            }
        }
        
        System.out.println("Rejoining channel...");
        joinChannel("#mstdeskeng");

        System.out.println("Rejoined #mstdeskeng");
    }

}
