package com.starkandwayne.serviceregistry;

import org.springframework.core.env.ConfigurableEnvironment;

public class CloudFoundrySessionData {
    public String INSTANCE_GUID;
    public String INSTANCE_INDEX;
    public String INSTANCE_ADDR;
    public String INSTANCE_IP;
    public String INSTANCE_PORT;
    public String INTERNAL_IP;
    public String INTERNAL_PORT;
    public String INTSTANCE_TYPE;
    public String PEERS;

    public static CloudFoundrySessionData GetEnvironment()  {
        CloudFoundrySessionData cfd = new CloudFoundrySessionData();
        cfd.INSTANCE_INDEX = System.getenv("CF_INSTANCE_INDEX");
        cfd.INSTANCE_GUID = System.getenv("CF_INSTANCE_GUID");
        cfd.INSTANCE_ADDR = System.getenv("CF_INSTANCE_ADDR");
        cfd.INSTANCE_IP = System.getenv("CF_INSTANCE_IP");
        cfd.INSTANCE_PORT = System.getenv("CF_INSTANCE_PORT");
        cfd.INTERNAL_IP = System.getenv("CF_INSTANCE_INTERNAL_IP");
        cfd.INTERNAL_PORT = System.getenv("VCAP_APP_PORT");
        cfd.PEERS = System.getenv("PEERS");
        cfd.INTSTANCE_TYPE = System.getenv("PEERING_TYPE");
        return cfd;
    }
}
