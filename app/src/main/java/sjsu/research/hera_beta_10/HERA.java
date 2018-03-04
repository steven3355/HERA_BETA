package sjsu.research.hera_beta_10;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Steven on 2/7/2018.
 */

public class HERA implements Serializable {
    private int H = 5;
    private double[] beta = {1, 0.5, 0.05, 0.005, 0.0005},
            gamma = {1, 0.5, 0.05, 0.005, 0.0005};
    private double alpha = 0.98;
    private long lastUpdateTime;
    private int timeUnitIndex = 1;
    private Map<String, List<Double>> reachabilityMatrix;
    private String self;
    public HERA() {
        reachabilityMatrix = new HashMap<>();
        reachabilityMatrix.put("abc", new ArrayList<Double>(3){});
        reachabilityMatrix.get("abc").add(1.0);
    }
    public HERA(Map<String, List<Double>> neighborMatrix) {
        reachabilityMatrix = neighborMatrix;
    }
    public HERA(int maxHop, double agingConstant, double[] intrinsicConfidence, double[] weight, int timeUnit, String selfAddress) {
        H = maxHop;
        alpha = agingConstant;
        beta = intrinsicConfidence;
        gamma = weight;
        timeUnitIndex = timeUnit;
        self = selfAddress;
        reachabilityMatrix = new HashMap<>();
    }
    public Map<String, List<Double>> getReachabilityMatrix() {
        return reachabilityMatrix;
    }
    public List<Double> getHopsReachability(String dest) {
        if (!reachabilityMatrix.containsKey(dest)) {
            reachabilityMatrix.put(dest, new ArrayList<Double>(H + 1));
        }
        return reachabilityMatrix.get(dest);
    }
    public double getHopReachability(String dest, int hop) {
        if (!reachabilityMatrix.containsKey(dest)) {
            reachabilityMatrix.put(dest, new ArrayList<Double>(H + 1));
        }
        return reachabilityMatrix.get(dest).get(hop);
    }
    public double getReachability(String dest) {
//        ageMatrix();
        double weightedSum = 0;
        if (!reachabilityMatrix.containsKey(dest)) {
            reachabilityMatrix.put(dest, new ArrayList<Double>(H + 1));
            return 0;
        }
        List<Double> hopReachabilities = reachabilityMatrix.get(dest);
        for (int i = 0; i <= H; i++) {
            weightedSum += gamma[i] * hopReachabilities.get(i);
        }
        return weightedSum;
    }

    public void updateDirectHop(String neighbor) {
        ageMatrix();
        if (!reachabilityMatrix.containsKey(neighbor)) {
            reachabilityMatrix.put(neighbor, new ArrayList<Double>(H + 1));
            reachabilityMatrix.get(neighbor).set(0, beta[0]);
        }
        else {
            reachabilityMatrix.get(neighbor).set(0 , reachabilityMatrix.get(neighbor).get(0) + 1);
        }
    }
    public void updateTransitiveHops(String neighbor, Map<String, List<Double>> neighborMap) {
        for(Map.Entry<String, List<Double>> entry : neighborMap.entrySet()) {
            if (entry.getKey() == self) {
                continue;
            }
            if (!reachabilityMatrix.containsKey(entry.getKey())) {
                reachabilityMatrix.put(entry.getKey(), new ArrayList<Double>(H + 1));
            }
            List<Double> updateHopList = reachabilityMatrix.get(entry.getKey());
            for (int h = 1; h < H; h++) {
                double delta = beta[h] * entry.getValue().get(h - 1);
                updateHopList.set(h, updateHopList.get(h) + delta);
            }
            reachabilityMatrix.put(entry.getKey(), updateHopList);
        }
    }
    private void ageMatrix() {
        long timeDiff = System.currentTimeMillis() - lastUpdateTime / TimeUnit.SECONDS.toMillis(1) / timeUnitIndex;
        double multiplier = Math.pow(alpha, timeDiff);
        for (Map.Entry<String, List<Double>> entry :reachabilityMatrix.entrySet()) {
            List<Double> curList = entry.getValue();
            for (int i = 0; i < H; i++) {
                curList.set(i, curList.get(i) * multiplier);
            }
            reachabilityMatrix.put(entry.getKey(), curList);
        }
        lastUpdateTime = System.currentTimeMillis();
    }
}