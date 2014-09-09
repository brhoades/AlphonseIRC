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

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jmhertlein.core.io.Files;
import org.jibble.pircbot.IrcException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Joshua Michael Hertlein <jmhertlein@gmail.com>
 */
public class MSTDeskEngRunner {
    private static final String XKCD_URL = "https://xkcd.com/info.0.json";
    private static final File CONFIG_FILE = Files.join(System.getProperty("user.home"), ".config", "alphonseirc", "config.yml");
    private static String nick, pass, server;
    private static List<String> channels;
    private static Set<String> noVoiceNicks, masters;
    private static LinkedList<DadLeaveReport> dadLeaveTimes;
    private static int maxXKCD;
    private static long cachedUTC;

    public static int getMaxXKCD() {
        return maxXKCD;
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

        loadConfig();

        AlphonseBot bot = new AlphonseBot(nick, pass, server, channels, maxXKCD, noVoiceNicks, masters, dadLeaveTimes);
        bot.setMessageDelay(500);
        try {
            bot.startConnection();
        } catch (IOException | IrcException ex) {
            Logger.getLogger(MSTDeskEngRunner.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean quit = false;
        while (!quit) {
            String curLine = scan.nextLine();

            switch (curLine) {
                case "exit":
                    System.out.println("Quitting.");
                    bot.onPreQuit();
                    bot.disconnect();
                    bot.dispose();
                    writeConfig();
                    quit = true;
                    System.out.println("Quit'd.");
                    break;
                case "msg":
                    Scanner lineScan = new Scanner(scan.nextLine());
                    try {
                        bot.sendMessage(lineScan.next(), lineScan.nextLine());
                    } catch (Exception e) {
                        System.err.println(e.getClass().toString());
                        System.err.println("Not enough args");
                    }
                    break;
                default:
                    System.out.println("Invalid command.");
            }
        }
    }

    private static void loadConfig() {
        boolean read = false;
        File f = CONFIG_FILE;
        if (!f.exists()) {
            read = true;
            try {
                f.getParentFile().mkdirs();
                f.createNewFile();
                java.nio.file.Files.setPosixFilePermissions(Paths.get(f.toURI()), PosixFilePermissions.fromString("rw-------"));
            } catch (IOException ex) {
                Logger.getLogger(MSTDeskEngRunner.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Error writing empty config.yml!");
            }
        }

        Map<String, Object> config;

        if (read) {
            Console console = System.console();
            console.printf("Nick: \n->");
            nick = console.readLine();
            console.printf("\nPassword: \n-|");
            pass = new String(console.readPassword());
            console.printf("\nServer: \n->");
            server = console.readLine();
            console.printf("\nChannels: (ex: #java,#linux,#gnome)\n->");
            channels = Arrays.asList(console.readLine().split(","));
            System.out.println("Fetching max XKCD...");
            maxXKCD = fetchMaxXKCD();
            System.out.println("Fetched.");
            cachedUTC = System.currentTimeMillis();
            
            dadLeaveTimes = new LinkedList<>();
            noVoiceNicks = new HashSet<>();

            writeConfig();
            System.out.println("Wrote config to file: " + CONFIG_FILE.getAbsolutePath());

        } else {
            try (FileInputStream fis = new FileInputStream(f)) {
                Yaml y = new Yaml();
                config = y.loadAs(fis, Map.class);
            } catch (IOException ex) {
                Logger.getLogger(MSTDeskEngRunner.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Error parsing config!");
                return;
            }

            
            nick = (String) config.get("nick");
            pass = (String) config.get("password");
            server = (String) config.get("server");
            channels = (List<String>) config.get("channels");
            maxXKCD = (Integer) config.get("cachedMaxXKCD");
            cachedUTC = (Long) config.get("cachedUTC");
            noVoiceNicks = (Set<String>) config.get("noVoiceNicks");
            masters =  (Set<String>) config.get("masters");
            if(masters == null) {
                masters = new HashSet<>();
                masters.add("Everdras");
            }
            
            if(noVoiceNicks == null)
                noVoiceNicks = new HashSet<>();
            
            noVoiceNicks.stream().forEach((s) -> System.out.println("Loaded novoice nick: " + s));
            masters.stream().forEach((s) -> System.out.println("Loaded master nick: " + s));
            
            if(checkXKCDUpdate())
                writeConfig();
            else
                System.out.println("Loaded cached XKCD.");
            
            List<Map<String, Object>> serialDadLeaveTimes = (List<Map<String, Object>>) config.get("dadLeaveTimes");
            
            dadLeaveTimes = new LinkedList<>();
            serialDadLeaveTimes.stream().forEach((time) -> {
                dadLeaveTimes.add(new DadLeaveReport((String) time.get("time"), (String) time.get("reporter")));
            });

        }
    }

    public static boolean checkXKCDUpdate() {
        if((System.currentTimeMillis() - cachedUTC*1000) > (2*24*60*60*1000)) { // 2 days
            maxXKCD = fetchMaxXKCD();
            cachedUTC = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public static void writeConfig() {
        Map<String, Object> m = new HashMap<>();
        m.put("nick", nick);
        m.put("password", pass);
        m.put("server", server);
        m.put("channels", channels);
        m.put("cachedMaxXKCD", maxXKCD);
        m.put("cachedUTC", cachedUTC);
        m.put("noVoiceNicks", noVoiceNicks);
        m.put("masters", masters);
        
        List<Map<String, Object>> serialDadTimes = new LinkedList<>();
        dadLeaveTimes.stream().map((r) -> {
            Map<String, Object> temp = new HashMap<>();
            temp.put("reporter", r.getReporter());
            temp.put("time", r.getTime().toString());
            return temp;
        }).forEach((temp) -> {
            serialDadTimes.add(temp);
        });
        
        m.put("dadLeaveTimes", serialDadTimes);

        Yaml yaml = new Yaml();
        String yamlOutput = yaml.dump(m);
        try(PrintWriter pw = new PrintWriter(CONFIG_FILE)){
            pw.print(yamlOutput);
        } catch (IOException ex) {
            Logger.getLogger(MSTDeskEngRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private static int fetchMaxXKCD() {
        URL xkcdURL;
        try {
            xkcdURL = new URL(XKCD_URL);
        } catch (MalformedURLException ex) {
            Logger.getLogger(AlphonseBot.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }

        JSONObject json;
        try (Scanner scan = new Scanner(xkcdURL.openStream())) {
            scan.useDelimiter("\\A");
            json = new JSONObject(scan.next());
        } catch (IOException ex) {
            Logger.getLogger(AlphonseBot.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }

        return json.getInt("num");
    }
}
