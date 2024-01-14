import java.util.ArrayList;
import java.util.List;

/**
 * @author Stefan Reichel
 */
public abstract class MUX {

//    public MUX (ArrayList<RREdge> rrEdges, FaultRates faultRates){
//        //TODO
//    }

    public abstract List<int[]> getDefectRREdgesList();

    public abstract int getNumberOfEdges();

    public abstract int getNumOfDefectEdges();

    public abstract int getNumberOfMemCells();

    public abstract int getNumberOfFaultyMemristors();

    public abstract int getNumOfSA0();

    public abstract int getNumOfSA1();

    public abstract int getNumOfUD();

    public abstract String printStats();

    public abstract String printGraph();
}
