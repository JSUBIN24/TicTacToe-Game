package com.adsquare.tictactoe.util;

import com.adsquare.tictactoe.model.Player;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BoardUtil {


    public static final int BOARD_SIZE = 3;
    public static final char EMPTY_CELL = '_';

    public static final int[][] WINNING_LINES = {
            {0,1,2},{3,4,5},{6,7,8},
            {0,3,6},{1,4,7},{2,5,8},
            {0,4,8},{2,4,6}
    };

    public static int toIndex(int row, int col) {
        return row * BOARD_SIZE + col;
    }

    public static boolean withinBounds(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    public static String setAt(String board, int idx, char value) {
        StringBuilder sb = new StringBuilder(board);
        sb.setCharAt(idx, value);
        return sb.toString();
    }

    public static boolean hasWon(String board, char mark) {
        for (int[] L : WINNING_LINES) {
            if (board.charAt(L[0]) == mark && board.charAt(L[1]) == mark && board.charAt(L[2]) == mark) return true;
        }
        return false;
    }

    public static boolean isDraw(String board) {
        return board.indexOf(EMPTY_CELL) < 0;
    }

    public static char markOf(Player player) {
        return player == Player.X ? 'X' : 'O';
    }

    public static Player opposite(Player player) {
        return player == Player.X ? Player.O : Player.X;
    }
}
