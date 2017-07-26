package com.jboxers.flashscore.domain;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by nikolayrusev on 7/3/17.
 */
@Data
@Builder
public class Stat {
    private static final int LIMIT = 5;
    private String id;
    private String status;
    private boolean isLive;
    private List<Game> games;

    @JsonGetter
    public float getOverPercentage() {
        int size = getSize();
        return size == 0 ? size : (float)getLastNGames(LIMIT).stream().filter(Game::isOver).count() / size * 100;
    }

    @JsonGetter
    public float getUnderPercentage() {
        int size = getSize();
        return size == 0 ? size : (float)getLastNGames(LIMIT).stream().filter(Game::isUnder).count() / size * 100;
    }

    @JsonGetter
    public float getBothTeamsScoredPercentage(){
        int size = getSize();
        return size == 0 ? size : (float)getLastNGames(LIMIT).stream().filter(Game::isBothTeamsScored).count() / size * 100;
    }

    private List<Game> getLastNGames(int n) {
        return games.stream().limit(n).collect(toList());
    }

    public int getSize() {
        return games.size() < LIMIT ? games.size() : LIMIT;
    }

}
