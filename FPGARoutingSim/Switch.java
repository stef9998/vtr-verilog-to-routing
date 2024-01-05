
/**
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
//public class Switch extends DefaultEdge {
public class Switch {
    final private MemCell controlCell;  // control cell of the switch/edge

    /**
     * set control Cell
     */
    public Switch(MemCell controlCell){
        this.controlCell = controlCell;
    }

    /**
     * returns cell fault of the memory cell controlling this switch
     */
    public Fault getFault(){
        return controlCell.getCellFault();
    }

    /**
     * returns the memory cell controlling this switch
     */
    public MemCell getControlCell(){
        return controlCell;
    }
}
