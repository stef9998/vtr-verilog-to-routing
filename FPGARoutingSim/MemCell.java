/**
 * Representation for a memory cell.
 * Calculates the fault of the memory cell with given memristors.
 * Placeholder Class to use legacy code
 * @author Stefan Reichel
 */
public class MemCell {

    MemCell2T2R memCell;

    public MemCell(FaultRates faultRates){
        memCell = new MemCell2T2R(faultRates);
    }

    /**
     * returns fault contained by the memory cell
     * @return fault contained by the memory cell
     */
    public Fault getCellFault(){
        return memCell.getCellFault();
    }

    public boolean puResContFault(){
        return memCell.puResContFault();
    }

    public boolean pdResContFault(){
        return memCell.pdResContFault();
    }
}
