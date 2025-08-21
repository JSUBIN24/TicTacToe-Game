package com.adsquare.tictactoe.service;

import com.adsquare.tictactoe.model.Game;
import com.adsquare.tictactoe.model.GameStatus;
import com.adsquare.tictactoe.model.Player;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    public Game applyMoveInMemory(Game game, int row, int col, Player player){

        validateBounds(row, col);

        if(game.getStatus() != GameStatus.IN_PROGRESS){
            throw new IllegalArgumentException("Game Already finished");
        }

        if(game.getNextPlayer() != player){
            throw new IllegalArgumentException("It's not " + player + " turn");
        }

        int index = row * 3 + col;

        String board = game.getBoard();
        if (board.charAt(index) != '_'){
                throw new IllegalArgumentException("Cell is occupied");
        }

        char markStatus = (player == Player.X) ? 'X': 'O';
        StringBuilder stringBuilder = new StringBuilder(board);
        stringBuilder.setCharAt(index, markStatus);
        game.setBoard(stringBuilder.toString());

        if(hasWon(stringBuilder.toString(), markStatus)){
            game.setStatus(GameStatus.WOW);
            return game;
        }

        if (stringBuilder.indexOf("_") == -1){
            game.setStatus(GameStatus.DRAW);
            return game;
        }

        game.setNextPlayer(player == player.X ? player.O : Player.X);
        return game;
    }

    private void validateBounds(int row, int col) {
        if (row < 0 || row > 2 || col < 0 || col > 2){
            throw new IllegalArgumentException("Row/Col out of bounds");
        }
    }

    private boolean hasWon(String board, char markSign) {
        int [][] lines = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
        };

        for (int [] linesInBoard : lines){
            if (board.charAt(linesInBoard[0]) == markSign && board.charAt(linesInBoard[1]) == markSign &&  board.charAt(linesInBoard[2]) == markSign) return true;
        }
        return false;
    }

}
