package com.adsquare.tictactoe.dto;

import com.adsquare.tictactoe.model.GameStatus;
import com.adsquare.tictactoe.model.Player;

import java.util.UUID;

public record CreateGameResponse(UUID gameId, String board, Player nextPlayer, GameStatus status) {
}
