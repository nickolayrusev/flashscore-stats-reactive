package com.jboxers.flashscore.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@ToString
@Data
public class FullStat {
    private Stat stat;
    private List<Standing> standing;
}
