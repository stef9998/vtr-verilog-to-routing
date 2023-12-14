import org.jgrapht.graph.DefaultEdge;

/**
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class Switch extends DefaultEdge {
    final private MemCell controlCell;  // control cell of the switch/edge

    /**
     * set control Cell
     */
    public Switch(MemCell controlCell){
        this.controlCell = controlCell;
    }

    /**
     * returns cell fault of the contained memCell
     */
    public Fault getFault(){
        return controlCell.getCellFault();
    }

    public MemCell getControlCell(){
        return controlCell;
    }
}
