
/**
 * Class was used in old MUX code by Lukas Freiberger to represent edges of a graph.
 * Now it is just used as a Wrapper for MemCell.
 *
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class Switch {
    final private MemCell controlCell;  // control cell of the switch/edge

    /**
     * set control Cell
     */
    public Switch(MemCell controlCell){
        this.controlCell = controlCell;
    }

    /**
     * returns fault of the memory cell controlling this switch
     * @return fault contained by the memory cell
     */
    public Fault getFault(){
        return controlCell.getCellFault();
    }

    /**
     * returns the memory cell controlling this switch
     * @return memory cell controlling this switch
     */
    public MemCell getControlCell(){
        return controlCell;
    }

    /**
     * Returns the number of faulty memristors in the memory cell that controls this switch
     * @return the number of faulty memristors
     */
    public int getNumOfFaultyMemristors(){
        return controlCell.getNumOfFaultyMemristors();
    }
}
