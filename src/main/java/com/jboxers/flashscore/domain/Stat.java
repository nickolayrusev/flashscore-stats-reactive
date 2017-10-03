package com.jboxers.flashscore.domain;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by nikolayrusev on 7/3/17.
 */
@Data
@Builder
public class Stat {
    private static final int LIMIT = 20;
    private String id;
    private String status;
    private boolean isLive;
    private String league;
    private String score;
    private List<Game> headToHeadGames;
    private List<Game> homeTeamGames;
    private List<Game> awayTeamGames;

    @JsonGetter
    public List<Game> getHeadToHeadGames(){
        return headToHeadGames.stream().limit(LIMIT).collect(Collectors.toList());
    }

    @JsonGetter
    public List<Game> getHomeTeamGames() {
        return homeTeamGames.stream().limit(LIMIT).collect(Collectors.toList());
    }

    @JsonGetter
    public List<Game> getAwayTeamGames() {
        return awayTeamGames.stream().limit(LIMIT).collect(Collectors.toList());
    }
}
