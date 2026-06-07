package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.entity.Tutorial;
import com.sales.service.TutorialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TutorialController {

    private final TutorialService tutorialService;

    @GetMapping("/tutorials")
    public ApiResponse<List<Tutorial>> listPublic() {
        return ApiResponse.success(tutorialService.listActive());
    }

    @GetMapping("/admin/tutorials")
    public ApiResponse<List<Tutorial>> listAll() {
        return ApiResponse.success(tutorialService.listAll());
    }

    @PostMapping("/admin/tutorials")
    public ApiResponse<Tutorial> save(@RequestBody Tutorial tutorial) {
        return ApiResponse.success(tutorialService.save(tutorial));
    }

    @DeleteMapping("/admin/tutorials/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tutorialService.delete(id);
        return ApiResponse.success();
    }
}
