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
public class Rcs2DataSourceConfig {
    @Bean(name = "rcs2DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.rcs2")
    public DataSource rcs2DataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "rcs2JdbcTemplate")
    @Autowired
    public JdbcTemplate rcs2JdbcTemplate(@Qualifier("rcs2DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
