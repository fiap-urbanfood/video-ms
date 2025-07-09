package com.fiap.video.controller;

import com.fiap.video.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/video")
public class VideoController {


    private final VideoService videoService;

    @Autowired
    public VideoController(VideoService videoService) {

        this.videoService = videoService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        String retunString = videoService.uploadVideo(file);
        return ResponseEntity.ok(retunString);

    }


    @GetMapping("/download/{filename}")
    public ResponseEntity<InputStreamResource> downloadFramesZip(@PathVariable String filename) throws IOException {
        return videoService.downloadFramesZip(filename);
    }


}