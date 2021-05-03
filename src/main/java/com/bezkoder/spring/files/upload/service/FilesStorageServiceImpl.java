package com.bezkoder.spring.files.upload.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.LogStream;
import org.mandas.docker.client.builder.jersey.JerseyDockerClientBuilder;
import org.mandas.docker.client.exceptions.DockerCertificateException;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ContainerConfig;
import org.mandas.docker.client.messages.ContainerCreation;
import org.mandas.docker.client.messages.ContainerInfo;
import org.mandas.docker.client.messages.ExecCreation;
import org.mandas.docker.client.messages.HostConfig;
import org.mandas.docker.client.messages.PortBinding;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FilesStorageServiceImpl implements FilesStorageService
{

   private final Path root = Paths.get("uploads");

   @Override
   public void init()
   {
      try
      {
         Files.createDirectory(root);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Could not initialize folder for upload!");
      }
   }

   @Override
   public void save(MultipartFile file)
   {
      try
      {
         Files.copy(file.getInputStream(), this.root.resolve(file.getOriginalFilename()));
      }
      catch (Exception e)
      {
         throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
      }
   }

   @Override
   public Resource load(String filename)
   {
      try
      {
         Path file = root.resolve(filename);
         Resource resource = new UrlResource(file.toUri());

         if (resource.exists() || resource.isReadable())
         {
            return resource;
         }
         else
         {
            throw new RuntimeException("Could not read the file!");
         }
      }
      catch (MalformedURLException e)
      {
         throw new RuntimeException("Error: " + e.getMessage());
      }
   }

   @Override
   public void deleteAll()
   {
      FileSystemUtils.deleteRecursively(root.toFile());
   }

   @Override
   public Stream<Path> loadAll()
   {
      try
      {
         return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Could not load the files!");
      }
   }

   @Override
   public void testDocker() throws InterruptedException, DockerCertificateException, DockerException
   {
      // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
      final DockerClient docker = new JerseyDockerClientBuilder().fromEnv().build(); // For Jersey

      // Pull an image
      docker.pull("busybox");

      // Bind container ports to host ports
      final String[] ports = { "80", "22" };
      final Map<String, List<PortBinding>> portBindings = new HashMap<>();
      for (String port : ports)
      {
         List<PortBinding> hostPorts = new ArrayList<>();
         hostPorts.add(PortBinding.of("0.0.0.0", port));
         portBindings.put(port, hostPorts);
      }

      // Bind container port 443 to an automatically allocated available host port.
      List<PortBinding> randomPort = new ArrayList<>();
      randomPort.add(PortBinding.randomPort("0.0.0.0"));
      portBindings.put("443", randomPort);

      final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

      // Create container with exposed ports
      final ContainerConfig containerConfig = ContainerConfig.builder().hostConfig(hostConfig).image("busybox")
            .exposedPorts(ports).cmd("sh", "-c", "while :; do sleep 1; done").build();

      final ContainerCreation creation = docker.createContainer(containerConfig);
      final String id = creation.id();

      // Inspect container
      final ContainerInfo info = docker.inspectContainer(id);

      // Start container
      docker.startContainer(id);

      // Exec command inside running container with attached STDOUT and STDERR
      final String[] command = { "sh", "-c", "ls" };
      final ExecCreation execCreation = docker.execCreate(id, command, DockerClient.ExecCreateParam.attachStdout(),
            DockerClient.ExecCreateParam.attachStderr());
      final LogStream output = docker.execStart(execCreation.id());
      final String execOutput = output.readFully();

      // Kill container
      docker.killContainer(id);

      // Remove container
      docker.removeContainer(id);

      // Close the docker client
      docker.close();
   }
   
   public void hash() {
      
      
      TraceTool
   }

}
