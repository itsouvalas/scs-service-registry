package com.starkandwayne.serviceregistry.peers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeerData {
    public String index;
    public String scheme;
    public String host;
    public String port; 

    @Override
    public String toString() {
        return this.scheme + "://" + this.host + ":" + this.port + "/eureka";
    }

}