package com.photoday.photoday.image.service;

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface S3Service {
    String saveImage(MultipartFile multipartFile) throws IOException, NoSuchAlgorithmException;

    String getMd5Hash(MultipartFile file) throws IOException, NoSuchAlgorithmException;

    byte[] downloadImage(String imagePath) throws IOException;

    HttpHeaders buildHeaders(String resourcePath, byte[] data);
}
