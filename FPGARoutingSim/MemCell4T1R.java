/**
 * Representation for a 4T(ransistor)1R(esistor) memory cell.
 * Calculates the fault of the memory cell with given memristors.
 * @author Stefan Reichel
 */
public class MemCell4T1R implements MemCell{

    final private Resistor mem1;    // pull-up and pull-down resistor/memristor of the memory cell

    public MemCell4T1R(FaultRates faultRates){
        // initialize memristor with fault-rate
        mem1 = new Resistor(faultRates);
    }

    /**
     * Returns fault contained by the memory cell
     * @return fault contained by the memory cell
     */
    @Override
    public Fault getCellFault(){
        return mem1.getFault();
    }

    /**
     * Returns the number of faulty memristors in this memory cell
     * @return the number of faulty memristors
     */
    @Override
    public int getNumOfFaultyMemristors() {
        return mem1.containsFault() ? 1 : 0;
    }

    //TODO after number of memristors with fault calculation is moved out of MUX, it can be added here.

}
