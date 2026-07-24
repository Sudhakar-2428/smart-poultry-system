package com.poultry.backend.service;

import com.poultry.backend.dto.ReverseGeocodeRequest;
import com.poultry.backend.dto.ReverseGeocodeResponse;

public interface LocationService {
    ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request);
}
