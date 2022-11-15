package com.starkandwayne.serviceregistry.environment;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class StaticPeerPostProcessor implements EnvironmentPostProcessor, InitializingBean {
   private static final DeferredLog LOG = new DeferredLog();

   public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
      String myServiceUrl = environment.getProperty("eureka.server.my-url");
      if (myServiceUrl == null) {
         LOG.debug("my-url is not yet available in environment");
      } else {
         LOG.info(String.format("My service url: %s", myServiceUrl));
         int count = this.getIntValueFromEnvironment(environment, "scs.service-registry.count", 1);
         LOG.info(String.format("Scale count: %d", count));
         int instanceIndex = this.getIntValueFromEnvironment(environment, "CF_INSTANCE_INDEX", 0);
         LOG.info(String.format("Cf instance index: %s", instanceIndex));
         PeerEnvironmentManager environmentManager = new PeerEnvironmentManager(environment);
         environmentManager.addScaledPeerUrls(myServiceUrl, count, instanceIndex);
      }
   }

   public void afterPropertiesSet() {
      LOG.replayTo(this.getClass());
   }

   private int getIntValueFromEnvironment(ConfigurableEnvironment environment, String key, int defaultValue) {
      String integerString = environment.getProperty(key);
      if (StringUtils.isEmpty(integerString)) {
         LOG.error(String.format("No %s provided, use default value %d", key, defaultValue));
         return defaultValue;
      } else {
         return Integer.parseInt(integerString);
      }
   }
}
