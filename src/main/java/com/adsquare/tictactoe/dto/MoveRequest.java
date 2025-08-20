package com.adsquare.tictactoe.dto;

import com.adsquare.tictactoe.model.Player;

public record MoveRequest(int row, int col, Player player) {
}
