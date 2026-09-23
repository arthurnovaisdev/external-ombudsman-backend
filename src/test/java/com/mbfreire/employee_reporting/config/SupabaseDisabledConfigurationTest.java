package com.mbfreire.employee_reporting.config;

import com.mbfreire.employee_reporting.service.FileStorageService;
import com.mbfreire.employee_reporting.service.SupabaseKeepAliveService;
import com.mbfreire.employee_reporting.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseDisabledConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    SupabaseConfig.class,
                    SupabaseStorageService.class,
                    FileStorageService.class,
                    SupabaseKeepAliveService.class
            );

    @Test
    void doesNotCreateSupabaseOrFileStorageBeansWhenFeatureIsDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean("supabaseRestClient");
            assertThat(context).doesNotHaveBean(RestClient.class);
            assertThat(context).doesNotHaveBean(SupabaseStorageService.class);
            assertThat(context).doesNotHaveBean(FileStorageService.class);
            assertThat(context).doesNotHaveBean(SupabaseKeepAliveService.class);
        });
    }
}
