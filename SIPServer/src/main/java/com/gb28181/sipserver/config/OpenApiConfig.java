package com.gb28181.sipserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sipServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GB28181 SIP Server API")
                        .description("GB28181 SIP信令服务器接口文档，提供设备管理、流媒体控制等功能。")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SIP Server Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")));
    }
}
