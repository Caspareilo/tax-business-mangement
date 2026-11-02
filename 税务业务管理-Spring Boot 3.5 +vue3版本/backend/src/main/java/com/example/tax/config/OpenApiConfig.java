package com.example.tax.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taxOpenApi() {
        return new OpenAPI().info(new Info()
                .title("税务业务管理系统 API")
                .version("v1")
                .description("教学模拟系统，不连接真实税务机关，不提交真实申报。"));
    }
}
