package com.note.service.config;

//day02
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {  // 类名可以不改，或改为 Knife4jConfig

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Note Service API")
                        .version("v1")
                        .description("笔记服务接口文档")
                        .contact(new Contact()
                                .name("Nysanka")
                                .email("nysanka@example.com")));
    }
}
