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
 * @since 2023-02-22
 */
@Configuration
@EnableTransactionManagement
public class Grs2DataSourceConfig {
    @Bean(name = "grs2DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.grs2")
    public DataSource grs2DataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "grs2JdbcTemplate")
    @Autowired
    public JdbcTemplate grs2JdbcTemplate(@Qualifier("grs2DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
