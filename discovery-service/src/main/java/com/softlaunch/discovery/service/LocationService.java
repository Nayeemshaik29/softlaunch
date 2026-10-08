package com.softlaunch.discovery.service;

import com.softlaunch.discovery.client.SwipedUsersClient;
import com.softlaunch.discovery.dto.LocationRequest;
import com.softlaunch.discovery.dto.NearbyUserResponse;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LocationService {

    private static final int SEARCH_LIMIT = 200;
    private static final String GEO_KEY = "geo:users";
    private static final int MAX_RESULTS = 50;

    private final StringRedisTemplate redis;
    private final SwipedUsersClient swipedUsersClient;

    public LocationService(StringRedisTemplate redis, SwipedUsersClient swipedUsersClient) {
        this.redis = redis;
        this.swipedUsersClient = swipedUsersClient;
    }

    public void updateLocation(UUID me, LocationRequest request) {
        redis.opsForGeo().add(GEO_KEY, new Point(request.longitude(), request.latitude()), me.toString());
    }

    public List<NearbyUserResponse> nearby(UUID me, int radiusKm) {
        requireLocation(me);
        Set<String> alreadySwiped = swipedUsersClient.swipedBy(me);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                GEO_KEY,
                GeoReference.fromMember(me.toString()),
                new Distance(radiusKm, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(SEARCH_LIMIT));

        if (results == null) {
            return List.of();
        }

        return results.getContent().stream()
                .filter(r -> !r.getContent().getName().equals(me.toString()))
                .filter(r -> !alreadySwiped.contains(r.getContent().getName()))
                .map(r -> new NearbyUserResponse(
                        UUID.fromString(r.getContent().getName()),
                        roundUpKm(r.getDistance().getValue())))
                .limit(MAX_RESULTS)
                .toList();
    }

    private void requireLocation(UUID me) {
        List<Point> positions = redis.opsForGeo().position(GEO_KEY, me.toString());
        if (positions == null || positions.isEmpty() || positions.get(0) == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Set your location first");
        }
    }

    private int roundUpKm(double km) {
        return Math.max(1, (int) Math.ceil(km));
    }
}