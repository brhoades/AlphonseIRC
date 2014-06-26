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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;

/**
 *
 * @author Joshua Michael Hertlein <jmhertlein@gmail.com>
 */
public class AlphonseBot extends PircBot {

    private static String INTERJECTION = "I'd just like to interject for moment. What you're refering to as $WORD, is in fact, GNU/$WORD, or as I've recently taken to calling it, GNU plus $WORD. $WORD is not a word unto itself, but rather another free component of a fully functioning GNU word made useful by the GNU letters, vowels and vital verbiage components comprising a full word as defined by POSIX. ";
    private Random gen;
    private String nick, pass, server;
    private List<String> channels;
    private LinkedList<String> previousSenders;
    private int maxXKCD;

    public AlphonseBot(String nick, String pass, String server, List<String> channels, int maxXKCD) {
        this.pass = pass;
        this.nick = nick;
        this.channels = channels;
        this.server = server;
        setName(nick);

        this.maxXKCD = maxXKCD;

        previousSenders = new LinkedList<>();
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
        if(previousSenders.size() > 200)
            previousSenders.removeFirst();
        previousSenders.add(sender);

        if (message.toLowerCase().startsWith(nick.toLowerCase()))
            if (message.toLowerCase().contains("xkcd"))
                sendXKCD(message, channel, sender);
            else {
                int n = gen.nextInt(100);
                if (n < 3)
                    interject(message, channel);
                else
                    sendMessage(channel, "hashcode() of \"" + message.substring(message.indexOf(":") + 2) + "\": " + message.substring(message.indexOf(":") + 2).hashCode());
            }
        else if (message.startsWith("!"))
            onCommand(channel, sender, message.substring(1).split(" "));
    }

    private void sendXKCD(String message, String channel, String sender) {
        if (MSTDeskEngRunner.checkXKCDUpdate()) {
            this.maxXKCD = MSTDeskEngRunner.getMaxXKCD();
            MSTDeskEngRunner.writeConfig();
        }

        boolean accept = false;
        for (String s : message.toLowerCase().split(" ")) {
            if (s.hashCode() == 93747762) //magic word
                accept = true;
        }

        if (message.toLowerCase().contains("please") || accept)
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

    private void onCommand(String channel, String sender, String[] args) {
        String cmd = args[0];

        switch (cmd) {
            case "billy":
                sendMessage(channel, "Measuring Billium levels...");
                int total = previousSenders.size(), billy = 0;
                for (String previousSender : previousSenders) {
                    if (previousSender.equals("brodes"))
                        billy++;
                }

                if(total > 0) {
                    BigDecimal conc = new BigDecimal(((float) billy) / total * 100);
                    sendMessage(channel, "Current Billium concentration: " + conc.toPlainString() + "%");
                    String status;
                    if(conc.compareTo(new BigDecimal(80)) > 0)
                        status = "!!DANGER!! OVERDOSE IMMENENT";
                    else if(conc.compareTo(new BigDecimal(50)) > 0)
                        status = "WARNING - DANGEROUS LEVELS";
                    else if(conc.compareTo(new BigDecimal(30)) > 0)
                        status = "Caution - Levels rising, but stable";
                    else
                        status = "Billium levels negligible.";
                    sendMessage(channel, "Current status: " + status);
                } else
                    sendMessage(channel, "Billium levels negligible.");
                break;
        }
    }
}
