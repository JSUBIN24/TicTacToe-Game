package com.adsquare.tictactoe.service;

import com.adsquare.tictactoe.model.Game;
import com.adsquare.tictactoe.model.GameStatus;
import com.adsquare.tictactoe.model.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    private final GameService gameService = new GameService();


    @Test
    void xWinsWithTopRow() {

        Game game = new Game();

        game = gameService.applyMoveInMemory(game, 0, 0, Player.X);
        game = gameService.applyMoveInMemory(game, 1, 0, Player.O);
        game = gameService.applyMoveInMemory(game, 0, 1, Player.X);
        game = gameService.applyMoveInMemory(game, 1, 1, Player.O);
        game = gameService.applyMoveInMemory(game, 0, 2, Player.X);

        assertEquals(GameStatus.WOW, game.getStatus());
        assertEquals(Player.X, game.getNextPlayer());
    }

    @Test
    void invalidMoveOnOccupiedCellThrows() {
        Game game = new Game();
        gameService.applyMoveInMemory(game, 0, 0, Player.X);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> gameService.applyMoveInMemory(game, 0, 0, Player.O));
        assertTrue(ex.getMessage().contains("Cell is occupied"));
    }

    @Test
    void wrongTurn_ShouldThrowItIsNotAPlayerTurn() {

        Game game = new Game();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> gameService.applyMoveInMemory(game, 0, 0, Player.O));
        assertTrue(ex.getMessage().contains("It's not"));
    }

    @Test
    void drawWhenBoardFullNoWinner() {
        Game game = new Game();
        game = gameService.applyMoveInMemory(game, 0, 0, Player.X);
        game = gameService.applyMoveInMemory(game, 1, 0, Player.O);
        game = gameService.applyMoveInMemory(game, 2, 0, Player.X);
        game = gameService.applyMoveInMemory(game, 0, 1, Player.O);
        game = gameService.applyMoveInMemory(game, 1, 1, Player.X);
        game = gameService.applyMoveInMemory(game, 2, 2, Player.O);
        game = gameService.applyMoveInMemory(game, 2, 1, Player.X);
        game = gameService.applyMoveInMemory(game, 0, 2, Player.O);
        game = gameService.applyMoveInMemory(game, 1, 2, Player.X);

        assertEquals(GameStatus.DRAW, game.getStatus());

    }


}