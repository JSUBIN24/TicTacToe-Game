package com.adsquare.tictactoe.controller;

import com.adsquare.tictactoe.dto.CreateGameResponse;
import com.adsquare.tictactoe.dto.MoveRequest;
import com.adsquare.tictactoe.exception.GameNotFoundException;
import com.adsquare.tictactoe.exception.InvalidMoveException;
import com.adsquare.tictactoe.model.Game;
import com.adsquare.tictactoe.model.GameStatus;
import com.adsquare.tictactoe.model.Player;
import com.adsquare.tictactoe.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @Autowired
    private ObjectMapper objectMapper;

    private Game testGame;
    private UUID testGameId;

    @BeforeEach
    void setUp() {
        testGameId = UUID.randomUUID();
        testGame = new Game();
        testGame.setId(testGameId);
        testGame.setBoard("_________");
        testGame.setStatus(GameStatus.IN_PROGRESS);
        testGame.setNextPlayer(Player.X);
    }

    @Test
    void shouldCreateGameSuccessfully() throws Exception {
        // Given
        CreateGameResponse response = new CreateGameResponse(testGameId, "_________", Player.X, GameStatus.IN_PROGRESS);
        when(gameService.createNewGame()).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/games"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(testGameId.toString()))
                .andExpect(jsonPath("$.board").value("_________"))
                .andExpect(jsonPath("$.nextPlayer").value("X"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldGetGameSuccessfully() throws Exception {
        // Given
        when(gameService.getGame(testGameId)).thenReturn(testGame);

        // When & Then
        mockMvc.perform(get("/api/v1/games/{id}", testGameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(testGameId.toString()))
                .andExpect(jsonPath("$.board").value("_________"));
    }

    @Test
    void shouldReturn404WhenGameNotFound() throws Exception {
        // Given
        when(gameService.getGame(testGameId)).thenThrow(new GameNotFoundException("Game not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/games/{id}", testGameId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListGamesWithPagination() throws Exception {
        // Given
        Page<Game> gamePage = new PageImpl<>(List.of(testGame), PageRequest.of(0, 20), 1);
        when(gameService.listOfGames(any())).thenReturn(gamePage);

        // When & Then
        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].gameId").value(testGameId.toString()));
    }

    @Test
    void shouldMakeMoveSuccessfully() throws Exception {
        // Given
        MoveRequest moveRequest = new MoveRequest(0, 0, Player.X);
        testGame.setBoard("X________");
        when(gameService.makeMove(testGameId, 0, 0, Player.X)).thenReturn(testGame);

        // When & Then
        mockMvc.perform(post("/api/v1/games/{id}/moves", testGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").value("X________"));
    }

    @Test
    void shouldRejectInvalidMove() throws Exception {
        // Given
        MoveRequest moveRequest = new MoveRequest(0, 0, Player.O);
        when(gameService.makeMove(testGameId, 0, 0, Player.O))
                .thenThrow(new InvalidMoveException("Invalid turn"));

        // When & Then
        mockMvc.perform(post("/api/v1/games/{id}/moves", testGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldResetGameSuccessfully() throws Exception {
        // Given
        when(gameService.resetGame(testGameId)).thenReturn(testGame);

        // When & Then
        mockMvc.perform(post("/api/v1/games/{id}/reset", testGameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").value("_________"));
    }

    @Test
    void shouldDeleteGameSuccessfully() throws Exception {
        // Given
        doNothing().when(gameService).deleteGame(testGameId);

        // When & Then
        mockMvc.perform(delete("/api/v1/games/{id}", testGameId))
                .andExpect(status().isNoContent());

        verify(gameService).deleteGame(testGameId);
    }
}