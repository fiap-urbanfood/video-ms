package com.fiap.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.video.controller.VideoController;
import com.fiap.video.entity.FileStatusDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class VideoService {

    private final SqsClient sqsClient;
    private final String queueUrl = "https://sqs.us-east-1.amazonaws.com/857378965163/status-fiap"; // Substitua pela sua URL
    private final String bucketName = "fiap-frame";
    private final S3Client s3Client;

    private final SnsClient snsClient;
    private final String snsTopicArn = "arn:aws:sns:us-east-1:857378965163:notification-fiap-email"; // Substitua pelo seu ARN

    @Autowired
    public VideoService(SqsClient sqsClient, S3Client s3Client, SnsClient snsClient) {
        this.sqsClient = sqsClient;
        this.s3Client = s3Client;
        this.snsClient = snsClient;
    }

    public String uploadVideo(MultipartFile file){
        if (file.isEmpty()) {
            return "No file selected";
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        // Salva o arquivo na raiz do projeto, pasta 'uploads'
        String uploadDirPathBase = VideoController.class.getClassLoader().getResource("").getPath();
        String uploadDirPath =  uploadDirPathBase + File.separator + "uploads";
        File uploadDir = new File(uploadDirPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();
        File dest = new File(uploadDir, fileName);
        try {
            file.transferTo(dest);
            VideoFrameExtractor.extractFrames(dest.getAbsolutePath(), uploadDirPathBase + "frame");

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key("file/video/"+fileName)
                    .build();
            s3Client.putObject(putObjectRequest, Paths.get(dest.getAbsolutePath()));

            uploadFolderToS3(bucketName, "file/video/frame");

            FileStatusDTO statusDTO = new FileStatusDTO(fileName, "SUCESSO");

            ObjectMapper mapper = new ObjectMapper();
            String statusDTOJson = mapper.writeValueAsString(statusDTO);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(statusDTOJson)
                    .build();
            sqsClient.sendMessage(sendMsgRequest);

            return "Video uploaded successfully: " + dest.getAbsolutePath();
        } catch (IOException e) {

            String errorMsg = "Erro ao processar vídeo upload: " + e.getMessage();
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .message(errorMsg)
                    .build();
            snsClient.publish(request);

            return "Error saving file: " + e.getMessage();
        }

    }

    public void uploadFolderToS3(String bucketName, String s3Prefix) {
        ClassLoader classLoader = getClass().getClassLoader();
        File folder = new File(classLoader.getResource("frame").getFile());

        try {
            Files.list(Paths.get(folder.getAbsolutePath()))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String s3Key = s3Prefix + "/" + fileName; // Exemplo: "file/frame_0.png"
                        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(s3Key)
                                .build();
                        s3Client.putObject(putObjectRequest, path);
                        System.out.println("Enviado para S3: " + s3Key);
                    });
        } catch (IOException e) {
            String errorMsg = "Erro ao processar vídeo S3: " + e.getMessage();
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .message(errorMsg)
                    .build();
            snsClient.publish(request);

            e.printStackTrace();
        }
    }

    public ResponseEntity<InputStreamResource> downloadFramesZip(String filename) throws IOException {

        String framesPrefix = "file/"+filename+"/frame";
        try {
            // Lista todos os arquivos na pasta frames do S3
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(framesPrefix)
                    .build();
            List<S3Object> objects = s3Client.listObjectsV2(listReq).contents();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (S3Object obj : objects) {
                    String key = obj.key();
                    String fileName = key.substring(framesPrefix.length());
                    if (fileName.isEmpty()) continue; // ignora o diretório

                    // Baixa o arquivo do S3
                    GetObjectRequest getReq = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();
                    try (InputStream is = s3Client.getObject(getReq)) {
                        zos.putNextEntry(new ZipEntry(fileName));
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    }
                }
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"frames.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bais));

        } catch (Exception e) {
            String errorMsg = "Erro ao processar vídeo download: " + e.getMessage();
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .message(errorMsg)
                    .build();
            snsClient.publish(request);

            return ResponseEntity.status(500).build();
        }
    }

}
