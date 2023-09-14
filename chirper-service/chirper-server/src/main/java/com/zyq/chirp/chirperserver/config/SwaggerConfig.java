package com.zyq.chirp.chirperserver.config;

/*
@Configuration
@EnableOpenApi
public class SwaggerConfig {
    Boolean swaggerEnabled=true;
    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                // 是否开启swagger
                .enable(swaggerEnabled)
                .select()
                // 过滤条件，扫描指定路径下的文件
                .apis(RequestHandlerSelectors.basePackage("com.zyq.chirp.chirperserver.controller"))
                // 指定路径处理，PathSelectors.any()代表不过滤任何路径
                //.paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        */
/*作者信息*//*

        Contact contact = new Contact("relzx766", "https://github.com/relzx766", "2984109304@qq.com");
        return new ApiInfo(
                "Chirper-UserServer接口",
                "user-server接口文档",
                "v1.0",
                "http://localhost:8081",
                contact,
                "Apache 2.0",
                "http://www.apache.org/licenses/LICENSE-2.0",
                new ArrayList()
        );
    }
}
*/
