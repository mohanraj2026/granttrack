package com.granttrack.disbursement.service;

import com.granttrack.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores milestone evidence documents on local disk and returns a relative path persisted on
 * the milestone. Self-contained to finance-service so disbursement file handling owns its own
 * storage root. Mirrors the validation rules of the core document storage for milestone evidence.
 */
@Slf4j
@Service
public class DocumentStorageService {

    /** Documents allowed for milestone evidence. */
    private static final Set<String> DELIVERY_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "csv", "zip", "png", "jpg", "jpeg");
    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    private final Path root;

    public DocumentStorageService(@Value("${granttrack.storage.upload-dir:./uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeMilestoneEvidence(Long milestoneId, MultipartFile file) {
        return store(file, "milestones/" + milestoneId + "/evidence", DELIVERY_EXTENSIONS, MAX_BYTES);
    }

    private String store(MultipartFile file, String relativeDir, Set<String> allowedExtensions, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("No document was provided");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("Document exceeds the size limit");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = StringUtils.getFilenameExtension(original);
        if (ext == null || !allowedExtensions.contains(ext.toLowerCase())) {
            throw new BusinessException("Unsupported file type. Allowed formats: " + allowedExtensions);
        }
        try {
            Path dir = root.resolve(relativeDir);
            Files.createDirectories(dir);
            String stored = UUID.randomUUID() + "." + ext.toLowerCase();
            Path target = dir.resolve(stored);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String relative = relativeDir + "/" + stored;
            log.info("Stored milestone evidence at {}", relative);
            return relative;
        } catch (IOException ex) {
            throw new BusinessException("Failed to store document: " + ex.getMessage());
        }
    }

    /** Load a previously stored evidence document as a downloadable resource. */
    public Resource load(String relativePath) {
        try {
            Path file = root.resolve(relativePath).normalize();
            if (!file.startsWith(root)) {
                throw new BusinessException("Invalid document path");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Document is not available");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BusinessException("Document could not be read");
        }
    }
}
