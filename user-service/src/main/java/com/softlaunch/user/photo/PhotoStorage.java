package com.softlaunch.user.photo;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** The only class that talks to S3. Bucket is PRIVATE: everything goes through signed, expiring links. */
@Component
class PhotoStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    PhotoStorage(S3Client s3, S3Presigner presigner, PhotoProperties props) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = props.bucket();
    }

    /**
     * Link for ONE upload: only this key, only this content type, only this exact size, only until expiry.
     * Content-Type and Content-Length are part of the signature: S3 rejects an upload that differs.
     */
    URL uploadLink(String key, String contentType, long contentLength, Duration ttl) {
        return presigner.presignPutObject(p -> p.signatureDuration(ttl).putObjectRequest(r -> r
                .bucket(bucket).key(key).contentType(contentType).contentLength(contentLength))).url();
    }

    URL viewLink(String key, Duration ttl) {
        return presigner.presignGetObject(p -> p.signatureDuration(ttl).getObjectRequest(r -> r.bucket(bucket).key(key))).url();
    }

    Optional<HeadObjectResponse> head(String key) {
        try {
            return Optional.of(s3.headObject(r -> r.bucket(bucket).key(key)));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return Optional.empty();
            throw e;
        }
    }

    byte[] read(String key) {
        return s3.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asByteArray();
    }

    void writeJpeg(String key, byte[] jpeg) {
        s3.putObject(r -> r.bucket(bucket).key(key).contentType("image/jpeg").cacheControl("private, max-age=3600"),
                RequestBody.fromBytes(jpeg));
    }

    void delete(String key) {
        s3.deleteObject(r -> r.bucket(bucket).key(key));   // deleting a missing key is not an error in S3
    }

    /** Everything stored for a user (account deletion). */
    int deleteAllOf(String userId) {
        int deleted = 0;
        for (String prefix : List.of("photos/" + userId + "/", "pending/" + userId + "/")) {
            List<ObjectIdentifier> keys = s3.listObjectsV2Paginator(r -> r.bucket(bucket).prefix(prefix)).contents().stream()
                    .map(o -> ObjectIdentifier.builder().key(o.key()).build()).toList();
            if (!keys.isEmpty()) {
                s3.deleteObjects(r -> r.bucket(bucket).delete(Delete.builder().objects(keys).build()));
                deleted += keys.size();
            }
        }
        return deleted;
    }
}
