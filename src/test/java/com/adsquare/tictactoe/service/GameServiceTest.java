package com.adsquare.tictactoe.service;


import com.adsquare.tictactoe.dto.CreateGameResponse;
import com.adsquare.tictactoe.exception.GameFinishedException;
import com.adsquare.tictactoe.exception.GameNotFoundException;
import com.adsquare.tictactoe.exception.InvalidMoveException;
import com.adsquare.tictactoe.model.Game;
import com.adsquare.tictactoe.model.GameStatus;
import com.adsquare.tictactoe.model.Player;
import com.adsquare.tictactoe.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository repository;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private UUID testGameId;

    @BeforeEach
    void setUp() {
        testGameId = UUID.randomUUID();
        testGame = new Game();
        testGame.setId(testGameId);
    }

    @Nested
    class GameCreationTests {

        @Test
        void shouldCreateNewGameSuccessfully() {
            // Given
            Game savedGame = new Game();
            savedGame.setId(testGameId);
            when(repository.save(any(Game.class))).thenReturn(savedGame);

            // When
            CreateGameResponse response = gameService.createNewGame();

            // Then
            assertNotNull(response);
            assertEquals(testGameId, response.gameId());
            assertEquals("_________", response.board());
            assertEquals(Player.X, response.nextPlayer());
            assertEquals(GameStatus.IN_PROGRESS, response.status());
            verify(repository).save(any(Game.class));
        }
    }

    @Nested
    class GameRetrievalTests {

        @Test
        void shouldGetGameByIdSuccessfully() {
            // Given
            when(repository.findById(testGameId)).thenReturn(Optional.of(testGame));

            // When
            Game result = gameService.getGame(testGameId);

            // Then
            assertNotNull(result);
            assertEquals(testGame, result);
            verify(repository).findById(testGameId);
        }

        @Test
        void shouldThrowExceptionWhenGameNotFound() {
            // Given
            when(repository.findById(testGameId)).thenReturn(Optional.empty());

            // When & Then
            GameNotFoundException exception = assertThrows(
                    GameNotFoundException.class,
                    () -> gameService.getGame(testGameId)
            );
            assertTrue(exception.getMessage().contains("Game not found"));
            verify(repository).findById(testGameId);
        }

        @Test
        void shouldListGamesWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Game> games = List.of(testGame);
            Page<Game> gamePage = new PageImpl<>(games, pageable, 1);
            when(repository.findAll(pageable)).thenReturn(gamePage);

            // When
            Page<Game> result = gameService.listOfGames(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(testGame, result.getContent().get(0));
            verify(repository).findAll(pageable);
        }
    }

    @Nested
    class GameResetAndDeleteTests {

        @Test
        void shouldResetGameSuccessfully() {
            // Given
            testGame.setBoard("XOX_O____");
            testGame.setStatus(GameStatus.X_WON);
            testGame.setNextPlayer(Player.O);
            when(repository.findById(testGameId)).thenReturn(Optional.of(testGame));
            when(repository.saveAndFlush(testGame)).thenReturn(testGame);

            // When
            Game result = gameService.resetGame(testGameId);

            // Then
            assertEquals("_________", result.getBoard());
            assertEquals(GameStatus.IN_PROGRESS, result.getStatus());
            assertEquals(Player.X, result.getNextPlayer());
            verify(repository).saveAndFlush(testGame);
        }

        @Test
        void shouldThrowExceptionWhenResettingNonExistentGame() {
            // Given
            when(repository.findById(testGameId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(GameNotFoundException.class, () -> gameService.resetGame(testGameId));
        }

        @Test
        void shouldDeleteGameSuccessfully() {
            // Given
            when(repository.existsById(testGameId)).thenReturn(true);

            // When
            gameService.deleteGame(testGameId);

            // Then
            verify(repository).existsById(testGameId);
            verify(repository).deleteById(testGameId);
        }

        @Test
        void shouldThrowExceptionWhenDeletingNonExistentGame() {
            // Given
            when(repository.existsById(testGameId)).thenReturn(false);

            // When & Then
            assertThrows(GameNotFoundException.class, () -> gameService.deleteGame(testGameId));
            verify(repository).existsById(testGameId);
            verify(repository, never()).deleteById(any());
        }
    }

    @Nested
    class MoveMakingTests {

        @Test
        void shouldMakeMoveSuccessfully() {
            // Given
            when(repository.findById(testGameId)).thenReturn(Optional.of(testGame));
            when(repository.saveAndFlush(any(Game.class))).thenReturn(testGame);

            // When
            Game result = gameService.makeMove(testGameId, 0, 0, Player.X);

            // Then
            assertNotNull(result);
            verify(repository).findById(testGameId);
            verify(repository).saveAndFlush(any(Game.class));
        }

        @Test
        void shouldRetryOnOptimisticLockExceptionAndSucceed() {
            // Given
            when(repository.findById(testGameId)).thenReturn(Optional.of(testGame));
            when(repository.saveAndFlush(any(Game.class)))
                    .thenThrow(new OptimisticLockingFailureException("Lock failed"))
                    .thenReturn(testGame);

            // When
            Game result = gameService.makeMove(testGameId, 0, 0, Player.X);

            // Then
            assertNotNull(result);
            verify(repository, times(2)).findById(testGameId);
            verify(repository, times(2)).saveAndFlush(any(Game.class));
        }

        @Test
        void shouldFailAfterMaxRetryAttemptsOnOptimisticLock() {
            // Given
            when(repository.findById(testGameId)).thenReturn(Optional.of(testGame));
            when(repository.saveAndFlush(any(Game.class)))
                    .thenThrow(new OptimisticLockingFailureException("Lock failed"));

            // When & Then
            assertThrows(OptimisticLockingFailureException.class,
                    () -> gameService.makeMove(testGameId, 0, 0, Player.X));
            verify(repository, times(3)).saveAndFlush(any(Game.class));
        }

        @Test
        @DisplayName("Should handle ObjectOptimisticLockingFailureException")
        void shouldHandleObjectOptimisticLockingFailureException() {
            // Given
            when(repository.findById(testGameId)).thenReturn(Optional.of(testGame));
            when(repository.saveAndFlush(any(Game.class)))
                    .thenThrow(new ObjectOptimisticLockingFailureException("Lock failed", null));

            // When & Then
            assertThrows(ObjectOptimisticLockingFailureException.class,
                    () -> gameService.makeMove(testGameId, 0, 0, Player.X));
        }
    }

    @Nested
    class InMemoryMoveLogicTests {

        @Test
        void shouldMakeFirstMoveSuccessfully() {
            // When
            Game result = gameService.applyMoveInMemory(testGame, 0, 0, Player.X);

            // Then
            assertEquals('X', result.getBoard().charAt(0));
            assertEquals(Player.O, result.getNextPlayer());
            assertEquals(GameStatus.IN_PROGRESS, result.getStatus());
        }

        @Test
        void xWinsWithTopRow() {
            Game game = new Game();

            game = gameService.applyMoveInMemory(game, 0, 0, Player.X);
            game = gameService.applyMoveInMemory(game, 1, 0, Player.O);
            game = gameService.applyMoveInMemory(game, 0, 1, Player.X);
            game = gameService.applyMoveInMemory(game, 1, 1, Player.O);
            game = gameService.applyMoveInMemory(game, 0, 2, Player.X);

            assertEquals(GameStatus.X_WON, game.getStatus());
        }


        @Test
        void oWinsWithAntiDiagonal() {
            Game game = new Game();

            game = gameService.applyMoveInMemory(game, 0, 0, Player.X);
            game = gameService.applyMoveInMemory(game, 0, 2, Player.O);
            game = gameService.applyMoveInMemory(game, 0, 1, Player.X);
            game = gameService.applyMoveInMemory(game, 1, 1, Player.O);
            game = gameService.applyMoveInMemory(game, 1, 0, Player.X);
            game = gameService.applyMoveInMemory(game, 2, 0, Player.O);

            assertEquals(GameStatus.O_WON, game.getStatus());
        }

        @Test
        void xWinsWithColumn() {
            Game game = new Game();

            game = gameService.applyMoveInMemory(game, 0, 0, Player.X);
            game = gameService.applyMoveInMemory(game, 0, 1, Player.O);
            game = gameService.applyMoveInMemory(game, 1, 0, Player.X);
            game = gameService.applyMoveInMemory(game, 0, 2, Player.O);
            game = gameService.applyMoveInMemory(game, 2, 0, Player.X);

            assertEquals(GameStatus.X_WON, game.getStatus());
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

    @Nested
    class MoveValidationTests {

        @Test
        void invalidMoveOnOccupiedCellThrows() {
            Game game = new Game();
            Game updatedGame = gameService.applyMoveInMemory(game, 0, 0, Player.X);

            InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                    () -> gameService.applyMoveInMemory(updatedGame, 0, 0, Player.O));
            assertTrue(ex.getMessage().contains("Cell is occupied"));
        }

        @Test
        void wrongTurn_ShouldThrowItIsNotAPlayerTurn() {
            Game game = new Game();

            InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                    () -> gameService.applyMoveInMemory(game, 0, 0, Player.O));
            assertTrue(ex.getMessage().contains("Invalid turn. Expected"));
        }

        @Test
        void shouldThrowExceptionForOutOfBoundsMoves() {
            Game game = new Game();

            // Test all invalid coordinates
            assertThrows(InvalidMoveException.class, () -> gameService.applyMoveInMemory(game, -1, 0, Player.X));
            assertThrows(InvalidMoveException.class, () -> gameService.applyMoveInMemory(game, 0, -1, Player.X));
            assertThrows(InvalidMoveException.class, () -> gameService.applyMoveInMemory(game, 3, 0, Player.X));
            assertThrows(InvalidMoveException.class, () -> gameService.applyMoveInMemory(game, 0, 3, Player.X));
            assertThrows(InvalidMoveException.class, () -> gameService.applyMoveInMemory(game, -1, -1, Player.X));
            assertThrows(InvalidMoveException.class, () -> gameService.applyMoveInMemory(game, 3, 3, Player.X));
        }

        @Test
        void shouldAcceptAllValidBoundaryCoordinates() {
            for (int row = 0; row <= 2; row++) {
                for (int col = 0; col <= 2; col++) {
                    Game game = new Game();
                    int finalRow = row;
                    int finalCol = col;
                    assertDoesNotThrow(() -> gameService.applyMoveInMemory(game, finalRow, finalCol, Player.X));
                }
            }
        }

        @Test
        void shouldThrowExceptionWhenGameAlreadyFinishedXWon() {
            Game game = new Game();
            game.setStatus(GameStatus.X_WON);

            GameFinishedException ex = assertThrows(GameFinishedException.class,
                    () -> gameService.applyMoveInMemory(game, 0, 0, Player.X));
            assertTrue(ex.getMessage().contains("Game already finished"));
        }

        @Test
        void shouldThrowExceptionWhenGameAlreadyFinishedOWon() {
            Game game = new Game();
            game.setStatus(GameStatus.O_WON);

            GameFinishedException ex = assertThrows(GameFinishedException.class,
                    () -> gameService.applyMoveInMemory(game, 0, 0, Player.O));
            assertTrue(ex.getMessage().contains("Game already finished"));
        }

        @Test
        void shouldThrowExceptionWhenGameAlreadyFinishedDraw() {
            Game game = new Game();
            game.setStatus(GameStatus.DRAW);

            GameFinishedException ex = assertThrows(GameFinishedException.class,
                    () -> gameService.applyMoveInMemory(game, 0, 0, Player.X));
            assertTrue(ex.getMessage().contains("Game already finished"));
        }
    }

    @Nested
    class EdgeCasesTests {

        @Test
        void shouldHandleAlternatingPlayersCorrectlyThroughFullGame() {
            Game game = new Game();

            assertEquals(Player.X, game.getNextPlayer());
            game = gameService.applyMoveInMemory(game, 0, 0, Player.X);
            assertEquals(Player.O, game.getNextPlayer());

            game = gameService.applyMoveInMemory(game, 0, 1, Player.O);
            assertEquals(Player.X, game.getNextPlayer());

            game = gameService.applyMoveInMemory(game, 0, 2, Player.X);
            assertEquals(Player.O, game.getNextPlayer());
        }

        @Test
        void shouldDetectWinImmediatelyWhenWinningMoveIsMade() {
            Game game = new Game();

            game = gameService.applyMoveInMemory(game, 0, 0, Player.X);
            game = gameService.applyMoveInMemory(game, 1, 0, Player.O);
            game = gameService.applyMoveInMemory(game, 0, 1, Player.X);
            game = gameService.applyMoveInMemory(game, 1, 1, Player.O);

            assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
            game = gameService.applyMoveInMemory(game, 0, 2, Player.X);
            assertEquals(GameStatus.X_WON, game.getStatus());
        }

        @Test
        void shouldNotChangeNextPlayerWhenGameEnds() {
            Game game = new Game();

            game = gameService.applyMoveInMemory(game, 0, 0, Player.X);
            game = gameService.applyMoveInMemory(game, 1, 0, Player.O);
            game = gameService.applyMoveInMemory(game, 0, 1, Player.X);
            game = gameService.applyMoveInMemory(game, 1, 1, Player.O);

            assertEquals(Player.X, game.getNextPlayer());

            game = gameService.applyMoveInMemory(game, 0, 2, Player.X);

            assertEquals(GameStatus.X_WON, game.getStatus());
            assertEquals(Player.X, game.getNextPlayer());
        }

        @Test
        @DisplayName("Should handle all corner positions correctly")
        void shouldHandleAllCornerPositionsCorrectly() {
            int[][] corners = {{0, 0}, {0, 2}, {2, 0}, {2, 2}};

            for (int[] corner : corners) {
                Game game = new Game();
                Game result = gameService.applyMoveInMemory(game, corner[0], corner[1], Player.X);
                int index = corner[0] * 3 + corner[1];
                assertEquals('X', result.getBoard().charAt(index));
                assertEquals(Player.O, result.getNextPlayer());
            }
        }

        @Test
        @DisplayName("Should handle center position correctly")
        void shouldHandleCenterPositionCorrectly() {
            Game game = new Game();
            Game result = gameService.applyMoveInMemory(game, 1, 1, Player.X);
            assertEquals('X', result.getBoard().charAt(4));
            assertEquals(Player.O, result.getNextPlayer());
        }
    }
}