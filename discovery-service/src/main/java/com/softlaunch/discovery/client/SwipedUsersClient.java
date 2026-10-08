package com.softlaunch.discovery.client;

import com.softlaunch.grpc.match.v1.GetSwipedUserIdsRequest;
import com.softlaunch.grpc.match.v1.MatchQueryServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class SwipedUsersClient {

    private static final Logger log = LoggerFactory.getLogger(SwipedUsersClient.class);

    private final MatchQueryServiceGrpc.MatchQueryServiceBlockingStub stub;

    public SwipedUsersClient(MatchQueryServiceGrpc.MatchQueryServiceBlockingStub stub) {
        this.stub = stub;
    }

    public Set<String> swipedBy(UUID me) {
        try {
            return new HashSet<>(stub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .getSwipedUserIds(GetSwipedUserIdsRequest.newBuilder().setUserId(me.toString()).build())
                    .getUserIdsList());
        } catch (StatusRuntimeException e) {
            log.warn("match-service unavailable ({}), serving unfiltered feed", e.getStatus().getCode());
            return Set.of();
        }
    }
}