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
 * @since 2023-02-09
 */
@Configuration
@EnableTransactionManagement
public class Unirs2DataSourceConfig {
    @Bean(name = "unirs2DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.unirs2")
    public DataSource unirs2DataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "unirs2JdbcTemplate")
    @Autowired
    public JdbcTemplate unirs2JdbcTemplate(@Qualifier("unirs2DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
