/**
 * Representation for a 4T(ransistor)1R(esistor) memory cell.
 * Calculates the fault of the memory cell with given memristors.
 * @author Stefan Reichel
 */
public class MemCell4T1R {

    final private Resistor mem1;    // pull-up and pull-down resistor/memristor of the memory cell

    public MemCell4T1R(FaultRates faultRates){
        // initialize memristor with fault-rate
        mem1 = new Resistor(faultRates);
    }

    /**
     * returns fault contained by the memory cell
     * @return fault contained by the memory cell
     */
    public Fault getCellFault(){
        return mem1.getFault();
    }

    //TODO after number of memristors with fault calculation is moved out of MUX, it can be added here.

}
