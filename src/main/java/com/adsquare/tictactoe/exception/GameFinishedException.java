package com.adsquare.tictactoe.exception;

public class GameFinishedException extends RuntimeException {
    public GameFinishedException(String message) {
        super(message);
    }
}
