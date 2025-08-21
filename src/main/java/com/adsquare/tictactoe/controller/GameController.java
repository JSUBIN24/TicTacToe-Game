package com.adsquare.tictactoe.controller;


import com.adsquare.tictactoe.dto.CreateGameResponse;
import com.adsquare.tictactoe.dto.GameResponse;
import com.adsquare.tictactoe.dto.MoveRequest;
import com.adsquare.tictactoe.model.Game;
import com.adsquare.tictactoe.service.GameService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Slf4j
@Validated
public class GameController {


    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<CreateGameResponse> createGame(){
        CreateGameResponse response = gameService.createNewGame();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public GameResponse getGame (@PathVariable UUID id){
        return GameResponse.buildGameResponse(gameService.getGame(id));
    }

    @GetMapping
    public Page<GameResponse> listGames(@RequestParam(defaultValue = "" + DEFAULT_PAGE) @Min(0) int page,
                                          @RequestParam(defaultValue = "" + DEFAULT_SIZE)@Min(1) @Max(MAX_PAGE_SIZE) int size){
        Pageable pageable = PageRequest.of(page,size);
        return gameService.listOfGames(pageable).map(GameResponse::buildGameResponse);
    }
    @PostMapping("/{id}/moves")
    public GameResponse makeMove(@PathVariable UUID id, @RequestBody MoveRequest moveRequest){
        Game game = gameService.makeMove(id, moveRequest.row(), moveRequest.col(), moveRequest.player());
        return GameResponse.buildGameResponse(game);
    }

    @PostMapping("/{id}/reset")
    public GameResponse resetGame(@PathVariable UUID id) {
        return GameResponse.buildGameResponse(gameService.resetGame(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable UUID id) {
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }
}
