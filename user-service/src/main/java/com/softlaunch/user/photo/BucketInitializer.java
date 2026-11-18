package com.softlaunch.user.photo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * LOCAL DEV ONLY (softlaunch.photos.create-bucket=true). In production the bucket is created once with
 * infrastructure tooling (Terraform/console), with "block all public access" ON.
 */
@Component
@ConditionalOnProperty(name = "softlaunch.photos.create-bucket", havingValue = "true")
class BucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BucketInitializer.class);

    private final S3Client s3;
    private final PhotoProperties props;

    BucketInitializer(S3Client s3, PhotoProperties props) {
        this.s3 = s3;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            s3.headBucket(r -> r.bucket(props.bucket()));
        } catch (NoSuchBucketException e) {
            s3.createBucket(r -> r.bucket(props.bucket()));   // new buckets are private by default
            log.info("Created bucket {}", props.bucket());
        } catch (S3Exception e) {
            if (e.statusCode() != 404) throw e;
            s3.createBucket(r -> r.bucket(props.bucket()));
            log.info("Created bucket {}", props.bucket());
        }
        // Belt and braces next to our own cleanup job: the storage itself deletes raw uploads after 1 day.
        try {
            s3.putBucketLifecycleConfiguration(r -> r.bucket(props.bucket()).lifecycleConfiguration(c -> c.rules(
                    LifecycleRule.builder().id("delete-abandoned-uploads").status(ExpirationStatus.ENABLED)
                            .filter(LifecycleRuleFilter.builder().prefix("pending/").build())
                            .expiration(x -> x.days(1)).build())));
        } catch (S3Exception e) {
            log.warn("Storage doesn't support lifecycle rules ({}); the cleanup job still removes abandoned uploads",
                    e.awsErrorDetails() == null ? e.getMessage() : e.awsErrorDetails().errorCode());
        }
    }
}
