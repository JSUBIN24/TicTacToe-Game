package com.adsquare.tictactoe.repository;

import com.adsquare.tictactoe.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {
}
