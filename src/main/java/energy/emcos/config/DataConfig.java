package energy.emcos.config;

/**
 * Конфигурационный класс для описания конфигурации источника данных
 * для доступа к базе данных модуля reports
 */

import energy.emcos.model.entity.ParamAt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
    basePackages = "energy.emcos.model.repo",
    entityManagerFactoryRef = "emcosEntityManager",
    transactionManagerRef = "emcosTransactionManager"
)
public class DataConfig {

    @Primary
    @Bean(name = "emcosDataSource")
    @ConfigurationProperties(prefix="spring.datasource")
    public DataSource emcosDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "emcosEntityManager")
    public LocalContainerEntityManagerFactoryBean emcosEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("emcosDataSource") DataSource emcosDataSource) {
        return builder
            .dataSource(emcosDataSource)
            .packages(ParamAt.class, Jsr310JpaConverters.class)
            .persistenceUnit("emcos")
            .build();
    }

    @Primary
    @Bean(name = "emcosTransactionManager")
    public PlatformTransactionManager emcosTransactionManager(@Qualifier("emcosEntityManager") EntityManagerFactory emcosEntityManagerFactory) {
        return new JpaTransactionManager(emcosEntityManagerFactory);
    }
}
