package com.softlaunch.user.photo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
class S3Config {

    @Bean(destroyMethod = "close")
    S3Client s3Client(PhotoProperties props) {
        var builder = S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                // path-style URLs (http://host/bucket/key): needed by RustFS/MinIO-style servers, fine on AWS too
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        if (hasText(props.endpoint())) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    /** Signs links with the PUBLIC address: the phone, not user-service, will use them. */
    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner(PhotoProperties props) {
        var builder = S3Presigner.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        if (hasText(props.publicEndpoint())) {
            builder.endpointOverride(URI.create(props.publicEndpoint()));
        }
        return builder.build();
    }

    private static AwsCredentialsProvider credentials(PhotoProperties props) {
        return hasText(props.accessKey())
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey(), props.secretKey()))
                : DefaultCredentialsProvider.builder().build();   // AWS: IAM role of the server/container
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
