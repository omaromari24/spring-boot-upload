package com.bezkoder.spring.files.upload.service;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.mandas.docker.client.exceptions.DockerCertificateException;
import org.mandas.docker.client.exceptions.DockerException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FilesStorageService {
  public void init();

  public void save(MultipartFile file);

  public Resource load(String filename);

  public void deleteAll();

  public Stream<Path> loadAll();
  
  public void testDocker() throws DockerCertificateException, DockerException, InterruptedException;

}
