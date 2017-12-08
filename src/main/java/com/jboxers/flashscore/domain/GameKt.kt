package com.jboxers.flashscore.domain

import com.fasterxml.jackson.annotation.JsonGetter

data class GameKt(val home: String? = null,
                  val away: String? = null,
                  val over: Boolean? = false,
                  val goals: Int? = 0) {

    @JsonGetter
    fun isOver() = goals ?: 0 > 2.5

    @JsonGetter
    fun isUnder() = !isOver()
}

