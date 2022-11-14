package com.starkandwayne.serviceregistry.environment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkandwayne.serviceregistry.ScsPeerInfo;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class ExternalPeerInfoCollector implements InitializingBean {
   private static final Logger LOG = LoggerFactory.getLogger(ExternalPeerInfoCollector.class);
   private final ConfigurableEnvironment environment;
   private final Executor collationExecutor;
   private final CompletionService peerInfoCompletionService;
   private final ObjectMapper objectMapper;

   private final RestOperations restTemplate;
   private final PeerEnvironmentManager environmentManager;
   private final RefreshScope refreshScope;
   private final AtomicInteger unresolvedPeerCount = new AtomicInteger();
   @Value("${scs.service-registry.external-peer-polling-interval-seconds}")
   private int peerPollingIntervalSeconds;


   public ExternalPeerInfoCollector(ConfigurableEnvironment environment, ObjectMapper objectMapper, RestOperations restTemplate, PeerEnvironmentManager environmentManager,  RefreshScope refreshScope) {
      this.environment = environment;
      this.objectMapper = objectMapper;
      this.restTemplate = restTemplate;
      this.environmentManager = environmentManager;
      this.refreshScope = refreshScope;
      this.peerInfoCompletionService = new ExecutorCompletionService(Executors.newCachedThreadPool(new ThreadFactory("fetcher %d")));
      this.collationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory("collator"));
   }

   public void afterPropertiesSet() throws Exception {
      String peersSetByBrokerString = this.environment.getProperty("scs.service-registry.peers");
      if (!StringUtils.isEmpty(peersSetByBrokerString)) {
         List<PeerConfigUserInput> peersSetByBroker = this.objectMapper.readValue(peersSetByBrokerString, new TypeReference<List<PeerConfigUserInput>>(){});
         this.unresolvedPeerCount.set(peersSetByBroker.size());
         LOG.info(String.format("%d external peers to be resolved: %s", this.unresolvedPeerCount.get(), peersSetByBroker));
         peersSetByBroker.forEach((peerConfig) -> {
            this.peerInfoCompletionService.submit(() -> {
               return this.fetchPeerInfo(peerConfig);
            });
         });
         this.collationExecutor.execute(this::awaitPeerInfo);
      }
   }

   private ScsPeerInfo fetchPeerInfo(PeerConfigUserInput peerConfig) throws InterruptedException {
      if (peerConfig.isSkipSslValidation()) {
         this.trustPeerCertificates(peerConfig);
      }

      return this.retrievePeerInfoFromInfoActuator(peerConfig);
   }

   private void trustPeerCertificates(PeerConfigUserInput peerConfig) throws InterruptedException {
      URI uri = peerConfig.getEurekaServerUri();
      int port = uri.getPort() > 0 ? uri.getPort() : 443;

      while(true) {
         LOG.info("Attempting SSL certificate fetch from {}", uri);

         try {
            LOG.warn("SSL validation will be skipped for {}", uri);
            return;
         } catch (Exception var5) {
            LOG.info(String.format("Failed to trust certificate at %s:%d: %s", uri.getHost(), port, var5.getMessage()));
            LOG.debug("Exception detail", var5);
            LOG.info("Sleeping {}s before retrying", this.peerPollingIntervalSeconds);
            Thread.sleep(Duration.of((long)this.peerPollingIntervalSeconds, ChronoUnit.SECONDS).toMillis());
         }
      }
   }

   private ScsPeerInfo retrievePeerInfoFromInfoActuator(PeerConfigUserInput peerConfig) throws InterruptedException {
      URI infoActuatorUri = peerConfig.getInfoActuatorUri();

      while(true) {
         LOG.debug("Attempting peer info fetch from {}", infoActuatorUri);

         try {
            ScsPeerInfo info = (ScsPeerInfo)this.restTemplate.getForObject(infoActuatorUri, ScsPeerInfo.class);
            if (info == null) {
               throw new RuntimeException(String.format("Peer info from %s is missing", infoActuatorUri));
            }

            info.setUri(peerConfig.getEurekaServerUri());
            return info;
         } catch (RestClientException var4) {
            LOG.info(String.format("Unable to fetch peer info from %s: %s", infoActuatorUri, var4.getMessage()));
            LOG.debug("Exception detail", var4);
            LOG.info("Sleeping {}s before retrying", this.peerPollingIntervalSeconds);
            Thread.sleep(Duration.of((long)this.peerPollingIntervalSeconds, ChronoUnit.SECONDS).toMillis());
         }
      }
   }

   private void awaitPeerInfo() {
      while(this.unresolvedPeerCount.get() > 0) {
         Future peerInfoFuture;
         try {
            LOG.info("Awaiting next of {} remaining peer infos", this.unresolvedPeerCount.get());
            peerInfoFuture = this.peerInfoCompletionService.take();
         } catch (InterruptedException var5) {
            LOG.error("Interrupted while awaiting peer information", var5);
            Thread.currentThread().interrupt();
            return;
         }

         try {
            ScsPeerInfo peerInfo = (ScsPeerInfo)peerInfoFuture.get();
            LOG.info("Received new peer information: {}", peerInfo);
            this.environmentManager.addPeerInfo(peerInfo);
            this.refreshScope.refreshAll();
            this.unresolvedPeerCount.decrementAndGet();
         } catch (ExecutionException var3) {
            LOG.error("Unable to fetch peer info", var3);
         } catch (InterruptedException var4) {
            LOG.error("Interrupted while getting peer info", var4);
            Thread.currentThread().interrupt();
         }
      }

      LOG.info("All configured peers have been resolved");
   }

   static final class PeerConfigUserInput {
      private URI uri;
      private boolean skipSslValidation;

      public URI getUri() {
         return this.uri;
      }

      public void setUri(URI uri) {
         this.uri = uri;
      }

      public boolean isSkipSslValidation() {
         return this.skipSslValidation && this.getUri().getScheme().equals("https");
      }

      public void setSkipSslValidation(boolean skipSslValidation) {
         this.skipSslValidation = skipSslValidation;
      }

      URI getInfoActuatorUri() {
         return this.getUriWithPath("/actuator/info");
      }

      URI getEurekaServerUri() {
         return this.getUriWithPath("/eureka/");
      }

      private URI getUriWithPath(String path) {
         return UriComponentsBuilder.fromUri(this.getUri()).replacePath(path).build().toUri();
      }

      public String toString() {
         return this.getUri().toString();
      }
   }

   private static class ThreadFactory implements java.util.concurrent.ThreadFactory {
      private static final ThreadGroup threadGroup = new ThreadGroup("external-peer-info");
      private final AtomicInteger count;
      private final String namingPattern;

      private ThreadFactory(String namingPattern) {
         this.count = new AtomicInteger();
         this.namingPattern = namingPattern;
      }

      public Thread newThread(Runnable r) {
         Thread thread = new Thread(threadGroup, r, String.format(this.namingPattern, this.count.getAndIncrement()));
         thread.setDaemon(true);
         return thread;
      }

      // $FF: synthetic method
      ThreadFactory(String x0, Object x1) {
         this(x0);
      }
   }
}
