package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "attachments.enabled", havingValue = "true")
@Slf4j
public class FileStorageService {

    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 200;

    private final long maxFileSizeBytes;

    private final SupabaseStorageService supabaseStorageService;

    public FileStorageService(
            @Value("${spring.servlet.multipart.max-file-size:10MB}")
            String maxFileSize,

            SupabaseStorageService supabaseStorageService
    ) {

        this.maxFileSizeBytes =
                DataSize
                        .parse(maxFileSize)
                        .toBytes();

        this.supabaseStorageService =
                supabaseStorageService;
    }

    public StoredFile storeFile(
            MultipartFile file
    ) {

        validateBasicFile(file);

        DetectedFileType detectedType =
                detectFileType(file);

        String originalFileName =
                sanitizeOriginalFileName(
                        file.getOriginalFilename()
                );

        validateOriginalExtension(
                originalFileName,
                detectedType
        );

        String storedFileName =
                UUID.randomUUID()
                        + "."
                        + detectedType.canonicalExtension;

        byte[] content;

        try {

            content =
                    file.getBytes();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Não foi possível ler o arquivo enviado.",
                    e
            );
        }

        supabaseStorageService.upload(
                storedFileName,
                content,
                detectedType.contentType
        );

        return new StoredFile(
                storedFileName,
                originalFileName,
                detectedType.contentType,
                file.getSize()
        );
    }

    public Resource loadFile(
            String storedFileName
    ) {

        validateStoredFileName(
                storedFileName
        );

        byte[] content =
                supabaseStorageService.download(
                        storedFileName
                );

        return new ByteArrayResource(
                content
        );
    }

    public boolean deleteFile(
            String storedFileName
    ) {

        try {

            validateStoredFileName(
                    storedFileName
            );

            return supabaseStorageService.delete(
                    storedFileName
            );

        } catch (RuntimeException e) {

            log.warn(
                    "Não foi possível remover um arquivo do armazenamento."
            );

            return false;
        }
    }

    private void validateBasicFile(
            MultipartFile file
    ) {

        if (file == null
                || file.isEmpty()) {

            throw new BusinessRuleException(
                    "Não é possível enviar um arquivo vazio."
            );
        }

        if (file.getSize()
                > maxFileSizeBytes) {

            throw new BusinessRuleException(
                    "O arquivo excede o tamanho máximo permitido."
            );
        }
    }

    private DetectedFileType detectFileType(
            MultipartFile file
    ) {

        byte[] header;

        try (
                InputStream inputStream =
                        file.getInputStream()
        ) {

            header =
                    inputStream.readNBytes(8);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Não foi possível analisar o arquivo enviado.",
                    e
            );
        }

        if (startsWith(
                header,
                new byte[]{
                        0x25,
                        0x50,
                        0x44,
                        0x46,
                        0x2D
                }
        )) {

            return DetectedFileType.PDF;
        }

        if (startsWith(
                header,
                new byte[]{
                        (byte) 0x89,
                        0x50,
                        0x4E,
                        0x47,
                        0x0D,
                        0x0A,
                        0x1A,
                        0x0A
                }
        )) {

            return DetectedFileType.PNG;
        }

        if (startsWith(
                header,
                new byte[]{
                        (byte) 0xFF,
                        (byte) 0xD8,
                        (byte) 0xFF
                }
        )) {

            return DetectedFileType.JPEG;
        }

        throw new BusinessRuleException(
                "Tipo de arquivo não permitido. Apenas PDF, JPEG e PNG são aceitos."
        );
    }

    private boolean startsWith(
            byte[] source,
            byte[] signature
    ) {

        if (source.length
                < signature.length) {

            return false;
        }

        for (int i = 0;
             i < signature.length;
             i++) {

            if (source[i]
                    != signature[i]) {

                return false;
            }
        }

        return true;
    }

    private String sanitizeOriginalFileName(
            String originalFileName
    ) {

        String fileName =
                originalFileName;

        if (fileName == null
                || fileName.isBlank()) {

            fileName = "arquivo";
        }

        fileName =
                StringUtils.cleanPath(
                        fileName
                );

        fileName =
                fileName.replace(
                        '\\',
                        '/'
                );

        int lastSlash =
                fileName.lastIndexOf('/');

        if (lastSlash >= 0) {

            fileName =
                    fileName.substring(
                            lastSlash + 1
                    );
        }

        fileName =
                fileName.replaceAll(
                        "\\p{Cntrl}",
                        "_"
                );

        fileName =
                fileName.trim();

        if (fileName.isBlank()
                || fileName.equals(".")
                || fileName.equals("..")) {

            fileName = "arquivo";
        }

        return limitFileNameLength(
                fileName
        );
    }

    private String limitFileNameLength(
            String fileName
    ) {

        if (fileName.length()
                <= MAX_ORIGINAL_FILENAME_LENGTH) {

            return fileName;
        }

        String extension =
                StringUtils.getFilenameExtension(
                        fileName
                );

        if (extension == null
                || extension.isBlank()) {

            return fileName.substring(
                    0,
                    MAX_ORIGINAL_FILENAME_LENGTH
            );
        }

        String suffix =
                "."
                        + extension;

        int maxBaseLength =
                MAX_ORIGINAL_FILENAME_LENGTH
                        - suffix.length();

        if (maxBaseLength <= 0) {

            return fileName.substring(
                    0,
                    MAX_ORIGINAL_FILENAME_LENGTH
            );
        }

        String baseName =
                fileName.substring(
                        0,
                        fileName.length()
                                - suffix.length()
                );

        return baseName.substring(
                0,
                Math.min(
                        baseName.length(),
                        maxBaseLength
                )
        ) + suffix;
    }

    private void validateOriginalExtension(
            String originalFileName,
            DetectedFileType detectedType
    ) {

        String extension =
                StringUtils.getFilenameExtension(
                        originalFileName
                );

        if (extension == null
                || extension.isBlank()) {

            return;
        }

        String normalizedExtension =
                extension.toLowerCase(
                        Locale.ROOT
                );

        if (!detectedType
                .allowedExtensions
                .contains(normalizedExtension)) {

            throw new BusinessRuleException(
                    "A extensão do arquivo não corresponde ao conteúdo enviado."
            );
        }
    }

    private void validateStoredFileName(
            String storedFileName
    ) {

        if (storedFileName == null
                || storedFileName.isBlank()) {

            throw new IllegalStateException(
                    "Nome de arquivo armazenado inválido."
            );
        }

        if (!storedFileName.matches(
                "^[0-9a-fA-F-]{36}\\.(pdf|jpg|png)$"
        )) {

            throw new IllegalStateException(
                    "Nome de arquivo armazenado inválido."
            );
        }
    }

    public record StoredFile(
            String storedFileName,
            String originalFileName,
            String contentType,
            long fileSize
    ) {
    }

    private enum DetectedFileType {

        PDF(
                "application/pdf",
                "pdf",
                Set.of("pdf")
        ),

        JPEG(
                "image/jpeg",
                "jpg",
                Set.of(
                        "jpg",
                        "jpeg",
                        "jfif"
                )
        ),

        PNG(
                "image/png",
                "png",
                Set.of("png")
        );

        private final String contentType;

        private final String canonicalExtension;

        private final Set<String> allowedExtensions;

        DetectedFileType(
                String contentType,
                String canonicalExtension,
                Set<String> allowedExtensions
        ) {

            this.contentType =
                    contentType;

            this.canonicalExtension =
                    canonicalExtension;

            this.allowedExtensions =
                    allowedExtensions;
        }
    }
}
