package com.adsquare.tictactoe.util;

import com.adsquare.tictactoe.model.Player;
import org.junit.jupiter.api.Test;

import static com.adsquare.tictactoe.util.BoardUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class BoardUtilTest {


    private static String boardFrom(char fill) {
        return String.valueOf(fill).repeat(BOARD_SIZE * BOARD_SIZE);
    }

    private static String boardWith(char base, char mark, int... idx) {
        char[] b = boardFrom(base).toCharArray();
        for (int i : idx) b[i] = mark;
        return new String(b);
    }


    @Test
    void toIndex_mapsCorrectly() {
        assertEquals(0, toIndex(0, 0));
        assertEquals(2, toIndex(0, 2));
        assertEquals(3, toIndex(1, 0));
        assertEquals(8, toIndex(2, 2));
    }

    @Test
    void withinBounds_validAndInvalid() {
        assertTrue(withinBounds(0, 0));
        assertTrue(withinBounds(2, 2));
        assertFalse(withinBounds(-1, 0));
        assertFalse(withinBounds(0, -1));
        assertFalse(withinBounds(3, 0));
        assertFalse(withinBounds(0, 3));
    }


    @Test
    void setAt_updatesBoardAtIndex() {
        String board = "_________"; // 9 underscores
        String after = setAt(board, 0, 'X');
        assertEquals('X', after.charAt(0));
        assertEquals('_', after.charAt(1));
        assertEquals('_', after.charAt(8));

        String after2 = setAt(after, 8, 'O');
        assertEquals('O', after2.charAt(8));
    }


    @Test
    void hasWon_trueForTopRowX() {
        String board = boardWith('_', 'X', 0, 1, 2);
        assertTrue(hasWon(board, 'X'));
        assertFalse(hasWon(board, 'O'));
    }

    @Test
    void hasWon_trueForMiddleColO() {
        String board = boardWith('_', 'O', 1, 4, 7);
        assertTrue(hasWon(board, 'O'));
        assertFalse(hasWon(board, 'X'));
    }

    @Test
    void hasWon_trueForDiagonalX() {
        String board = boardWith('_', 'X', 0, 4, 8);
        assertTrue(hasWon(board, 'X'));
        assertFalse(hasWon(board, 'O'));
    }

    @Test
    void hasWon_falseWhenNoLine() {
        String board = "XOX_OX___"; // mixed, no winner
        assertFalse(hasWon(board, 'X'));
        assertFalse(hasWon(board, 'O'));
    }


    @Test
    void isDraw_trueWhenBoardFullAndNoUnderscores() {
        String draw = "XOXOOXXXO";
        assertTrue(isDraw(draw));
        assertFalse(hasWon(draw, 'X'));
        assertFalse(hasWon(draw, 'O'));
    }

    @Test
    void isDraw_falseWhenEmptyCellsRemain() {
        assertFalse(isDraw("_________"));
        assertFalse(isDraw("X________"));
    }


    @Test
    void markOf_returnsCorrectChar() {
        assertEquals('X', markOf(Player.X));
        assertEquals('O', markOf(Player.O));
    }

    @Test
    void opposite_returnsOtherPlayer() {
        assertEquals(Player.O, opposite(Player.X));
        assertEquals(Player.X, opposite(Player.O));
    }
}