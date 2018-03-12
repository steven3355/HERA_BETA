package sjsu.research.hera_beta_10;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Steven on 2/7/2018.
 */

public class MessageSystem {
    private Map<String, Queue<Message>> messageMap;
    private static String TAG = "MessageSystem";
    MessageSystem() {
        messageMap = new HashMap<>();
        messageMap.put("8ab5e5cf2db6867a", new LinkedList<Message>());
        messageMap.get("8ab5e5cf2db6867a").add(new Message("abcd".getBytes()));
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

    public void buildToSendMessageQueue(Connection curConnection) {
        HERA neighborHera = new HERA(curConnection.getNeighborHERAMatrix());
        HERA myHera = new HERA(curConnection.getMyHERAMatrix());
        List<String> mMessageDestinationList = this.getMessageDestinationList();

        for (String dest : mMessageDestinationList) {
            double myReachability = myHera.getReachability(dest);
            double neighborReachability = neighborHera.getReachability(dest);
            System.out.println("My reachability for destionation " + dest + " is " + myReachability);
            System.out.println("Neighbor Reachability is " + neighborReachability);
            if (neighborReachability > myReachability || curConnection.getNeighborAndroidID().equals(dest)) {
                Log.d(TAG, dest + " added to toSendQueue");
                curConnection.pushToSendQueue(dest);
            }
        }
    }


}
