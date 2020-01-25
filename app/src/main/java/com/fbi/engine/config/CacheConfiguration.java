package com.fbi.engine.config;

import javax.annotation.PreDestroy;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import io.github.jhipster.config.JHipsterConstants;
import io.github.jhipster.config.JHipsterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
@RequiredArgsConstructor
@AutoConfigureBefore(value = { WebConfigurer.class, DatabaseConfiguration.class })
public class CacheConfiguration {

	private final Environment env;

	@PreDestroy
	public void destroy() {
		log.info("Closing Cache Manager");
		Hazelcast.shutdownAll();
	}

	@Bean
	public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
		log.debug("Starting HazelcastCacheManager");
		return new com.hazelcast.spring.cache.HazelcastCacheManager(hazelcastInstance);
	}

	@Bean
	public HazelcastInstance hazelcastInstance(JHipsterProperties jHipsterProperties) {
		log.debug("Configuring Hazelcast");
		HazelcastInstance hazelCastInstance = Hazelcast.getHazelcastInstanceByName("fbiengine");
		if (hazelCastInstance != null) {
			log.debug("Hazelcast already initialized");
			return hazelCastInstance;
		}
		Config config = new Config();
		config.setInstanceName("fbiengine");
		config.getNetworkConfig().setPort(5701);
		config.getNetworkConfig().setPortAutoIncrement(true);

		// In development, remove multicast auto-configuration
		if (env.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))) {
			System.setProperty("hazelcast.local.localAddress", "127.0.0.1");

			config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
			config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
			config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
		}
		config.getMapConfigs().put("default", initializeDefaultMapConfig());
		config.getMapConfigs().put("com.fbi.engine.domain.*", initializeDomainMapConfig(jHipsterProperties));
		return Hazelcast.newHazelcastInstance(config);
	}

	private MapConfig initializeDefaultMapConfig() {
		MapConfig mapConfig = new MapConfig();

		/*
		 * Number of backups. If 1 is set as the backup-count for example, then all
		 * entries of the map will be copied to another JVM for fail-safety. Valid
		 * numbers are 0 (no backup), 1, 2, 3.
		 */
		mapConfig.setBackupCount(0);

		/*
		 * Valid values are: NONE (no eviction), LRU (Least Recently Used), LFU (Least
		 * Frequently Used). NONE is the default.
		 */
		mapConfig.setEvictionPolicy(EvictionPolicy.LRU);

		/*
		 * Maximum size of the map. When max size is reached, map is evicted based on
		 * the policy defined. Any integer between 0 and Integer.MAX_VALUE. 0 means
		 * Integer.MAX_VALUE. Default is 0.
		 */
		mapConfig.setMaxSizeConfig(new MaxSizeConfig(0, MaxSizeConfig.MaxSizePolicy.USED_HEAP_SIZE));

		return mapConfig;
	}

	private MapConfig initializeDomainMapConfig(JHipsterProperties jHipsterProperties) {
		MapConfig mapConfig = new MapConfig();
		mapConfig.setTimeToLiveSeconds(jHipsterProperties.getCache().getHazelcast().getTimeToLiveSeconds());
		return mapConfig;
	}
}
