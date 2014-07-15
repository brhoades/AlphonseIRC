/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jmhertlein.alphonseirc;

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
     * Tries to make a move for ap layer
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
    
    public String[] printBoard() {
        String[] ret = new String[board.length];
        
        StringBuilder b = new StringBuilder(board.length * 2 - 1);
        for(int y = 0; y < board.length; y++) {
            for(int x = 0; x < board[y].length; x++) {
                b.append(board[x][y]);
                if(x != board.length - 1)
                    b.append("|");
            }
            ret[y] = b.toString();
            b = new StringBuilder(board.length * 2 - 1);
        }
        return ret;
    }
    
    public static void main(String ... args) {
        TTTGame g = new TTTGame("X", "O");
        
        g.placeX(1,1);
        
        System.out.println("\n");
        for(String s : g.printBoard()) {
            System.out.println(s);
        }
        
        System.out.println(g.isWon());
        System.out.println(g.getWinner());
    }
    
    public String getWinner() {
        char winner = 0;
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
        
        if(winner == 0)
            return null;
        else if(winner == 'X')
            return xPlayer;
        else if(winner == 'O')
            return oPlayer;
        else
            return null;
    }
    
    public boolean isWon() {
        return getWinner() != null;
    }
}
