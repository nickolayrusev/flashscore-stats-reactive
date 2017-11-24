package com.jboxers.flashscore.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
    private Boolean over;
    private Boolean under;
    private Integer totalGoals;
    private Boolean bothTeamsScored;

    private static String[] splitScore(String score) {
        return score.contains("(") ?
                score.substring(0, score.indexOf("(")).split(":") : score.split(":");
    }

    private Optional<Integer> getHomeScore() {
        return hasScore() ? Optional.of(new Integer(splitScore(score)[0].trim())) : Optional.empty();
    }

    private Optional<Integer> getAwayScore() {
        return hasScore() ? Optional.of(new Integer(splitScore(score)[1].trim())) : Optional.empty();
    }

    private boolean hasScore() {
        return splitScore(score).length == 2;
    }

    @JsonGetter
    public boolean isOver() {
        return getTotalGoals() > 2.5;
    }

    @JsonGetter
    public Integer getTotalGoals() {
        return Stream.of(getAwayScore(),getHomeScore())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(0,(a,b)-> a + b);
    }

    @JsonGetter
    public boolean isUnder() {
        return !isOver();
    }

    @JsonGetter
    public boolean isBothTeamsScored() {
        return Stream.of(getAwayScore(),getHomeScore())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(q->!Objects.equals(0,q))
                .count() > 1;
    }
}
