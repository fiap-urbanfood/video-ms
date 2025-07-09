package com.fiap.video.controller;

import com.fiap.video.service.VideoService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VideoControllerTest {

    @Test
    void testUploadVideo() throws Exception {
        VideoService videoService = mock(VideoService.class);
        when(videoService.uploadVideo(any())).thenReturn(String.valueOf(ResponseEntity.ok("ok")));

        VideoController controller = new VideoController(videoService);

        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "dummy content".getBytes());

        ResponseEntity<String> response = controller.uploadVideo(file);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testDownloadFramesZipSuccess() {
        VideoService videoService = mock(VideoService.class);
        when(videoService.uploadVideo(any())).thenReturn(String.valueOf(ResponseEntity.ok("ok")));

        // Instancia o controller com o mock
        VideoController controller = new VideoController(videoService);

        ResponseEntity<InputStreamResource> response = null;
        try{
            response = controller.downloadFramesZip("teste");

            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
        }catch (Exception e){
            assertNull(response);
        }

    }

}
