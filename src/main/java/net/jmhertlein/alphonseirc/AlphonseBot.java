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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
    
    private final Set<String> noVoiceNicks;

    public AlphonseBot(String nick, String pass, String server, List<String> channels, int maxXKCD, Set<String> noVoiceNicks) {
        this.pass = pass;
        this.nick = nick;
        this.channels = channels;
        this.server = server;
        setName(nick);

        this.maxXKCD = maxXKCD;
        this.noVoiceNicks = noVoiceNicks;
        voicing = true;

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

        if(voicing && !noVoiceNicks.contains(sender)) {
            voice(channel, sender);
        }
        
        System.out.println("Voiced " + sender);
        System.out.println(sender + " joined.");
    }

    @Override
    protected void onConnect() {
        if(!pass.isEmpty()) {
            this.identify(pass);
            System.out.println("Messaged NickServ");
        } else
            System.out.println("Empty pass, skipping nickserv identification.");
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        if(previousSenders.size() > 200)
            previousSenders.removeFirst();
        previousSenders.add(sender);
        
        if(message.equalsIgnoreCase(String.format("%s: help", nick))) {
            sendMessage(sender, "Master, I am capable of many things. I shall list them:");
            sendMessage(sender, "Lord Munroe is truly witty- I can fetch you one of his drole comics. (!xkcd)");
            sendMessage(sender, "I am well-versed in the art of converting phrases to numbers. (!hash)");
            sendMessage(sender, "Lord Stallman's logic is undeniable. I can correct the verbiage of the unenlightened with His GNU+Wisdom. (!interject)");
            sendMessage(sender, "I am also well-phrased in the art of turning phrases into letters. (!encode)");
            sendMessage(sender, "I can also turn them back into phrases. (!decode)");
            sendMessage(sender, "And sir, should you wish me to keep your orders private, simply /msg me them and I will report them to you confidentially.");
        }

        
        if (message.startsWith("!"))
            onCommand(channel, sender, message.substring(1).split(" "));
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
        if(args.length != 3) {
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
        
        if(message.startsWith("!"))
            onCommand(sender, sender, message.substring(1).split(" "));
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

    private void onCommand(String target, String sender, String[] args) {
        String cmd = args[0];
        
        String rest = "";
        for(int i = 1; i < args.length; i++)
            rest += args[i] + " ";
        rest = rest.trim();

        switch (cmd) {
            case "voice":
                handleVoice(target, args, true);
                break;
            case "novoice":
                handleVoice(target, args, false);
                break;
            case "stopvoice":
                voicing = false;
                sendMessage(target, "Stopping voicing.");
                break;
            case "startvoice":
                voicing = true;
                sendMessage(target, "Resuming voicing.");
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
        }
    }

    private void handleBillyCommand(String target) {
        sendMessage(target, "Measuring Billium levels...");
        int total = previousSenders.size(), billy = 0;
        for (String previousSender : previousSenders) {
            if (previousSender.equals("brodes"))
                billy++;
        }
        if(total > 0) {
            BigDecimal conc = new BigDecimal(((float) billy) / total * 100);
            sendMessage(target, "Current Billium concentration: " + conc.toPlainString() + "%");
            String status;
            if(conc.compareTo(new BigDecimal(80)) > 0)
                status = "!!DANGER!! OVERDOSE IMMENENT";
            else if(conc.compareTo(new BigDecimal(50)) > 0)
                status = "WARNING - DANGEROUS LEVELS";
            else if(conc.compareTo(new BigDecimal(30)) > 0)
                status = "Caution - Levels rising, but stable";
            else
                status = "Billium levels negligible.";
            sendMessage(target, "Current status: " + status);
        } else
            sendMessage(target, "Billium levels negligible.");
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
        this.setMessageDelay(210);
        List<String> output = new LinkedList<>();
        StringBuilder b = new StringBuilder();
        int c = 0;
        for(String s : perms) {
            b.append(s);
            b.append(' ');
            c++;
            if(c == 8) {
                c = 0;
                output.add(b.toString());
                b = new StringBuilder();
            }
        }
        if(b.length() != 0)
            output.add(b.toString());
        for(String s : output) {
            sendMessage(target, s);
        }
        this.setMessageDelay(origDelay);
    }
    
  public static List<String> permute(String s) {
    List<String> ret = new LinkedList<>();
    if(s.isEmpty()) {
      ret.add("");
      return ret;
    }

    for(char c : s.toCharArray()) {
      for(String subPermute : permute(s.replaceFirst("[" + c + "]", "")))
        ret.add(c + subPermute);
    }
    return ret;
  }

    private void handleHashCommand(String target, String sender, String[] args, String cmd, String rest) {
        sendMessage(target, "hashcode() of \"" + rest + "\": " + rest.hashCode());
    }

    private void handleVoice(String target, String[] args, boolean voiced) {
        if(args.length != 2) {
            sendMessage(target, "Incorrect number of args. Usage: !novoice <nick>");
            return;
        }
        String targetNick = args[1];
        
        if(voiced) {
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
}
