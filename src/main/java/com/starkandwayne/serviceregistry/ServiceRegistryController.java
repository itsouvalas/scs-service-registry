package com.starkandwayne.serviceregistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/config")
public class ServiceRegistryController {
    
    @GetMapping(
        value = "/session-data",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<CloudFoundrySessionData> getCfSessionData() {
        return ResponseEntity.ok().body(CloudFoundrySessionData.GetEnvironment());
    }

    @GetMapping(
        value = "/peers",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<LinkedList<ScsPeerInfo>> getPeers() {
        return ResponseEntity.ok().body(ServiceRegistryApplication.CurrentLoadedPeers);
        }

    @PostMapping(
        value = "/peers",
        consumes = {MediaType.APPLICATION_JSON_VALUE},
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<LinkedList<ScsPeerInfo>> postPeers(@RequestBody LinkedList<ScsPeerInfo> listpeers) {
       try
       {
        ServiceRegistryApplication.CurrentLoadedPeers = listpeers;
        System.out.println(ServiceRegistryApplication.CurrentLoadedPeers);
        UpdateEnvExcuteProcess(listpeers);
        return ResponseEntity.ok().body(ServiceRegistryApplication.CurrentLoadedPeers);
       }
       catch (Exception ex)
       {
        return ResponseEntity.badRequest().body(listpeers);
       }

        
    }

    public void UpdateEnvExcuteProcess(LinkedList<ScsPeerInfo> listpeers) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listpeers);
            ConfigurableEnvironment environment = new StandardEnvironment();
 MutablePropertySources propertySources = environment.getPropertySources();
 Map<String, Object> myMap = new HashMap<>();
 myMap.put("PATH", json);
 propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
        } catch (JsonProcessingException e) {
            
            e.printStackTrace();
        }
       
        
    }
}
