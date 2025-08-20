package com.adsquare.tictactoe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue
    private UUID id;

    // board as 9-char string '_' means empty
    @Column(nullable = false, length = 9)
    private String board = "_________";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Player nextPlayer = Player.X;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.IN_PROGRESS;

    @Version
    private long version;
}
