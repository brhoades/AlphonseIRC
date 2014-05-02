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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jmhertlein.core.io.LFSeparatedFile;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Joshua Michael Hertlein <jmhertlein@gmail.com>
 */
public class AlphonseBot extends PircBot {

    private static String INTERJECTION = "I'd just like to interject for moment. What you're refering to as $WORD, is in fact, GNU/$WORD, or as I've recently taken to calling it, GNU plus $WORD. $WORD is not a word unto itself, but rather another free component of a fully functioning GNU word made useful by the GNU letters, vowels and vital verbiage components comprising a full word as defined by POSIX. ";
    private Random gen;
    private String nick, pass, server;
    private List<String> channels;
    private int maxXKCD;

    public AlphonseBot(String nick, String pass, String server, List<String> channels, int maxXKCD) {
        this.pass = pass;
        this.nick = nick;
        this.channels = channels;
        this.server = server;
        setName(nick);

        this.maxXKCD = maxXKCD;
    }

    public void startConnection() throws IOException, IrcException {
        System.out.println("Joining " + server);
        connect(server);
        System.out.println("Joined " + server);
        for (String chan : channels) {
            System.out.println("Joining " + chan);
            joinChannel(chan);
            System.out.println("Joined " + chan);
        }
        gen = new Random();
    }

    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        if (sender.equals(nick))
            return;

        voice(channel, sender);
        System.out.println("Voiced " + sender);
        System.out.println(sender + " joined.");
    }

    @Override
    protected void onConnect() {
        this.identify(pass);
        System.out.println("Messaged NickServ");
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (message.toLowerCase().contains(nick.toLowerCase()))
            if (message.toLowerCase().contains("xkcd"))
                sendXKCD(message, channel, sender);
            else {
                int n = gen.nextInt(100);
                if (n < 3)
                    interject(message, channel);
                else if (n < 90)
                    sendMessage(channel, "hashcode() of \"" + message + "\": " + message.hashCode());
                else
                    sendXKCD(message, channel, sender);
            }
    }

    private void sendXKCD(String message, String channel, String sender) {
        if(MSTDeskEngRunner.checkXKCDUpdate()) {
            this.maxXKCD = MSTDeskEngRunner.getMaxXKCD();
            MSTDeskEngRunner.writeConfig();
        }
        if (message.toLowerCase().contains("please"))
            sendMessage(channel, sender + ": I found an XKCD for you: https://xkcd.com/" + (1 + gen.nextInt(maxXKCD)));
        else
            sendMessage(channel, sender + ": Say \"please\"!");
    }

    private void interject(String message, String channel) {
        String[] words = message.split(" ");
        List<String> wordList = new ArrayList<>(words.length);
        for (String w : words) {
            if (!w.toLowerCase().contains(nick.toLowerCase()))
                wordList.add(w);
        }
        if (wordList.size() >= 1) {
            String word = wordList.get(gen.nextInt(wordList.size()));
            sendMessage(channel, INTERJECTION.replace("$WORD", word));
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
        if (recipientNick.equals(nick)) {
            System.out.println(kickerNick + " kicked me! Rejoining...");
            joinChannel(channel);
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
                connect(server);
                System.out.println("Reconnected.");
            } catch (IOException | IrcException ex) {
                System.err.println("Error reconnecting to " + server);
            }
        }

        System.out.println("Rejoining channels...");
        for (String chan : channels) {
            System.out.println("Rejoining " + chan);
            joinChannel(chan);
            System.out.println("Rejoined " + chan);
        }
        System.out.println("Rejoining finished.");
    }

    public void onPreQuit() {
        for (String s : channels) {
            partChannel(s, "Tried to dereference 0x00000000");
        }
    }
}
