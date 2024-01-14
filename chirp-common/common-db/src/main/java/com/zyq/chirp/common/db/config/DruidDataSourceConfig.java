package com.zyq.chirp.common.db.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableAutoConfiguration
@Configuration
public class DruidDataSourceConfig {

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    @RefreshScope
    public DruidDataSource dataSource() {
        return new DruidDataSourceWrapper();

    }
  /*  @Bean
    @ConfigurationProperties("spring.datasource.master")
    public DruidDataSource masterDataSource(){
       return DruidDataSourceBuilder.create().build();
    }
    @Resource
    SlavesProperties slavesProperties;
    @Bean
    public List<DruidDataSource>slaveDataSource(){
      return   slavesProperties.getDataSources().stream()
                .map(properties -> {
                    DruidDataSource dataSource = new DruidDataSource();
                    dataSource.setUrl(properties.getUrl());
                    dataSource.setDriverClassName(properties.getDriverClassName());
                    dataSource.setUsername(properties.getUsername());
                    dataSource.setPassword(properties.getPassword());
                    //添加到db context
                    DBContext.addSlave();
                    return dataSource;
                }).toList();
    }
    @Bean
    @Primary
    public DataSource myRoutingDataSource(){
        HashMap<Object, Object> dataSourceMap = new HashMap<>();
        DruidDataSource masterDataSource = masterDataSource();
        dataSourceMap.put(DBContext.MASTER, masterDataSource);
        List<DruidDataSource> druidDataSources = slaveDataSource();
        for (int i = 0; i < druidDataSources.size(); i++) {
            dataSourceMap.put(DBContext.getSlave(i),druidDataSources.get(i));
        }
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(dataSourceMap);
        return routingDataSource;
    }*/
}
