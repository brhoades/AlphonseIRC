/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jmhertlein.alphonseirc;

import java.awt.Color;
import org.jibble.pircbot.Colors;

/**
 *
 * @author joshua
 */
public class TTTGame {
    private final char[][] board;
    private final String xPlayer, oPlayer;
    private boolean xTurn;
    
    public TTTGame(String x, String o) {
        board = new char[3][3];
        board[0][0] = '_';
        board[1][0] = '_';
        board[2][0] = '_';
        board[0][1] = '_';
        board[1][1] = '_';
        board[2][1] = '_';
        board[0][2] = ' ';
        board[1][2] = ' ';
        board[2][2] = ' ';
        
        xPlayer = x;
        oPlayer = o;
        xTurn = true;
    }
    
    public void placeX(int x, int y) {
        placeChar(x, y, 'X');
    }
    
    public void placeO(int x, int y) {
        placeChar(x, y, 'O');
    }
    
    private void placeChar(int x, int y, char c) {
        board[x][y] = c;
    }

    public String getXPlayer() {
        return xPlayer;
    }

    public String getOPlayer() {
        return oPlayer;
    }
    
    public boolean isParticipating(String nick) {
        return nick.equals(xPlayer) || nick.equals(oPlayer);
    }
    
    /**
     * Tries to make a move for a player
     * @param nick
     * @param x
     * @param y
     * @return true if move was done, false if the nick wasn't a player in the game
     */
    public boolean moveForPlayer(String nick, int x, int y) {
        if(nick.equals(xPlayer) && xTurn)
            placeX(x, y);
        else if(nick.equals(oPlayer) && !xTurn)
            placeO(x, y);
        else
            return false;
        
        return true;
    }
    
    public void flipTurn() {
        xTurn = !xTurn;
    }
    
    public boolean isValidMove(int x, int y) {
        return board[x][y] == '_' || board[x][y] == ' ';
    }
    
    public String[] printBoard() {
        String[] ret = new String[board.length];
        
        ret[0] = fetchPretty(0,0) + Colors.UNDERLINE + "|" + fetchPretty(1,0) + Colors.UNDERLINE + "|" +  fetchPretty(2,0);
        ret[1] = fetchPretty(0,1) + Colors.UNDERLINE + "|" + fetchPretty(1,1) + Colors.UNDERLINE + "|" +  fetchPretty(2,1);
        ret[2] = fetchPretty(0,2, false) + "|" + fetchPretty(1,2, false) + "|" +  fetchPretty(2,2, false);
        return ret;
    }
    
    private String fetchPretty(int x, int y) {
        return fetchPretty(x, y, true);
    }
    
    private String fetchPretty(int x, int y, boolean underlined) {
        StringBuilder b = new StringBuilder();
        boolean isTaken = (board[x][y] == 'X' || board[x][y] == 'O');
        
        if(isTaken) {
            b.append(underlined ? Colors.UNDERLINE : Colors.NORMAL);
            b.append(board[x][y]);
            b.append(Colors.NORMAL);
        } else {
            b.append(Colors.BLUE);
            b.append(encode(x,y));
            b.append(Colors.NORMAL);
        }
        
        return b.toString();
    }
    
    public static char encode(int x, int y) {
        return (char) ('A' + (y*3 + x));
    }
    
    public static int[] decode(char c) {
        c -= 'A';
        int y = c / 3, x = c % 3;
        return new int[]{x,y};
    }

    public boolean isXTurn() {
        return xTurn;
    }
    
    
    public static void main(String ... args) {
//        TTTGame g = new TTTGame("X", "O");
//        
//        g.placeX(1,1);
//        
//        System.out.println("\n");
//        for(String s : g.printBoard()) {
//            System.out.println(s);
//        }
//        
//        System.out.println(g.isWon());
//        System.out.println(g.getWinner());
        
        System.out.println(encode(0,0));
        for(int i : decode('E'))
            System.out.println(i);
    }
    
    public String getWinner() {
        char winner = 0;
        
        //check full board
        boolean full = true;
        for(char[] arr : board)
            for(char c : arr)
                full &= (c == 'X' || c == 'O');
        
        //check columns
        for(int x = 0; x < board.length; x++) {
            if(board[x][0] == board[x][1] 
                           && board[x][1] == board[x][2]) {
                winner = board[x][0];
                System.out.println(String.format("won b/c had [%s][*]", x));
            }
        }
        
        //check rows
        for(int y = 0; y < board.length; y++) {
            if(board[0][y] == board[1][y] 
                           && board[1][y] == board[2][y]) {
                winner = board[0][y];
                System.out.println(String.format("won b/c had [*][%s]", y));
            }
        }
        
        //check diags
        if(board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            winner = board[0][0];
            //System.out.println("Won diagonally from top-left");
        }
        if(board[0][2] == board[1][1] && board[2][0] == board[1][1]) {
            winner = board[0][2];
            System.out.println("Won diagonally from top-right");
        }
        
        if(winner == 'X')
            return xPlayer;
        else if(winner == 'O')
            return oPlayer;
        else if(full)
            return "Nobody";
        else
            return null;
        
    }
    
    public boolean isWon() {
        return getWinner() != null;
    }
}
