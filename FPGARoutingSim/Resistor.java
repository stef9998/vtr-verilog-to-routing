/**
 * Representation for memristor.
 * Used to tell if a specific memristor has a fault and which fault-type it is.
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class Resistor {
    private final Fault fault;  // fault of resistor/memristor

    public Resistor(FaultRates faultRates) {
        double rnd = Math.random() * 100;   // random number between 0 and 100 to select the fault

        // if rnd is between 0 and fault-rate of SA0
        if (rnd < faultRates.getFaultRate(Fault.SA0)){
            fault = Fault.SA0;  // fault is SA0
        }
        // if rnd is between fault-rate of SA0 and SA1
        else if (rnd < faultRates.getFaultRate(Fault.SA0) + faultRates.getFaultRate(Fault.SA1)){
            fault = Fault.SA1;  // fault is SA1
        }
        // if rnd is between fault-rate of SA1 and UD
        else if (rnd < faultRates.getFaultRate(Fault.SA0) + faultRates.getFaultRate(Fault.SA1) + faultRates.getFaultRate(Fault.UD)){
            fault = Fault.UD;   // fault is UD
        } else {
            fault = Fault.FF;   // memristor is fault free
        }
    }

    /**
     * returns true if memristor contains fault (SA0, SA1, UD)
     */
    public boolean containsFault(){
        return fault != Fault.FF;
    }

    /**
     * returns the fault-type contained in memristor
     * @return fault-type
     */
    public Fault getFault() {
        return fault;
    }
}
