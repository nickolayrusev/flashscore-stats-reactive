package com.jboxers.flashscore.domain;

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
        if(Objects.isNull(homeScore)){
            homeScore = new Integer(splitScore(score)[0].trim());
        }
        return homeScore;
    }

    private Integer getAwayScore(){
        if(Objects.isNull(awayScore)){
            awayScore = new Integer(splitScore(score)[1].trim());
        }
        return awayScore;
    }

    public boolean isOver(){
       return getTotalGoals() > 2.5;
    }

    public int getTotalGoals(){
        return getAwayScore() + getHomeScore();
    }

    public boolean isUnder(){
       return !isOver();
    }

    public boolean isBothTeamsScored(){
        return  !homeScore.equals(0) && !awayScore.equals(0);
    }
}
