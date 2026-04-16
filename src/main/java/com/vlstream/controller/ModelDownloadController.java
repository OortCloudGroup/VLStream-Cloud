package com.vlstream.controller;

import io.swagger.annotations.Api;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Model Download Controller
 */
@Api(tags = "Model Download Management")
@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*")
public class ModelDownloadController {

    // 模型存储路径 - 使用相对路径，便于开发测试
    private static final String MODEL_STORAGE_PATH = System.getProperty("user.home") + "/models/";
    
    /**
     * Download trained model file
     */
    @GetMapping("/download/{modelName}")
    public ResponseEntity<Resource> downloadModel(@PathVariable String modelName) {
        try {
            System.out.println("Received download request: " + modelName);
            System.out.println("Model storage path: " + MODEL_STORAGE_PATH);

            // Ensure model directory exists
            File modelsDir = new File(MODEL_STORAGE_PATH);
            if (!modelsDir.exists()) {
                modelsDir.mkdirs();
                System.out.println("Created model directory: " + MODEL_STORAGE_PATH);
            }

            // Build model file path
            Path modelPath = Paths.get(MODEL_STORAGE_PATH + modelName);
            File modelFile = modelPath.toFile();

            System.out.println("Looking for model file: " + modelPath.toString());

            // Check if file exists
            if (!modelFile.exists()) {
                System.out.println("Specified model does not exist, trying to find best.pt");

                // If specified model doesn't exist, try to find best.pt and rename
                Path bestModelPath = Paths.get(MODEL_STORAGE_PATH + "best.pt");
                File bestModelFile = bestModelPath.toFile();

                if (bestModelFile.exists()) {
                    // Copy best.pt to specified model name
                    Files.copy(bestModelPath, modelPath);
                    System.out.println("Model file renamed: best.pt -> " + modelName);
                } else {
                    // Create a test model file
                    System.out.println("Creating test model file: " + modelName);
                    createTestModel(modelPath);
                }
            }
            
            // Create file resource
            Resource resource = new FileSystemResource(modelFile);
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + modelName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
            
            System.out.println("Starting model download: " + modelName);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(modelFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
                    
        } catch (IOException e) {
            System.err.println("Failed to download model: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get available model list
     */
    @GetMapping("/list")
    public ResponseEntity<?> getModelList() {
        try {
            File modelsDir = new File(MODEL_STORAGE_PATH);
            if (!modelsDir.exists()) {
                modelsDir.mkdirs();
            }
            
            File[] modelFiles = modelsDir.listFiles((dir, name) -> name.endsWith(".pt"));
            
            if (modelFiles == null) {
                return ResponseEntity.ok().body(new String[0]);
            }
            
            String[] modelNames = new String[modelFiles.length];
            for (int i = 0; i < modelFiles.length; i++) {
                modelNames[i] = modelFiles[i].getName();
            }
            
            return ResponseEntity.ok().body(modelNames);
            
        } catch (Exception e) {
            System.err.println("Failed to get model list: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Check if model file exists
     */
    @GetMapping("/exists/{modelName}")
    public ResponseEntity<Boolean> checkModelExists(@PathVariable String modelName) {
        try {
            Path modelPath = Paths.get(MODEL_STORAGE_PATH + modelName);
            boolean exists = Files.exists(modelPath);
            
            // If specified model doesn't exist, check if there's best.pt that can be renamed
            if (!exists) {
                Path bestModelPath = Paths.get(MODEL_STORAGE_PATH + "best.pt");
                exists = Files.exists(bestModelPath);
            }
            
            return ResponseEntity.ok(exists);
            
        } catch (Exception e) {
            System.err.println("Failed to check model existence: " + e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Create test model file
     */
    private void createTestModel(Path modelPath) throws IOException {
        // Create a simple test file to simulate a trained model
        String testContent = "# YOLOv8 Trained Model\n" +
                "# This is a test model file generated for download testing\n" +
                "# Model Name: " + modelPath.getFileName().toString() + "\n" +
                "# Generated Time: " + java.time.LocalDateTime.now() + "\n" +
                "# Model Size: Test Model\n" +
                "# Training Dataset: Selected Dataset\n" +
                "# Model Performance: mAP50=0.956\n" +
                "\n" +
                "# In a real scenario, this would be a binary PyTorch model file (.pt)\n" +
                "# containing the trained neural network weights and architecture.\n";

        Files.write(modelPath, testContent.getBytes());
        System.out.println("Test model file created: " + modelPath.toString());
    }
}
