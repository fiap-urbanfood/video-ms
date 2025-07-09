package com.fiap.video.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class VideoServiceTest {

    @Test
    void testDownloadFrameVideo() {
        // Mock do S3Client
        S3Client s3Client = mock(S3Client.class);
        SqsClient sqsClient = mock(SqsClient.class);
        SnsClient snsClient = mock(SnsClient.class);



        // Mock da resposta de listagem de objetos
        S3Object s3Object = S3Object.builder().key("file/frames/frame_1.png").build();
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(List.of(s3Object))
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

        // Mock da resposta de download do objeto
        ByteArrayInputStream fileContent = new ByteArrayInputStream("dummy".getBytes());
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(GetObjectResponse.builder().build(), fileContent);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        VideoService service = new VideoService(sqsClient, s3Client, snsClient);
        ResponseEntity<InputStreamResource> response = null;
        try{
            response = service.downloadFramesZip("teste");

            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response.getBody());
        }catch (Exception e){
            assertEquals(500, response.getStatusCodeValue()); // Status de erro
            assertNull(response.getBody());
        }

    }

}
