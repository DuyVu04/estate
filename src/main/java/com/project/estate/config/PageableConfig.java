package com.project.estate.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageableConfig implements WebMvcConfigurer {

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {

    PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();

    // Client: page=1 -> Spring Pageable: pageNumber=0
    resolver.setOneIndexedParameters(true);

    // Mặc định: 20 phần tử/trang
    resolver.setFallbackPageable(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

    // Không cho size > 100
    resolver.setMaxPageSize(100);

    resolvers.add(resolver);
  }
}
