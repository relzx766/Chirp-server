package com.zyq.chirp.common.db.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MPConfig {
    /*   @Resource
        DataSource myRoutingDataSource;
        @Bean
        public SqlSessionFactory sqlSessionFactory() throws Exception {
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(myRoutingDataSource);
            factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResource("classpath:mapper/*.xml"));
            return factoryBean.getObject();
        }
        @Bean
        public PlatformTransactionManager platformTransactionManager(){
            return new DataSourceTransactionManager(myRoutingDataSource);
        }*/
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
