package com.poultry.backend.service;

import com.poultry.backend.dto.FarmWeatherResponse;
import com.poultry.backend.dto.WeatherResponse;

public interface WeatherService {
    WeatherResponse getWeather(Double lat, Double lon);
    FarmWeatherResponse getFarmWeather(Long farmId);
    void refreshFarmWeather(Long farmId);
    void refreshAllFarmsWeather();
}
