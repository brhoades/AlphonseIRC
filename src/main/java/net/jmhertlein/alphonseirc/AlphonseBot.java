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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;

/**
 *
 * @author Joshua Michael Hertlein <jmhertlein@gmail.com>
 */
public class AlphonseBot extends PircBot {

    private static final String INTERJECTION = "I'd just like to interject for moment. What you're refering to as $WORD, is in fact, GNU/$WORD, or as I've recently taken to calling it, GNU plus $WORD. $WORD is not a word unto itself, but rather another free component of a fully functioning GNU word made useful by the GNU letters, vowels and vital verbiage components comprising a full word as defined by POSIX. ";
    private Random gen;
    private final String nick, pass, server;
    private final List<String> channels;
    private final LinkedList<String> previousSenders;
    private int maxXKCD;
    private boolean voicing;
    private TTTGame ttt;
    private final Map<LocalDate, LocalTime> dadLeaveTimes;

    private final Set<String> noVoiceNicks, masters;

    public AlphonseBot(String nick, String pass, String server, List<String> channels, int maxXKCD, Set<String> noVoiceNicks, Set<String> masters, Map<LocalDate, LocalTime> dadLeaveTimes) {
        this.pass = pass;
        this.nick = nick;
        this.channels = channels;
        this.server = server;
        setName(nick);

        this.maxXKCD = maxXKCD;
        this.noVoiceNicks = noVoiceNicks;
        this.masters = masters;
        voicing = true;
        
        this.dadLeaveTimes = dadLeaveTimes;

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
        if (sender.equals(nick)) {
            return;
        }

        if (voicing && !noVoiceNicks.contains(sender)) {
            voice(channel, sender);
        }

        System.out.println("Voiced " + sender);
        System.out.println(sender + " joined.");
    }

    @Override
    protected void onConnect() {
        if (!pass.isEmpty()) {
            this.identify(pass);
            System.out.println("Messaged NickServ");
        } else {
            System.out.println("Empty pass, skipping nickserv identification.");
        }
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (previousSenders.size() > 200) {
            previousSenders.removeFirst();
        }
        previousSenders.add(sender);

        if (message.equalsIgnoreCase(String.format("%s: help", nick))) {
            sendMessage(sender, "Master, I am capable of many things. I shall list them:");
            sendMessage(sender, "Lord Munroe is truly witty- I can fetch you one of his drole comics. (!xkcd)");
            sendMessage(sender, "I am well-versed in the art of converting phrases to numbers. (!hash)");
            sendMessage(sender, "Lord Stallman's logic is undeniable. I can correct the verbiage of the unenlightened with His GNU+Wisdom. (!interject)");
            sendMessage(sender, "I am also well-phrased in the art of turning phrases into letters. (!encode)");
            sendMessage(sender, "I can also turn them back into phrases. (!decode)");
            sendMessage(sender, "And sir, should you wish me to keep your orders private, simply /msg me them and I will report them to you confidentially.");
        }

        if (message.startsWith("!")) {
            onCommand(channel, sender, message.substring(1).split(" "));
        }
    }

    private void sendXKCD(String channel, String sender) {
        if (MSTDeskEngRunner.checkXKCDUpdate()) {
            this.maxXKCD = MSTDeskEngRunner.getMaxXKCD();
            MSTDeskEngRunner.writeConfig();
        }

        sendMessage(channel, sender + ": Your XKCD is ready, sir: https://xkcd.com/" + (1 + gen.nextInt(maxXKCD)));
    }

    private void interject(String target, String[] args) {
        String name, word;
        if (args.length != 3) {
            sendMessage(target, "Incorrect number of args. Usage: !interject <GNU+target> <GNU+word>");
            return;
        }
        name = args[1];
        word = args[2];

        sendMessage(target, name + ": " + INTERJECTION.replace("$WORD", word));
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        System.out.printf("[PM][%s (Login: %s)]: %s\n", sender, login, message);

        if (message.startsWith("!")) {
            onCommand(sender, sender, message.substring(1).split(" "));
        }
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
        boolean error = false;
        while (!isConnected() || error) {
            try {
                System.out.printf("Trying to reconnect... (Attempt %s)\n", attempt);
                connect(server);
                System.out.println("Reconnected.");
                error = false;
            } catch (IOException | IrcException ex) {
                System.err.println("Error reconnecting to " + server);
                error = true;
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

    private void onCommand(String target, String sender, String[] args) {
        String cmd = args[0];
        String rest = "";
        for (int i = 1; i < args.length; i++) {
            rest += args[i] + " ";
        }
        rest = rest.trim();

        switch (cmd) {
            case "voice":
                if (masters.contains(sender)) {
                    handleVoice(target, args, true);
                }
                break;
            case "novoice":
                if (masters.contains(sender)) {
                    handleVoice(target, args, false);
                }
                break;
            case "stopvoice":
                if (masters.contains(sender)) {
                    voicing = false;
                    sendMessage(target, "Stopping voicing.");
                }
                break;
            case "startvoice":
                if (masters.contains(sender)) {
                    voicing = true;
                    sendMessage(target, "Resuming voicing.");
                }
                break;
            case "xkcd":
                sendXKCD(target, sender);
                break;
            case "interject":
                interject(target, args);
                break;
            case "hash":
                handleHashCommand(target, sender, args, cmd, rest);
                break;
            case "encode":
                sendMessage(target, sender + ": " + Base64.encodeBase64String(rest.getBytes()));
                break;
            case "decode":
                sendMessage(target, sender + ": " + new String(Base64.decodeBase64(rest)));
                break;
            case "permute":
                handlePermuteCommand(sender, target, args);
                break;
            case "billy":
                handleBillyCommand(target);
                break;
            case "tt":
            case "ttt":
                handleTTTCommand(target, sender, args);
                break;
            case "dad":
                handleDadCommand(target, sender, args);
                break;
        }
    }

    private void handleBillyCommand(String target) {
        sendMessage(target, "Measuring Billium levels...");
        int total = previousSenders.size(), billy = 0;
        for (String previousSender : previousSenders) {
            if (previousSender.equals("brodes")) {
                billy++;
            }
        }
        if (total > 0) {
            BigDecimal conc = new BigDecimal(((float) billy) / total * 100);
            sendMessage(target, "Current Billium concentration: " + conc.toPlainString() + "%");
            String status;
            if (conc.compareTo(new BigDecimal(80)) > 0) {
                status = "!!DANGER!! OVERDOSE IMMENENT";
            } else if (conc.compareTo(new BigDecimal(50)) > 0) {
                status = "WARNING - DANGEROUS LEVELS";
            } else if (conc.compareTo(new BigDecimal(30)) > 0) {
                status = "Caution - Levels rising, but stable";
            } else {
                status = "Billium levels negligible.";
            }
            sendMessage(target, "Current status: " + status);
        } else {
            sendMessage(target, "Billium levels negligible.");
        }
    }

    private void handlePermuteCommand(String sender, String target, String[] args) {
        if (!sender.equals("Everdras")) {
            sendMessage(target, sender + " pls go");
            return;
        }
        if (args.length != 2) {
            sendMessage(target, "Incorrect number of args. Usage: !permute <string>");
            return;
        }

        String word = args[1];
        if (word.length() > 5) {
            sendMessage(target, "Too long, max length: 5");
            return;
        }
        sendMessage(target, "Permuting...");
        List<String> perms = permute(word);
        sendMessage(target, "Permutations: " + perms.size());
        long origDelay = getMessageDelay();
        this.setMessageDelay(10);
        List<String> output = new LinkedList<>();
        StringBuilder b = new StringBuilder();
        int c = 0;
        for (String s : perms) {
            b.append(s);
            b.append(' ');
            c++;
            if (c == 8) {
                c = 0;
                output.add(b.toString());
                b = new StringBuilder();
            }
        }
        if (b.length() != 0) {
            output.add(b.toString());
        }
        for (String s : output) {
            sendMessage(target, s);
        }
        this.setMessageDelay(origDelay);
    }

    public static List<String> permute(String s) {
        List<String> ret = new LinkedList<>();
        if (s.isEmpty()) {
            ret.add("");
            return ret;
        }

        for (char c : s.toCharArray()) {
            for (String subPermute : permute(s.replaceFirst("[" + c + "]", ""))) {
                ret.add(c + subPermute);
            }
        }
        return ret;
    }

    private void handleHashCommand(String target, String sender, String[] args, String cmd, String rest) {
        sendMessage(target, "hashcode() of \"" + rest + "\": " + rest.hashCode());
    }

    private void handleVoice(String target, String[] args, boolean voiced) {
        if (args.length != 2) {
            sendMessage(target, "Incorrect number of args. Usage: !novoice <nick>");
            return;
        }
        String targetNick = args[1];

        if (voiced) {
            noVoiceNicks.remove(targetNick);
            voice(target, targetNick);
            sendMessage(target, "Voiced " + targetNick);
        } else {
            noVoiceNicks.add(targetNick);
            deVoice(target, targetNick);
            sendMessage(target, "Devoiced " + targetNick);
        }

        MSTDeskEngRunner.writeConfig();
    }

    private void handleTTTCommand(String target, String sender, String[] args) {
        String cmd;
        if (args.length > 1) {
            cmd = args[1];
        } else {
            sendMessage(target, "Incorrect number of args. Usage: !ttt [start|kill|mv]");
            return;
        }

        switch (cmd) {
            case "start":
                if (args.length != 3) {
                    sendMessage(target, "Incorrect number of args. Usage: !ttt start [otherPlayer]");
                    break;
                }

                if (ttt != null) {
                    sendMessage(target, "Game already in progress.");
                    break;
                }
                String x,
                 o;
                if (gen.nextBoolean()) {
                    x = sender;
                    o = args[2];
                } else {
                    x = args[2];
                    o = sender;
                }

                ttt = new TTTGame(x, o);
                sendMessage(target, "X player is: " + x);
                sendMessage(target, "O player is: " + o);
                sendMessage(target, "X goes first.");
                long original = getMessageDelay();
                setMessageDelay(10);
                for (String s : ttt.printBoard()) {
                    sendMessage(target, s);
                }
                setMessageDelay(original);
                break;
            case "kill":
                if (ttt == null) {
                    sendMessage(target, "No game to kill.");
                    break;
                }
                if (masters.contains(sender) || ttt.isParticipating(nick)) {
                    ttt = null;
                    sendMessage(target, "Game killed.");
                } else {
                    sendMessage(target, "Insufficient permission to kill game.");
                    break;
                }
                break;
            case "move":
            case "mv":
                int[] dest = handleDestProcessing(target, args);
                
                if(dest == null)
                    return;

                int i = dest[0],
                 j = dest[1];

                if (!ttt.isValidMove(i, j)) {
                    sendMessage(target, "Invalid move.");
                    break;
                }

                if (ttt.moveForPlayer(sender, i, j)) {
                    sendMessage(target, "Moved.");
                    long orig = getMessageDelay();
                    this.setMessageDelay(10);
                    for (String s : ttt.printBoard()) {
                        sendMessage(target, s);
                    }
                    setMessageDelay(orig);
                    ttt.flipTurn();
                } else {
                    sendMessage(target, sender + ": You're either not in this game, or it's not your turn.");
                    break;
                }

                if (ttt.isWon()) {
                    sendMessage(target, ttt.getWinner() + " has won.");
                    ttt = null;
                }
                break;
            case "print":
                if (ttt == null) {
                    sendMessage(target, "No game in session.");
                    break;
                } else {
                    for (String s : ttt.printBoard()) {
                        sendMessage(target, s);
                    }
                    sendMessage(target, "It is " + (ttt.isXTurn() ? "X" : "O") + "'s turn.");
                }
                break;
            default:
                sendMessage(target, "Unknown sub-command of !ttt. Usage: !ttt start [otherPlayer]");
                break;
        }

    }

    private int[] handleDestProcessing(String target, String[] args) {
        if (args.length == 4) {
            String xPos = args[2], yPos = args[3];
            int i, j;
            try {
                i = Integer.parseInt(xPos);
                if (i < 0 || i > 2) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException nfe) {
                sendMessage(target, String.format("Invalid integer \"%s\", should be int in range [0,2] inclusive.", xPos));
                return null;
            }
            try {
                j = Integer.parseInt(yPos);
                if (j < 0 || j > 2) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException nfe) {
                sendMessage(target, String.format("Invalid integer \"%s\", should be int in range [0,2] inclusive.", yPos));
                return null;
            }
            
            return new int[]{i, j};
        } else if (args.length == 3) {
            if(args[2].length() != 1) {
                sendMessage(target, "Label should be a single character between A and I, inclusive.");
                return null;
            }
            
            char input = args[2].toUpperCase().charAt(0);
            if(input < 'A' || input > 'I') {
                sendMessage(target, "Label should be a single character between A and I, inclusive.");
                return null;
            }
            
            return TTTGame.decode(input);
        } else {
            sendMessage(target, "Incorrect number of args. Usage: !ttt mv (<x> <y>|<label>)  where (0,0) is the top-left");
            return null;
        }
    }

    private void handleDadCommand(final String target, String sender, String[] args) {
        if(args.length == 1) {
            sendMessage(target, "Usage: !dad [left|list|say]");
            return;
        }
        
        switch(args[1]) {
            case "left":
                ZonedDateTime now = ZonedDateTime.now();
                this.dadLeaveTimes.put(LocalDate.now(), LocalTime.now());
                sendMessage(target, "Marked dad's leave time as now (" + now.format(DateTimeFormatter.ISO_LOCAL_TIME) + ").");
                break;
            case "list":
                int num = 3;
                if(args.length == 3) {
                    try {
                        num = Integer.parseInt(args[2]);
                        if(num > 10)
                            num = 10;
                    } catch(NumberFormatException nfe) {
                        sendMessage(target, "Error parsing " + args[2] + " into int: " + nfe.getMessage());
                        sendMessage(target, "Usage: !dad list (optional: number, default 3, max 10)");
                        return;
                    }
                }
                
                final int prevDays = num;
                dadLeaveTimes.keySet().stream()
                        .sorted()
                        .filter(date -> date.isAfter(LocalDate.now().minusDays(prevDays)))
                        .map(date -> date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " || " + dadLeaveTimes.get(date).format(DateTimeFormatter.ISO_LOCAL_TIME))
                        .forEach(leaveTime -> sendMessage(target, leaveTime));
                    
                        
                break;
            case "say":
                String msg;
                switch(gen.nextInt(5)) {
                    case 0: 
                        msg = "TYPES LOUDLY";
                        break;
                    case 1:
                        msg = "BREATHES DEEPLY";
                        break;
                    case 2:
                        msg = "ANGRILY TYPES AN EMAIL";
                        break;
                    case 3:
                        msg = "BACKSPACES WITH AUTHORITY";
                        break;
                    case 4:
                        msg = "STRETCHES WHILE EXHALING";
                        break;
                    default:
                        msg = "Someone made nextInt() go too high";
                        break;
                }
                
                this.sendAction(target, msg);
                break;
            default:
                System.out.println("Bad switch on " + args[1]);
        }
        
    }
}