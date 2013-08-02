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
package net.jmhertlein.alfredirc;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.IrcException;

/**
 *
 * @author Joshua Michael Hertlein <jmhertlein@gmail.com>
 */
public class MCTownsRunner {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        
        List<String> owners = new LinkedList<>();
        owners.add("Everdras");
        owners.add("Everdras_");
        
        //get nick's password
        System.out.print("Password: ");
        String password = scan.nextLine();
        
        AlfredBot bot = new AlfredBot(owners, "Alphonse", password);
        bot.setMessageDelay(500);
        try {
            System.out.println("Connecting to esper.net.");
            bot.connect("irc.esper.net");
            System.out.println("Joining MCTowns channel.");
            bot.joinChannel("#mctowns");
            System.out.println("Done.");
        } catch (IOException | IrcException ex) {
            Logger.getLogger(MCTownsRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        boolean quit = false;
        while(!quit) {
            if(scan.nextLine().isEmpty()) {
                System.out.println("Quitting.");
                bot.disconnect();
                bot.dispose();
                quit = true;
                System.out.println("Quit'd.");
            }
        }
    }
}
