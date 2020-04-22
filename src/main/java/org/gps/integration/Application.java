package org.gps.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.tb.core.configuration.CoreConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

@SpringBootApplication
@Import({CoreConfiguration.class})
public class Application {
	private final static Logger Log = Logger.getLogger(Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean(name = "objectMapper")
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean("tprExecutorService")
	public ScheduledExecutorService tprExecutorService() {
		return Executors.newSingleThreadScheduledExecutor();
	}

	@Bean("scheduledExecutorService")
    public ScheduledExecutorService scheduledExecutorService() {
    	return Executors.newScheduledThreadPool(1);
    }
	
	@Bean("customRestTemplate")
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		int timeoutMilliseconds = 60 * 1000;
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setConnectionRequestTimeout(timeoutMilliseconds);
		factory.setReadTimeout(timeoutMilliseconds);
		factory.setConnectTimeout(timeoutMilliseconds);
		List<ClientHttpRequestInterceptor> interceptorList = new ArrayList<>();
		InterceptingClientHttpRequestFactory interceptorFactory = new InterceptingClientHttpRequestFactory(new BufferingClientHttpRequestFactory(factory), interceptorList);
		restTemplate.setRequestFactory(interceptorFactory);
		//restTemplate.setErrorHandler(new RestTemplateErrorHandler());
		//restTemplate.setInterceptors(Arrays.asList(new RestTemplateInterceptor()));
		return restTemplate;
	}

}
