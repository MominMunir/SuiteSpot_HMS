package com.suitespot.service;

import com.suitespot.entity.SystemSettings;
import com.suitespot.repository.SystemSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class SystemSettingsService {

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    public SystemSettings getSettings() {
        return systemSettingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> systemSettingsRepository.save(new SystemSettings()));
    }

    public SystemSettings updateSettings(SystemSettings settings) {
        return systemSettingsRepository.save(settings);
    }

    public void resetToDefaults() {
        systemSettingsRepository.deleteAll();
        systemSettingsRepository.save(new SystemSettings());
    }
}
