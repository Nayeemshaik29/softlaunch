package com.softlaunch.user.photo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    public record UploadRequest(@NotBlank String contentType, @Positive long sizeBytes) {
    }

    /** Step 1: "I want to upload a JPEG of N bytes" -> a signed link, valid a few minutes. */
    @PostMapping("/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoService.UploadTicket uploadUrl(@RequestHeader("X-User-Id") UUID me, @Valid @RequestBody UploadRequest request) {
        return photoService.uploadLink(me, request.contentType(), request.sizeBytes());
    }

    /** Step 3: "done uploading" -> checked, cleaned, resized, added to the profile. */
    @PostMapping("/{photoId}/confirm")
    public PhotoService.PhotoView confirm(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID photoId) {
        return photoService.confirm(me, photoId);
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID photoId) {
        photoService.delete(me, photoId);
    }
}
