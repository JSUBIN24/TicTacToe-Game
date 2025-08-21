package com.adsquare.tictactoe.service;

import com.adsquare.tictactoe.dto.CreateGameResponse;
import com.adsquare.tictactoe.exception.GameFinishedException;
import com.adsquare.tictactoe.exception.GameNotFoundException;
import com.adsquare.tictactoe.exception.InvalidMoveException;
import com.adsquare.tictactoe.model.Game;
import com.adsquare.tictactoe.model.GameStatus;
import com.adsquare.tictactoe.model.Player;
import com.adsquare.tictactoe.repository.GameRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.adsquare.tictactoe.util.BoardUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 10L;

    private final GameRepository repository;


    @Transactional
    public CreateGameResponse createNewGame() {
        log.info("Creating new game");
        Game game = repository.save(new Game());
        log.info("Created new game with ID: {}", game.getId());
        return new CreateGameResponse(game.getId(), game.getBoard(), game.getNextPlayer(), game.getStatus());
    }

    @Transactional
    public Game makeMove(UUID id, int row, int col, Player player) {
        log.info("Making move for game {}: player={}, row={}, col={}", id, player, row, col);

        int attempts = 0;
        while (true) {
            try {
                Game game = getGame(id);

                validateGameInProgress(game);
                Game updated = applyMoveInMemory(game, row, col, player);

                Game saved = repository.saveAndFlush(updated);

                log.info("Move completed successfully for game {}", id);
                return saved;

            } catch (OptimisticLockingFailureException | OptimisticLockException e) {
                attempts++;
                log.warn("Optimistic lock conflict for game {} (attempt {})", id, attempts);
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    log.error("Max retry attempts reached for game {}", id);
                    throw e;
                }
                sleepQuietly(RETRY_BACKOFF_MS);
            }
        }
    }

    @Transactional
    public Game resetGame(UUID id) {
        Game game = repository.findById(id)
                .orElseThrow(() -> new GameNotFoundException("Game not found: " + id));

        game.setBoard("_________");
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setNextPlayer(Player.X);

        return repository.saveAndFlush(game);
    }

    @Transactional
    public void deleteGame(UUID id) {
        if (!repository.existsById(id)) {
            throw new GameNotFoundException("Game not found: " + id);
        }
        repository.deleteById(id);
    }


    @Transactional(readOnly = true)
    public Game getGame(UUID id) {
        log.debug("Fetching game with ID: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new GameNotFoundException("Game not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Game> listOfGames(Pageable pageable) {
        log.debug("Fetching games list with pageable: {}", pageable);
        return repository.findAll(pageable);
    }


    public Game applyMoveInMemory(Game game, int row, int col, Player player) {

        Game gameCopy = createCopyOfGame(game);
        validateMove(gameCopy, row, col, player);

        int index = toIndex(row, col);
        String updatedBoard = setAt(gameCopy.getBoard(), index, markOf(player));
        gameCopy.setBoard(updatedBoard);

        updateGameState(gameCopy, player, updatedBoard);
        return gameCopy;
    }

    private Game createCopyOfGame(Game orginalGame) {
        Game copyingGame = new Game();
        copyingGame.setId(orginalGame.getId());
        copyingGame.setBoard(orginalGame.getBoard());
        copyingGame.setStatus(orginalGame.getStatus());
        copyingGame.setNextPlayer(orginalGame.getNextPlayer());
        copyingGame.setVersion(orginalGame.getVersion());
        return copyingGame;
    }

    private void validateMove(Game game, int row, int col, Player player) {
        validateBounds(row, col);
        validateGameInProgress(game);
        validatePlayerTurn(game, player);
        validateCellEmpty(game, row, col);
    }

    private void validateBounds(int row, int col) {
        if (!withinBounds(row, col)) {
            throw new InvalidMoveException("Row/Col out of bounds");
        }
    }

    private void validateCellEmpty(Game game, int row, int col) {
        int index = toIndex(row, col);
        if (game.getBoard().charAt(index) != EMPTY_CELL) {
            throw new InvalidMoveException("Cell is occupied");
        }
    }

    private void validatePlayerTurn(Game game, Player player) {
        if (game.getNextPlayer() != player) {
            throw new InvalidMoveException("Invalid turn. Expected: " + game.getNextPlayer() + ", got: " + player);
        }
    }

    private void validateGameInProgress(Game game) {
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameFinishedException("Game already finished with status: " + game.getStatus());
        }
    }

    private void updateGameState(Game game, Player player, String board) {
        char mark = markOf(player);

        if (hasWon(board, mark)) {
            game.setStatus(player == Player.X ? GameStatus.X_WON : GameStatus.O_WON); // Option A winner encoding
            log.info("Game {} won by player {}", game.getId(), player);
            return;
        }

        if (isDraw(board)) {
            game.setStatus(GameStatus.DRAW);
            log.info("Game {} ended in a draw", game.getId());
            return;
        }

        game.setNextPlayer(opposite(player));
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

}
