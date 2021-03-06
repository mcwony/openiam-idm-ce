package org.openiam.webconsole.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Validator;

import org.codehaus.jackson.JsonGenerator.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.openiam.webconsole.web.interceptor.BaseCleanNotificationInterceptor;
import org.openiam.webconsole.web.interceptor.BaseSecurityInterceptor;
import org.openiam.webconsole.web.resolver.UserSessionArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

/**
 * User: Alexander Duckardt<br/>
 * Date: 8/25/12
 */
@Configuration
@EnableWebMvc
@EnableAsync
@ImportResource({ "classpath:applicationContext.xml",
        "classpath:idmservice-Context.xml",
        "classpath:connector-coreContext.xml" })
@ComponentScan(basePackages = { "org.openiam.webconsole.web" })
public class WebConsoleConfig extends WebMvcConfigurerAdapter {
    @Autowired
    private IWebConsoleProperties webConsoleProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations(
                "/resources/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/");
    }

    @Bean
    public ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);
        return mapper;
    }

    @Bean
    public InternalResourceViewResolver jspViewResolver() {
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setPrefix("/WEB-INF/view/");
        bean.setSuffix(".jsp");
        bean.setOrder(1);
        return bean;
    }

    @Bean
    public ViewResolver contentNegotiatingViewResolver() {
        ContentNegotiatingViewResolver viewResolver = new ContentNegotiatingViewResolver();
        viewResolver.setOrder(0);
        Map<String, String> mediaTypes = new HashMap<String, String>();
        mediaTypes.put("json", "application/json");
        viewResolver.setMediaTypes(mediaTypes);
        List<View> defaultViews = new ArrayList<View>();
        defaultViews.add(new MappingJacksonJsonView());
        viewResolver.setDefaultViews(defaultViews);
        return viewResolver;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setDefaultEncoding("UTF-8");
        ms.setCacheSeconds(600);
        ms.setFallbackToSystemLocale(false);
        ms.setBasenames(new String[] { "/WEB-INF/i18n/labels",
                "/WEB-INF/i18n/buttons", "/WEB-INF/i18n/messages",
                "/WEB-INF/i18n/titles" });
        return ms;
    }

    @Bean
    public Validator validator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource());
        return factory;
    }

    @Override
    public void addArgumentResolvers(
            List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new UserSessionArgumentResolver());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * 
     * org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
     * #addInterceptors(org.springframework.web.servlet.config.annotation.
     * InterceptorRegistry)
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration securityInterceptor = registry
                .addInterceptor(new BaseSecurityInterceptor(
                        webConsoleProperties));
        securityInterceptor.addPathPatterns("/secure/**");
        registry.addInterceptor(new BaseCleanNotificationInterceptor());
    }

    @Override
    public void configureMessageConverters(
            List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJacksonHttpMessageConverter());
    }
}
