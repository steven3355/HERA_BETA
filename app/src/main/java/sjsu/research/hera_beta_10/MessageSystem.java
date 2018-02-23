package sjsu.research.hera_beta_10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Steven on 2/7/2018.
 */

public class MessageSystem {
    private Map<String, Queue<Message>> messageMap;

    MessageSystem() {
        messageMap = new HashMap<>();
    }
    Queue<Message> getMessageQueue(String dest) {
        return messageMap.get(dest);
    }
    void putMessage(byte[] input) {

    }

    Message getMessage(String dest) {
        return getMessageQueue(dest).poll();
    }
    List<String> getMessageDestinationList() {
        return new ArrayList<>(messageMap.keySet());
    }

    public void buildToSnedMessageQueue(Connection curConnection) {
        HERA neighborHera = new HERA(curConnection.getNeighborHERAMatrix());
        HERA myHera = new HERA(curConnection.getMyHERAMatrix());
        List<String> mMessageDestinationList = this.getMessageDestinationList();

        for (String dest : mMessageDestinationList) {
            double myReachability = myHera.getReachability(dest);
            double neighborReachability = neighborHera.getReachability(dest);
            System.out.println("My reachability for destionation " + dest + " is " + myReachability);
            System.out.println("Neighbor Reachability is " + neighborReachability);
            if (neighborReachability > myReachability) {
                curConnection.pushToSendQueue(dest);
            }
        }
    }
}
