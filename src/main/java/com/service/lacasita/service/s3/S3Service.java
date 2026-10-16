package com.service.lacasita.service.s3;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final WebpConverter webpConverter;

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    @Value("${aws.s3.bucket}")
    private String bucketName;


    public S3Service(S3Client s3Client, WebpConverter webpConverter) {
        this.s3Client       = s3Client;
        this.webpConverter  = webpConverter;
    }

    /** Sube a la raíz del bucket. Ver {@link #uploadFileToS3(MultipartFile, String)}. */
    public String uploadFileToS3(MultipartFile file) throws IOException {
        return uploadFileToS3(file, null);
    }

    /**
     * Sube una imagen a S3 bajo una carpeta.
     */
    public String uploadFileToS3(MultipartFile file, String carpeta) throws IOException {

        WebpConverter.Resultado convertido = webpConverter.convertir(
                file.getBytes(), file.getOriginalFilename(), file.getContentType());

        String nombre = UUID.randomUUID() + "-" + convertido.nombreArchivo();
        String prefijo = carpeta == null || carpeta.isBlank()
                ? ""
                : carpeta.replaceAll("^/+|/+$", "") + "/";
        String key = prefijo + nombre;

        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(convertido.contentType())
                .build(),
            RequestBody.fromBytes(convertido.datos()));

        return key;
    }

    /**
     * Borra la imagen que quedó huérfana al reemplazarla.
     */
    public void deleteFileFromS3(String key) {

        if (key == null || key.isBlank()) return;

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo borrar de S3 la imagen '{}': {}", key, e.getMessage());
        }
    }
}
