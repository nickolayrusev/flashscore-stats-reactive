package com.jboxers.flashscore.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Created by nikolayrusev on 6/30/17.
 */
@Data
@Builder
public class Game {
    private String home;
    private String away;
    private LocalDate date;
    private String score;
    private String league;

    private Integer homeScore;
    private Integer awayScore;

    private static String[] splitScore(String score){
        return score.contains("(") ?
                score.substring(0, score.indexOf("(")).split(":") :  score.split(":");
    }

    private Integer getHomeScore(){
        if(hasScore() && Objects.isNull(homeScore)){
            homeScore = new Integer(splitScore(score)[0].trim());
        }
        return homeScore;
    }

    private Integer getAwayScore(){
        if(hasScore() && Objects.isNull(awayScore)){
            awayScore = new Integer(splitScore(score)[1].trim());
        }
        return awayScore;
    }

    private boolean hasScore(){
        return splitScore(score).length  == 2;
    }

    public boolean isOver(){
       return getTotalGoals() > 2.5;
    }

    public int getTotalGoals(){
        return hasScore() ? getAwayScore() + getHomeScore() : 0;
    }

    public boolean isUnder(){
       return !isOver();
    }

    public boolean isBothTeamsScored(){
        return hasScore() && (!homeScore.equals(0) && !awayScore.equals(0));
    }
}
