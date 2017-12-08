package com.jboxers.flashscore.web

import com.jboxers.flashscore.domain.GameKt
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class GameKtController {

    @GetMapping("/game/{goals}")
    fun game(@PathVariable goals: Int) = GameKt(away = "asasa", home = "hi", goals = goals)
}