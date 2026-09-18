package com.mbfreire.employee_reporting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SupabaseKeepAliveService {

    private static final Logger logger =
            LoggerFactory.getLogger(SupabaseKeepAliveService.class);

    private final RestClient restClient;
    private final String bucketName;

    public SupabaseKeepAliveService(
            RestClient.Builder restClientBuilder,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.secret-key}") String secretKey,
            @Value("${supabase.storage.bucket}") String bucketName
    ) {
        this.bucketName = bucketName;

        this.restClient = restClientBuilder
                .baseUrl(supabaseUrl)
                .defaultHeader("apikey", secretKey)
                .build();
    }

    @Scheduled(
            cron = "0 0 4 * * MON,THU",
            zone = "America/Bahia"
    )
    public void keepAlive() {

        try {
            restClient.get()
                    .uri("/storage/v1/bucket/{bucket}", bucketName)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Supabase keep-alive executado com sucesso.");

        } catch (Exception exception) {
            logger.error(
                    "Falha ao executar Supabase keep-alive: {}",
                    exception.getMessage()
            );
        }
    }
}
