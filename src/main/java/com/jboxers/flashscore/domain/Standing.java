package com.jboxers.flashscore.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Standing {
    private int position;
    private String team;
    private int matchesPlayed;
    private int wins;
    private int loses;
    private int draws;
    private String goalDifference;
    private int points;
}
