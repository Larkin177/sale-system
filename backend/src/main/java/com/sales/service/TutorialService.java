package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.entity.Tutorial;
import com.sales.mapper.TutorialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TutorialService {

    private final TutorialMapper tutorialMapper;

    public List<Tutorial> listActive() {
        return tutorialMapper.selectList(
                new LambdaQueryWrapper<Tutorial>()
                        .eq(Tutorial::getStatus, "active")
                        .orderByAsc(Tutorial::getCategory, Tutorial::getSortOrder));
    }

    public List<Tutorial> listAll() {
        return tutorialMapper.selectList(
                new LambdaQueryWrapper<Tutorial>()
                        .orderByAsc(Tutorial::getCategory, Tutorial::getSortOrder));
    }

    public Tutorial save(Tutorial tutorial) {
        if (tutorial.getId() == null) {
            tutorialMapper.insert(tutorial);
        } else {
            tutorialMapper.updateById(tutorial);
        }
        return tutorial;
    }

    public void delete(Long id) {
        tutorialMapper.deleteById(id);
    }

    public Tutorial getById(Long id) {
        return tutorialMapper.selectById(id);
    }
}
