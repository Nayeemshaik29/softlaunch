package com.softlaunch.user.photo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param endpoint       where user-service reaches S3 (inside Docker: http://rustfs:9000; on AWS: leave empty)
 * @param publicEndpoint what goes INTO signed links, so it must work from a phone/browser (http://localhost:9000 locally)
 * @param accessKey      empty in production: the AWS SDK then uses the server's IAM role (no keys in config at all)
 * @param createBucket   local dev only: create the bucket at startup
 */
@ConfigurationProperties(prefix = "softlaunch.photos")
public record PhotoProperties(String bucket, String region, String endpoint, String publicEndpoint,
                              String accessKey, String secretKey, boolean createBucket,
                              long maxUploadBytes, long maxPixels, int maxDimension,
                              Duration uploadLinkTtl, Duration viewLinkTtl, Duration pendingTtl) {
}
