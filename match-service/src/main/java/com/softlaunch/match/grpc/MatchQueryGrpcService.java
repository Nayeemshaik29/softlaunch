package com.softlaunch.match.grpc;

import com.softlaunch.grpc.match.v1.GetSwipedUserIdsRequest;
import com.softlaunch.grpc.match.v1.GetSwipedUserIdsResponse;
import com.softlaunch.grpc.match.v1.MatchQueryServiceGrpc;
import com.softlaunch.match.repository.SwipeRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MatchQueryGrpcService extends MatchQueryServiceGrpc.MatchQueryServiceImplBase {

    private final SwipeRepository swipeRepository;

    public MatchQueryGrpcService(SwipeRepository swipeRepository) {
        this.swipeRepository = swipeRepository;
    }

    @Override
    public void getSwipedUserIds(GetSwipedUserIdsRequest request,
                                 StreamObserver<GetSwipedUserIdsResponse> responseObserver) {
        UUID userId;
        try {
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("user_id must be a UUID")
                    .asRuntimeException());
            return;
        }

        List<String> swiped = swipeRepository.findTargetIdsBySwiperId(userId).stream()
                .map(UUID::toString)
                .toList();

        responseObserver.onNext(GetSwipedUserIdsResponse.newBuilder().addAllUserIds(swiped).build());
        responseObserver.onCompleted();
    }
}