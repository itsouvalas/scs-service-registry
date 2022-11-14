package com.starkandwayne.serviceregistry.environment;

import com.starkandwayne.serviceregistry.ScsPeerInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

@Component
class PeerEnvironmentManager implements ApplicationEventPublisherAware {
   private static final Logger LOG = LoggerFactory.getLogger(PeerEnvironmentManager.class);
   private static final String PEER_MANAGER_PROPERTY_SOURCE = "PeerPropertySource";
   private static final String PEER_INFOS_PROPERTY_KEY = "scs.service-registry.peer-infos";
   static final String REGISTER_WITH_EUREKA_PROPERTY_KEY = "eureka.client.register-with-eureka";
   static final String SERVICE_URLS_PROPERTY_KEY = "eureka.client.service-url.defaultZone";
   static final String MY_SERVICE_URL_PROPERTY_KEY = "eureka.server.my-url";
   private static final Object lock = new Object();
   private final Map map;
   private ApplicationEventPublisher applicationEventPublisher;
   private AtomicInteger nextPeerIndex = new AtomicInteger(0);

   PeerEnvironmentManager(ConfigurableEnvironment environment) {
      if (!environment.getPropertySources().contains("PeerPropertySource")) {
         synchronized(lock) {
            if (!environment.getPropertySources().contains("PeerPropertySource")) {
               environment.getPropertySources().addFirst(new MapPropertySource("PeerPropertySource", new ConcurrentHashMap()));
          //     environment.getConversionService().addConverter(new ScsPeerInfo.Converter());
            }
         }
      }

      this.map = (Map)environment.getPropertySources().get("PeerPropertySource").getSource();
   }

   public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
      this.applicationEventPublisher = applicationEventPublisher;
   }

   void addScaledPeerUrls(String myServiceUrl, int count, int instanceIndex) {
      this.addPeerUrlsInternal(myServiceUrl, count);
      if (count > 1) {
         this.map.put("eureka.client.register-with-eureka", "true");
         this.map.put("eureka.server.my-url", this.getServiceUrlWithIndex(myServiceUrl, instanceIndex));
         this.publishEnvironmentChangeEvent("eureka.client.service-url.defaultZone", "eureka.client.register-with-eureka", "eureka.server.my-url");
      } else {
         this.publishEnvironmentChangeEvent("eureka.client.service-url.defaultZone");
      }

   }

   void addPeerInfo(ScsPeerInfo peerInfo) {
      this.addPeerUrlsInternal(peerInfo.getUri().toString(), peerInfo.getCount());
      String peerInfoKey = String.format("%s[%d]", "scs.service-registry.peer-infos", this.nextPeerIndex.getAndIncrement());
      this.map.put(peerInfoKey, peerInfo);
      if (this.map.getOrDefault("eureka.client.register-with-eureka", "").equals("true")) {
         this.publishEnvironmentChangeEvent("eureka.client.service-url.defaultZone", peerInfoKey);
      } else {
         this.map.put("eureka.client.register-with-eureka", "true");
         this.publishEnvironmentChangeEvent("eureka.client.service-url.defaultZone", peerInfoKey, "eureka.client.register-with-eureka");
      }

   }

   private void addPeerUrlsInternal(String peerUrl, int count) {
      this.map.merge("eureka.client.service-url.defaultZone", String.join(",", this.getAllScaledServiceUrls(peerUrl, count)), (oldPeers, newPeers) -> {
         return String.join(",", oldPeers.toString(), newPeers.toString());
      });
   }

   private List getAllScaledServiceUrls(String baseServiceUrl, int count) {
      return count == 1 ? Collections.singletonList(baseServiceUrl) : (List)IntStream.range(0, count).mapToObj((i) -> {
         return this.getServiceUrlWithIndex(baseServiceUrl, i);
      }).collect(Collectors.toList());
   }

   private String getServiceUrlWithIndex(String baseServiceUrl, int index) {
      return String.format("%s#%d", baseServiceUrl, index);
   }

   private void publishEnvironmentChangeEvent(String... updatedKeys) {
      LOG.debug("Updated environment: {}", this.map);
      if (this.applicationEventPublisher != null) {
         this.applicationEventPublisher.publishEvent(new EnvironmentChangeEvent(new HashSet(Arrays.asList(updatedKeys))));
      }
   }
}
