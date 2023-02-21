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
public class NewAuthDbDataSourceConfig {
    @Bean(name = "newAuthDbDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.newauthdb")
    public DataSource newAuthDbDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "newAuthDbJdbcTemplate")
    @Autowired
    public JdbcTemplate newAuthDbJdbcTemplate(@Qualifier("newAuthDbDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
