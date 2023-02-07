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
public class UniRs1DataSourceConfig {
    @Bean(name = "uniRs1DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.unirs1")
    public DataSource uniRs1DataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "uniRs1JdbcTemplate")
    @Autowired
    public JdbcTemplate uniRs1JdbcTemplate(@Qualifier("uniRs1DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
