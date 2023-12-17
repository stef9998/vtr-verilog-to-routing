import java.util.HashMap;

/**
 * Contains the fault-rates for the specific fault types
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class FaultRates {
    final HashMap<Fault, Double> faultRates = new HashMap<>();  // HashMap containing Faults as Key and the fault-rate as value

    /**
     * @param faultRates fault-rates for the different kinds of faults [SA0, SA1, UD]
     */
    public FaultRates(double[] faultRates){
        this.faultRates.put(Fault.SA0, faultRates[0]);
        this.faultRates.put(Fault.SA1, faultRates[1]);
        this.faultRates.put(Fault.UD, faultRates[2]);
    }

    /**
     * @param faultRate fault-rate for faults if all have the same kind of probability
     */
    public FaultRates(double faultRate){
        this(new double[]{faultRate, faultRate, faultRate});
    }

    /**
     * returns fault-rate to a given fault type
     * @param fault fault type
     * @return fault-rate
     */
    public double getFaultRate(Fault fault){
        return faultRates.get(fault);
    }
}
