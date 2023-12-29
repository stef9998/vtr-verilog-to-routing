import org.jgrapht.graph.DefaultEdge;

/**
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class Switch extends DefaultEdge {
    final private MemCell controlCell;  // control cell of the switch/edge
    final private Fault memFault;

    /**
     * set control Cell
     */
    public Switch(MemCell controlCell){
        this.controlCell = controlCell;
        memFault = this.controlCell.getCellFault();
    }

    /**
     * returns cell fault of the memory cell controlling this switch
     */
    public Fault getFault(){
        return memFault;
    }

    /**
     * returns the memory cell controlling this switch
     */
    public MemCell getControlCell(){
        return controlCell;
    }
}
