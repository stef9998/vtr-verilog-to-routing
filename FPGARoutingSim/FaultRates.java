import java.util.HashMap;

public class FaultRates {
    final HashMap<Fault, Double> faultRates = new HashMap<>();  // HashMap containing Faults as Key and the faultrate as value

    // Constructor
    public FaultRates(double[] faultRates){
        // initialize faultrates in HashMap
        this.faultRates.put(Fault.SA0, faultRates[0]);
        this.faultRates.put(Fault.SA1, faultRates[0] + faultRates[1]);
        this.faultRates.put(Fault.UD, faultRates[0] + faultRates[1] + faultRates[2]);
    }

    // returns faultrate to a given fault type
    public double getFaultRate(Fault fault){
        return faultRates.get(fault);
    }
}
