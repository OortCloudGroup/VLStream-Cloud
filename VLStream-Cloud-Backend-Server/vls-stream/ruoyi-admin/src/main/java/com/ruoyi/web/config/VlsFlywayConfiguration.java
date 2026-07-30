package com.ruoyi.web.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runs Flyway against the primary dynamic data source.
 *
 * <p>The project uses dynamic-datasource instead of Spring Boot's standard
 * DataSource auto-configuration, so Flyway must be connected explicitly.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VlsFlywayConfiguration {

    private static final String[] FLOWABLE_ENGINE_BEANS = {
        "processEngine",
        "eventRegistryEngine",
        "cmmnEngine",
        "dmnEngine",
        "formEngine",
        "contentEngine",
        "appEngine"
    };

    /**
     * Flowable also manages database tables during startup. Make its engines
     * wait until VLStream's Flyway migration has completed.
     */
    @Bean
    public static BeanFactoryPostProcessor flowableDependsOnVlsFlyway() {
        return beanFactory -> {
            for (String beanName : FLOWABLE_ENGINE_BEANS) {
                if (!beanFactory.containsBeanDefinition(beanName)) {
                    continue;
                }
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                Set<String> dependencies = new LinkedHashSet<>();
                String[] existingDependencies = beanDefinition.getDependsOn();
                if (existingDependencies != null) {
                    dependencies.addAll(Arrays.asList(existingDependencies));
                }
                dependencies.add("vlsFlyway");
                beanDefinition.setDependsOn(dependencies.toArray(new String[0]));
            }
        };
    }

    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway vlsFlyway(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String locations,
            @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:1.1.2}") String baselineVersion,
            @Value("${spring.flyway.baseline-description:VLStream Cloud v1.1.2 baseline}") String baselineDescription,
            @Value("${spring.flyway.validate-on-migrate:true}") boolean validateOnMigrate,
            @Value("${spring.flyway.out-of-order:false}") boolean outOfOrder) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations(locations.split(","))
            .baselineOnMigrate(baselineOnMigrate)
            .baselineVersion(MigrationVersion.fromVersion(baselineVersion))
            .baselineDescription(baselineDescription)
            .validateOnMigrate(validateOnMigrate)
            .outOfOrder(outOfOrder)
            .load();
    }
}
