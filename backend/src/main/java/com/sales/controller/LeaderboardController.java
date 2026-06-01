package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/top3")
    public ApiResponse<List<Map<String, Object>>> top3() {
        return leaderboardService.getTop3();
    }
}
