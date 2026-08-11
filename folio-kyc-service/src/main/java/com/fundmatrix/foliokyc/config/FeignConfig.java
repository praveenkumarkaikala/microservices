package com.fundmatrix.foliokyc.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
    	
    	return new RequestInterceptor() {
			
			@Override
			public void apply(RequestTemplate requestTemplate) {
				 ServletRequestAttributes attrs =
		                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		            if (attrs != null) {
		                String header = attrs.getRequest().getHeader("Authorization");
		                if (header != null) {
		                    requestTemplate.header("Authorization", header);
		                }
				
			}
		}
       
    };
}
} 
