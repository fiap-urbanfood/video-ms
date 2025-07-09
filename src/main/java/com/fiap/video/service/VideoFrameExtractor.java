package com.fiap.video.service;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.File;


public class VideoFrameExtractor {
    static {
        OpenCV.loadLocally();
    }

    public static void extractFrames(String videoPath, String outputDir) {
        VideoCapture cap = new VideoCapture(videoPath);
        if (!cap.isOpened()) {
            System.out.println("Erro ao abrir o vídeo!");
            return;
        }
        Mat frame = new Mat();
        int frameNumber = 0;
        while (cap.read(frame)) {
            File pasta = new File(outputDir);
            pasta.mkdirs();
            String fileName = pasta.getAbsolutePath() + "/frame_" + frameNumber + ".png";
            Imgcodecs.imwrite(fileName, frame);
            frameNumber++;
        }
        cap.release();
        System.out.println("Extração concluída. Total de frames: " + frameNumber);
    }
}