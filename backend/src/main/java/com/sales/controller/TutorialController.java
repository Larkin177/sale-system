package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.entity.Tutorial;
import com.sales.entity.TutorialCategory;
import com.sales.mapper.TutorialCategoryMapper;
import com.sales.service.TutorialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TutorialController {

    private final TutorialService tutorialService;
    private final TutorialCategoryMapper tutorialCategoryMapper;

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

    // ===== Category management =====

    @GetMapping("/categories")
    public ApiResponse<List<TutorialCategory>> listPublicCategories() {
        return ApiResponse.success(tutorialCategoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TutorialCategory>()
                        .orderByAsc(TutorialCategory::getSortOrder)));
    }

    @GetMapping("/admin/categories")
    public ApiResponse<List<TutorialCategory>> listCategories() {
        return ApiResponse.success(tutorialCategoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TutorialCategory>()
                        .orderByAsc(TutorialCategory::getSortOrder)));
    }

    @PostMapping("/admin/categories")
    public ApiResponse<TutorialCategory> saveCategory(@RequestBody TutorialCategory category) {
        if (category.getId() != null) {
            tutorialCategoryMapper.updateById(category);
        } else {
            tutorialCategoryMapper.insert(category);
        }
        return ApiResponse.success(category);
    }

    @DeleteMapping("/admin/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        tutorialCategoryMapper.deleteById(id);
        return ApiResponse.success();
    }
}
