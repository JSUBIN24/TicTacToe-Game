package com.adsquare.tictactoe.dto;

import com.adsquare.tictactoe.model.Game;
import com.adsquare.tictactoe.model.GameStatus;
import com.adsquare.tictactoe.model.Player;

import java.util.UUID;

public record GameResponse(UUID gameId, String board, Player nextPlayer, GameStatus gameStatus) {
    public static GameResponse buildGameResponse (Game game) {
        return new GameResponse(game.getId(), game.getBoard(),game.getNextPlayer(),game.getStatus());
    }
}
