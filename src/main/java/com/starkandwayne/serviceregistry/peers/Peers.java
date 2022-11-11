package com.starkandwayne.serviceregistry.peers;

import java.util.LinkedList;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkandwayne.serviceregistry.ServiceRegistryApplication;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Peers {
    public LinkedList<PeerData> CurrentPeers = new LinkedList<PeerData>();

    public void AddPeer(PeerData peer) {
        CurrentPeers.add(peer);
    }

    public void ClearPeers() {
        CurrentPeers.clear();
    }

    public static Peers LoadPeersFromENV(){

        Peers peer = new Peers();

        if (ServiceRegistryApplication.SessionData.INTSTANCE_TYPE == null)
        {
            System.out.println("PEERING_TYPE was null");
            return peer;
        } else 
        {
            if (!ServiceRegistryApplication.SessionData.INTSTANCE_TYPE.equals("clustered")) {
                System.out.println("PEERING_TYPE was " + ServiceRegistryApplication.SessionData.INTSTANCE_TYPE);
                return peer;
            }      
        }

        if (ServiceRegistryApplication.SessionData.PEERS == null) {
            System.out.println("PEERS was null");
            return peer;
        }

        peer.CurrentPeers = GetPeersFromJSON(ServiceRegistryApplication.SessionData.PEERS);
        return peer;
    }

    public static LinkedList<PeerData> GetPeersFromJSON(String jsonString){
        String peerString = jsonString.replace("\\\"", "\"");
        LinkedList<PeerData> currentPeers = new LinkedList<PeerData>();
        ObjectMapper obj = new ObjectMapper();
        try {
            System.out.print("loading peers from: "  + peerString);
            currentPeers =  obj.readValue(peerString, new TypeReference<LinkedList<PeerData>>(){});
        } catch (JsonMappingException e) {
            
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            
            e.printStackTrace();
        }
        return currentPeers;
    }


    public String GetPeersAsString() {
        StringBuilder peerList = new StringBuilder();
        int count = CurrentPeers.size();
        int current_index = 0;
        for (PeerData peerData : CurrentPeers) {
            if (peerData.host.equals(ServiceRegistryApplication.SessionData.INSTANCE_IP) && peerData.port.equals(ServiceRegistryApplication.SessionData.INSTANCE_PORT)) {
                peerList.append("http://localhost:8080/eureka");
            } else 
            {
                peerList.append(peerData.toString());
            }     
            current_index++;
            if (current_index != count)
            {
                peerList.append(",");
            }
        }

        return peerList.toString();
    }
}
