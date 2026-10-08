package com.softlaunch.discovery.controller;

import com.softlaunch.discovery.dto.LocationRequest;
import com.softlaunch.discovery.dto.NearbyUserResponse;
import com.softlaunch.discovery.service.LocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final LocationService locationService;

    public DiscoveryController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PutMapping("/me/location")
    public ResponseEntity<Void> updateLocation(@RequestHeader("X-User-Id") UUID me,
                                               @Valid @RequestBody LocationRequest request) {
        locationService.updateLocation(me, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby")
    public List<NearbyUserResponse> nearby(@RequestHeader("X-User-Id") UUID me,
                                           @RequestParam(defaultValue = "25") @Min(1) @Max(100) int radiusKm) {
        return locationService.nearby(me, radiusKm);
    }
}