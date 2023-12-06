import org.jgrapht.graph.DefaultEdge;

public class Switch extends DefaultEdge {
    final private MemCell controlCell;  // control cell of the switch/edge

    // Constructor
    public Switch(MemCell controlCell){
        // set control Cell
        this.controlCell = controlCell;
    }

    // returns cell fault of the contained memCell
    public Fault getFault(){
        return controlCell.getCellFault();
    }

    public MemCell getControlCell(){
        return controlCell;
    }
}
