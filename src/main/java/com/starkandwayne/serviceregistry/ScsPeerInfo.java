package com.starkandwayne.serviceregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Objects;

import org.springframework.lang.Nullable;

@JsonIgnoreProperties(
   ignoreUnknown = true
)
public class ScsPeerInfo {
   private URI uri;
   @JsonProperty("nodeCount")
   private int count;
   @JsonProperty("service-instance-id")
   private String serviceInstanceId;
   @JsonProperty("issuer")
   private URI issuerUri;
   @JsonProperty("jwk-set-uri")
   private URI jwkSetUri;

   public URI getUri() {
      return this.uri;
   }

   public void setUri(URI uri) {
      this.uri = uri;
   }

   public int getCount() {
      return this.count;
   }

   public void setCount(int count) {
      this.count = count;
   }

   public String getServiceInstanceId() {
      return this.serviceInstanceId;
   }

   public void setServiceInstanceId(String serviceInstanceId) {
      this.serviceInstanceId = serviceInstanceId;
   }

   public URI getIssuerUri() {
      return this.issuerUri;
   }

   public void setIssuerUri(URI issuerUri) {
      this.issuerUri = issuerUri;
   }

   public URI getJwkSetUri() {
      return this.jwkSetUri;
   }

   public void setJwkSetUri(URI jwkSetUri) {
      this.jwkSetUri = jwkSetUri;
   }

   public String toString() {
      return "ScsPeerInfo{uri=" + this.uri + ", count=" + this.count + ", serviceInstanceId='" + this.serviceInstanceId + '\'' + ", issuerUri=" + this.issuerUri + ", jwkSetUri=" + this.jwkSetUri + '}';
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ScsPeerInfo peerInfo = (ScsPeerInfo)o;
         if (this.count != peerInfo.count) {
            return false;
         } else if (!Objects.equals(this.uri, peerInfo.uri)) {
            return false;
         } else if (!Objects.equals(this.serviceInstanceId, peerInfo.serviceInstanceId)) {
            return false;
         } else {
            return !Objects.equals(this.issuerUri, peerInfo.issuerUri) ? false : Objects.equals(this.jwkSetUri, peerInfo.jwkSetUri);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.uri != null ? this.uri.hashCode() : 0;
      result = 31 * result + this.count;
      result = 31 * result + (this.serviceInstanceId != null ? this.serviceInstanceId.hashCode() : 0);
      result = 31 * result + (this.issuerUri != null ? this.issuerUri.hashCode() : 0);
      result = 31 * result + (this.jwkSetUri != null ? this.jwkSetUri.hashCode() : 0);
      return result;
   }

   public static class Converter implements org.springframework.core.convert.converter.Converter<ScsPeerInfo, String> {
      private static final ObjectMapper JSON = new ObjectMapper();

      public String convert(ScsPeerInfo source) {
         try {
            return JSON.writeValueAsString(source);
         } catch (JsonProcessingException var3) {
            throw new RuntimeException(var3);
         }
      }
   }
}
