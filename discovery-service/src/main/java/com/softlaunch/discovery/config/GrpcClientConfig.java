package com.softlaunch.discovery.config;

import com.softlaunch.grpc.match.v1.MatchQueryServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    public MatchQueryServiceGrpc.MatchQueryServiceBlockingStub matchQueryStub(GrpcChannelFactory channels) {
        return MatchQueryServiceGrpc.newBlockingStub(channels.createChannel("match-service"));
    }
}