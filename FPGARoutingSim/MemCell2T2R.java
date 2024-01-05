/**
 * Representation for a 2T(ransistor)2R(esistor) memory cell.
 * Calculates the fault of the memory cell with given memristors.
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class MemCell2T2R implements MemCell{
    final private Resistor puRes, pdRes;    // pull-up and pull-down resistor/memristor of the memory cell
    final private int numOfMemristorFaults;
    public MemCell2T2R(FaultRates faultRates){
        // initialize new pull-up and pull-down memristor with fault-rate
        this.puRes = new Resistor(faultRates);
        this.pdRes = new Resistor(faultRates);
        this.numOfMemristorFaults = calcMemristorFaults();
    }

    private int calcMemristorFaults(){
        int memristorFaults = 0;
        if (puResContainsFault()){
            memristorFaults++;
        }
        if (pdResContainsFault()){
            memristorFaults++;
        }
        return memristorFaults;
    }

    /**
     * returns fault contained by the memory cell
     * @return fault contained by the memory cell
     */
    @Override
    public Fault getCellFault(){
        // fault is FF (fault free) in default
        Fault fault = Fault.FF;

        // if pull-up memristor contains fault and pull-down memristor contains no fault
        if(puRes.containsFault() && !pdRes.containsFault()){
            // check the fault of pull-up memristor
            switch (puRes.getFault()){
                case SA1: fault = Fault.SA1; break;     // in case puRes contains SA1 memory cell is SA1
                case SA0: fault = Fault.SA0; break;     // in case puRes contains SA0 memory cell is SA0
                case UD: fault = Fault.UD;              // in case puRes contains UD memory cell is UD
            }
        }
        // else if pull-down memristor contains fault and pull-up memristor contains no fault
        else if(!puRes.containsFault() && pdRes.containsFault()){
            // check the fault of pull-down memristor
            switch (pdRes.getFault()){
                case SA1: fault = Fault.SA0; break;     // in case pdRes contains SA1 memory cell is SA0
                case SA0: fault = Fault.SA1; break;     // in case pdRes contains SA0 memory cell is SA1
                case UD: fault = Fault.UD;              // in case pdRes contains UD memory cell is UD
            }
        }
        // else if both contain fault
        else if(puRes.containsFault() && pdRes.containsFault()){
            // if both contain the same fault
            if(puRes.getFault() == pdRes.getFault()){
                fault = Fault.UD;                       // memory cell is UD
            }
            // else if one of the both or both contain a UD-Fault
            else if(puRes.getFault() == Fault.UD || pdRes.getFault() == Fault.UD){
                fault = Fault.UD;                       // memory cell is UD
            }
            // else if both contain a different fault and there is no UD in memristors
            else{
                fault = puRes.getFault();               // memory cell is the same as the fault of puRes
            }
        }

        // return the fault of the memory cell
        return fault;
    }

    /**
     * Returns the number of faulty memristors in this memory cell
     * @return the number of faulty memristors
     */
    @Override
    public int getNumOfFaultyMemristors(){
        return numOfMemristorFaults;
    }

    public boolean puResContainsFault(){
        return puRes.containsFault();
    }

    public boolean pdResContainsFault(){
        return pdRes.containsFault();
    }
}
