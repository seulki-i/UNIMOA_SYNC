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
 * @since 2023-02-06
 */
@Configuration
@EnableTransactionManagement
public class Rcs1DataSourceConfig {
    @Bean(name = "rcs1DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.rcs1")
    public DataSource rcs1DataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "rcs1JdbcTemplate")
    @Autowired
    public JdbcTemplate rcs1JdbcTemplate(@Qualifier("rcs1DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
