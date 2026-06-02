package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.entity.SiteSetting;
import com.sales.mapper.SiteSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SiteSettingService {

    private final SiteSettingMapper siteSettingMapper;

    // 客户端展示的设置key列表
    private static final List<String> PUBLIC_KEYS = Arrays.asList(
            "site_name", "site_subtitle",
            "hero_title", "hero_subtitle", "hero_image",
            "hero_bg_color", "hero_bg_color_end",
            "feature_1_icon", "feature_1_title", "feature_1_desc",
            "feature_1_icon_url", "feature_1_content_type", "feature_1_content_value",
            "feature_2_icon", "feature_2_title", "feature_2_desc",
            "feature_2_icon_url", "feature_2_content_type", "feature_2_content_value",
            "feature_3_icon", "feature_3_title", "feature_3_desc",
            "feature_3_icon_url", "feature_3_content_type", "feature_3_content_value",
            "footer_text",
            "pay_title", "pay_subtitle",
            "download_title", "download_subtitle"
    );

    public Map<String, String> getAllSettings() {
        List<SiteSetting> settings = siteSettingMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        settings.forEach(s -> map.put(s.getSettingKey(), s.getSettingValue()));
        return map;
    }

    public Map<String, String> getPublicSettings() {
        List<SiteSetting> settings = siteSettingMapper.selectList(
                new LambdaQueryWrapper<SiteSetting>().in(SiteSetting::getSettingKey, PUBLIC_KEYS));
        Map<String, String> map = new HashMap<>();
        settings.forEach(s -> map.put(s.getSettingKey(), s.getSettingValue()));
        return map;
    }

    public String getSetting(String key) {
        SiteSetting setting = siteSettingMapper.selectOne(
                new LambdaQueryWrapper<SiteSetting>().eq(SiteSetting::getSettingKey, key));
        return setting != null ? setting.getSettingValue() : null;
    }

    public void updateSetting(String key, String value) {
        SiteSetting setting = siteSettingMapper.selectOne(
                new LambdaQueryWrapper<SiteSetting>().eq(SiteSetting::getSettingKey, key));
        if (setting != null) {
            setting.setSettingValue(value);
            siteSettingMapper.updateById(setting);
        } else {
            setting = new SiteSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            siteSettingMapper.insert(setting);
        }
    }

    public void batchUpdate(Map<String, String> settings) {
        settings.forEach(this::updateSetting);
    }
}
