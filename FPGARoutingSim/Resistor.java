public class Resistor {
    final Fault fault;  // fault of resistor/memristor

    // Constructor
    public Resistor(FaultRates faultRates) {
        double rnd = Math.random() * 100;   // random number between 0 and 100 to select the fault

        // if rnd is between 0 and faultrate of SA0
        if (0 <= rnd && rnd < faultRates.getFaultRate(Fault.SA0)){
            fault = Fault.SA0;  // fault is SA0
        }
        // else if rnd is between faultrate of SA0 and SA1
        else if (faultRates.getFaultRate(Fault.SA0) <= rnd && rnd < faultRates.getFaultRate(Fault.SA1)){
            fault = Fault.SA1;  // fault is SA1
        }
        // else if rnd is between faultrate of SA1 and UD
        else if (faultRates.getFaultRate(Fault.SA1) <= rnd && rnd < faultRates.getFaultRate(Fault.UD)){
            fault = Fault.UD;   // fault is UD
        } else {
            fault = Fault.FF;   // memristor is fault free
        }
    }

    // returns true if memristor contains fault
    public boolean contFault(){
        return fault != Fault.FF;
    }

    // returns the fault contained in memristor
    public Fault getFault() {
        return fault;
    }
}
