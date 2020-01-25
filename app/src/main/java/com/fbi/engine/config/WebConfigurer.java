package com.fbi.engine.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import io.github.jhipster.config.JHipsterConstants;
import io.github.jhipster.config.JHipsterProperties;
import io.github.jhipster.web.filter.CachingHttpHeadersFilter;
import io.undertow.UndertowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebConfigurer implements ServletContextInitializer, WebServerFactoryCustomizer<WebServerFactory> {

	private final Environment env;

	private final JHipsterProperties jHipsterProperties;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		if (env.getActiveProfiles().length != 0) {
			log.info("Web application configuration, using profiles: {}", (Object[]) env.getActiveProfiles());
		}
		EnumSet<DispatcherType> disps = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD,
				DispatcherType.ASYNC);

		if (env.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_PRODUCTION))) {
			initCachingHttpHeadersFilter(servletContext, disps);
		}
		log.info("Web application fully configured");
	}

	/**
	 * Resolve path prefix to static resources.
	 */
	private String resolvePathPrefix() {
		String fullExecutablePath = this.getClass().getResource("").getPath();
		String rootPath = Paths.get(".").toUri().normalize().getPath();
		String extractedPath = fullExecutablePath.replace(rootPath, "");
		int extractionEndIndex = extractedPath.indexOf("target/");
		if (extractionEndIndex <= 0) {
			return "";
		}
		return extractedPath.substring(0, extractionEndIndex);
	}

	/**
	 * Initializes the caching HTTP Headers Filter.
	 */
	private void initCachingHttpHeadersFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {
		log.debug("Registering Caching HTTP Headers Filter");
		FilterRegistration.Dynamic cachingHttpHeadersFilter = servletContext.addFilter("cachingHttpHeadersFilter",
				new CachingHttpHeadersFilter(jHipsterProperties));

		cachingHttpHeadersFilter.addMappingForUrlPatterns(disps, true, "/content/*");
		cachingHttpHeadersFilter.addMappingForUrlPatterns(disps, true, "/app/*");
		cachingHttpHeadersFilter.setAsyncSupported(true);
	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = jHipsterProperties.getCors();
		if (config.getAllowedOrigins() != null && !config.getAllowedOrigins().isEmpty()) {
			log.debug("Registering CORS filter");
			source.registerCorsConfiguration("/api/**", config);
			source.registerCorsConfiguration("/v2/api-docs", config);
		}
		return new CorsFilter(source);
	}

	private void setMimeMappings(WebServerFactory server) {
		if (server instanceof ConfigurableServletWebServerFactory) {
			MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
			// IE issue, see https://github.com/jhipster/generator-jhipster/pull/711
			mappings.add("html", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
			// CloudFoundry issue, see https://github.com/cloudfoundry/gorouter/issues/64
			mappings.add("json", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
			ConfigurableServletWebServerFactory servletWebServer = (ConfigurableServletWebServerFactory) server;
			servletWebServer.setMimeMappings(mappings);
		}
	}

	@Override
	public void customize(WebServerFactory factory) {
		setMimeMappings(factory);
		// When running in an IDE or with ./mvnw spring-boot:run, set location of the
		// static web assets.
		setLocationForStaticAssets(factory);

		/*
		 * Enable HTTP/2 for Undertow -
		 * https://twitter.com/ankinson/status/829256167700492288 HTTP/2 requires HTTPS,
		 * so HTTP requests will fallback to HTTP/1.1. See the JHipsterProperties class
		 * and your application-*.yml configuration files for more information.
		 */
		if (jHipsterProperties.getHttp().getVersion().equals(JHipsterProperties.Http.Version.V_2_0)
				&& factory instanceof UndertowServletWebServerFactory) {

			((UndertowServletWebServerFactory) factory)
					.addBuilderCustomizers(builder -> builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true));
		}

	}

	private void setLocationForStaticAssets(WebServerFactory server) {
		if (server instanceof ConfigurableServletWebServerFactory) {
			ConfigurableServletWebServerFactory servletWebServer = (ConfigurableServletWebServerFactory) server;
			File root;
			String prefixPath = resolvePathPrefix();
			root = new File(prefixPath + "target/www/");
			if (root.exists() && root.isDirectory()) {
				servletWebServer.setDocumentRoot(root);
			}
		}

	}
}
