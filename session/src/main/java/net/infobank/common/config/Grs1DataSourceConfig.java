package net.infobank.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * @author skkim
 * @since 2023-02-17
 */
@Configuration
@EnableTransactionManagement
public class Grs1DataSourceConfig {
    @Bean(name = "grs1DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.grs1")
    public DataSource grs1DataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "grs1JdbcTemplate")
    @Autowired
    public JdbcTemplate grs1JdbcTemplate(@Qualifier("grs1DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
