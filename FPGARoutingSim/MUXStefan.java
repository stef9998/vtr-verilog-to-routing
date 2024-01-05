import org.jgrapht.GraphPath;

import java.util.*;

/**
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class MUXStefan {

    private final int switchID, sinkNodeID, muxSize, blockSize;                                     // Index of sink node, ID of the switch, Number of source nodes, Maximum size of one input block
    private final HashMap<Integer, RRNodeType> srcNodeTypes = new HashMap<>();          // Hash Map with Source Node Types
    private final RRNodeType sinkRRNodeType;                                            // Node Type of Sink Node
    private final FaultRates faultRates;
    private int numOfSA0MemCells, numOfSA1MemCells, numOfUDMemCells = 0;
    private int numOfSA0Memristors, numOfSA1Memristors, numOfUDMemristors = 0;
    //TODO think about if actually neccessary to have them as field
    // probably just possible to get it from SwitchTree<-Switch<-MemCell<-...
    private final Set<RREdge> defectRREdges = new HashSet<>(); //TODO change to TreeSet maybe -> maybe not, as it will be sorted later anyways. And does not seem to be that slow. And we can change later how it will be sorted!
    private ArrayList<Vertex> srcVertices = new ArrayList<>();                          // Source Vertex of the Graph
//    Graph<Vertex, Switch> muxGraph = new SimpleDirectedGraph<>(Switch.class);   // Graph containing nodes and edges representing the MUX architecture //TODO if I want something for printout, do something close to this

    private final Switch[] frstStageSwitches;                                           // switches in first stage
    private final Switch[] scndStageSwitches;                                           // switches in second stage
    private int secondStageInDegree;
    private ArrayList<GraphPath<Vertex, Switch>> defectPaths = new ArrayList<>();       // array list of defect paths/edges in the mux

    // Constructor
    public MUXStefan(ArrayList<RREdge> rrEdges, FaultRates faultRates){
        this.faultRates = faultRates;
//        this.rrEdges = rrEdges;
        frstStageSwitches = null; //TODO delete
        scndStageSwitches = null; //TODO delete

        // Set switchID, sinkNodeID, muxSize and maxBlockSize
        this.switchID = rrEdges.get(0).getSwitchID();
        this.sinkNodeID = rrEdges.get(0).getSinkNodeID();
        this.muxSize = rrEdges.size();
        this.blockSize = calcBlockSize(); //TODO

        // Sink and Source Node Types for print out
        this.sinkRRNodeType = rrEdges.get(0).getSinkNodeType();

        for (RREdge rrEdge : rrEdges) {
            this.srcNodeTypes.put(rrEdge.getNodeID(), rrEdge.getNodeType()); //TODO seems to be not used
        }

        calculateUsability(rrEdges);

    }

    private void calculateUsability(ArrayList<RREdge> rrEdges) {
        // split list of edges into sublists. One sublist represent one connected block in first multiplexer stage.
        List<List<RREdge>> firstStageNeighborhoods = ListSplitter.splitList(rrEdges, blockSize);
        secondStageInDegree = firstStageNeighborhoods.size();
        List<Switch> secondStageSwitches = new ArrayList<>();
        List<Fault> secondStageFaults = new ArrayList<>(); //TODO needed?

        inNeighborhoodFaultStatusVariables secondStageNeighborhood = new inNeighborhoodFaultStatusVariables();

        for (int i = 0; i < secondStageInDegree; i++) {
            secondStageSwitches.add(i, new Switch(new MemCell4T1R(faultRates)));
            secondStageFaults.add(i, secondStageSwitches.get(i).getFault());
        }
        inNeigborhoodStatusVariablesCalc(secondStageInDegree, secondStageSwitches, secondStageNeighborhood);


        // UD in second stage or more than one SA1 in second stage
        if (secondStageNeighborhood.stageHasUD || (secondStageNeighborhood.stageNoOfSA1 > 1)){
            // remove everything //TODO maybe change to other possible fix (first stage all 0)
            for (int i = 0; i < secondStageInDegree; i++) {
                defectRREdges.addAll(firstStageNeighborhoods.get(i));
            }
        }
        // one SA1 in second stage
        else if (secondStageNeighborhood.stageNoOfSA1 == 1){ //TODO merge into above if other possible fix works
            // remove everything except the one SA1 path
            for (int i = 0; i < secondStageInDegree; i++) {
                List<RREdge> firstStageTree = firstStageNeighborhoods.get(i);
                Fault memCellFault = secondStageFaults.get(i);
                if (memCellFault == Fault.SA1){
                    calculateFirstStageDefects(firstStageTree, memCellFault);
                } else {
                    defectRREdges.addAll(firstStageTree);
                }
            }
        }
        // no UD or SA1 in second stage
        else {
            for (int i = 0; i < secondStageInDegree; i++) {
                List<RREdge> firstStageTree = firstStageNeighborhoods.get(i);
                Fault memCellFault = secondStageFaults.get(i);
                if (memCellFault == Fault.SA0) {
                    defectRREdges.addAll(firstStageTree);
                } else {
                    calculateFirstStageDefects(firstStageTree, memCellFault);
                }
            }
        }
    }

    private inNeighborhoodFaultStatusVariables calculateFirstStageDefects(List<RREdge> rrEdges, Fault secondStageFault){
        List<Fault> memCellfaults = new ArrayList<>(); //TODO needed?
        List<Switch> switches = new ArrayList<>();
        int inDegree = rrEdges.size();
        inNeighborhoodFaultStatusVariables faultVariables = new inNeighborhoodFaultStatusVariables();

        for (int i = 0; i < inDegree; i++) {
            switches.add(i, new Switch(new MemCell4T1R(faultRates)));
            memCellfaults.add(i, switches.get(i).getFault());
        }
        inNeigborhoodStatusVariablesCalc(inDegree, switches, faultVariables);

        // UD or more than one SA1
        if (faultVariables.stageHasUD || (faultVariables.stageNoOfSA1 > 1)){
            if (secondStageFault == Fault.SA1) {
                defectRREdges.addAll(rrEdges);
                //TODO adjustment if second stage SA1 calc is changed
                // as we need to remove every node of complete MUX, because UD + SA1
            } else if (secondStageFault == Fault.FF) {
                defectRREdges.addAll(rrEdges);
            }
        }
        // one SA1
        else if (faultVariables.stageNoOfSA1 == 1){
            // remove everything except the SA1
            for (int i = 0; i < inDegree; i++) {
                Fault memCellFault = memCellfaults.get(i);
                if (memCellFault != Fault.SA1){
                    defectRREdges.add(rrEdges.get(i));
                }
            }
        }
        // no UD or SA1
        else {
            for (int i = 0; i < inDegree; i++) {
                Fault memCellFault = memCellfaults.get(i);
                if (memCellFault == Fault.SA0) {
                    defectRREdges.add(rrEdges.get(i));
                }
            }
        }
        return faultVariables;
    }

    private static void inNeigborhoodStatusVariablesCalc(int inDegree, List<Switch> switches, inNeighborhoodFaultStatusVariables faultVariables) {
        for (int i = 0; i < inDegree; i++) {
            Fault memCellFault = switches.get(i).getFault();

            switch (memCellFault) {
                case UD:
                    faultVariables.stageHasUD = true;
                    break;
                case SA0:
                    break;
                case SA1:
                    faultVariables.stageNoOfSA1++;
                    break;
                case FF:
                    break;
            }

        }
    }

    private static class inNeighborhoodFaultStatusVariables {
        public boolean stageHasUD;
        public int stageNoOfSA1;

        public inNeighborhoodFaultStatusVariables() {
            this.stageHasUD = false;
            this.stageNoOfSA1 = 0;
        }

        public inNeighborhoodFaultStatusVariables(boolean stageHasUD, int stageNoOfSA1) {
            this.stageHasUD = stageHasUD;
            this.stageNoOfSA1 = stageNoOfSA1;
        }
    }


    // returns the list with defect paths
    public ArrayList<int[]> getRREdgeDeleteList(){ //TODO is the old method from lucas MUX
        return (ArrayList<int[]>) getDefectRREdgesList();
    }

    public Set<RREdge> getDefectRREdges() {
        return defectRREdges;
    }

    public List<int[]> getDefectRREdgesList() {
        List<int[]> edges = new ArrayList<>();
        int i = 0;
        for (RREdge edge : defectRREdges) {
            edges.add(i, XMLReader.convertRREdgeForXMLWrite(edge));
            i++;
        }
        return edges;
    }


    // calculates the block size such that number of mem cells is minimal
    // TODO: is the old method from lukas. Does not minimize the memCells anymore.
    //  minimal would now be the trivial case of one stage.
    private int calcBlockSize(){
        int blockSize = muxSize;        // initial block size is mux size
        int numOfMemCell = muxSize;     // initial number of mem cells is mux size
        int rest;                       // is one if there is an additional partial block
        int newNumOfMemCell;            // calculated number of mem cells

        // for every block size from 1 to (mux size - 1)
        for (int newBlockSize = 1; newBlockSize < muxSize; newBlockSize++){

            // calculate if there will be an additional partial block
            rest = ((muxSize % newBlockSize) == 0) ? 0 : 1;

            // calculate the required number of mem cells for the new block size
            newNumOfMemCell = (newBlockSize + (muxSize / newBlockSize)) + rest;

            // if the required number of mem cells for the new block size is smaller than the number of required mem cells
            if (newNumOfMemCell < numOfMemCell){

                // set number of mem cells and block size to the calculated values
                numOfMemCell = newNumOfMemCell;
                blockSize = newBlockSize;
            }
        }
        // return the calculated block size
        return blockSize;
    }

    /**
     * @return if MUX has more than one stage (== two stages)
     */
    private boolean hasMultipleStages(){
        return muxSize > blockSize;
    }

    /**
     * @return if MUX has only one stage
     */
    private boolean hasOneStage(){
        return muxSize <= blockSize;
    }

    //TODO put Switch/Fault/... in usage-calculation into new fields to print them out if needed
//    /**
//     * returns a readable representation of the MUX Graph
//     * @return representation of the MUX Graph
//     */
//    public String printGraph(){
//        StringBuilder out = new StringBuilder();
//
//        // print graph in String Builder
//        Set<Switch> edges = muxGraph.edgeSet();
//        for(Switch edge: edges){
//            out.append("Edge Source: (").append(muxGraph.getEdgeSource(edge).getVertexID()).append(", ").append(muxGraph.getEdgeSource(edge).getVertexType()).append("); Fault: ").append(edge.getFault()).append("; Edge Target: (").append(muxGraph.getEdgeTarget(edge).getVertexID()).append(", ").append(muxGraph.getEdgeTarget(edge).getVertexType()).append(")\n");
//        }
//        return out.toString();
//    }

    /**
     * prints some statistics about the multiplexer
     * @return statistics about the multiplexer
     */
    public String printStats(){
        // general information on MUX
        String out = ("Mux Size: " + muxSize + " Inputs\nSink Node: " + sinkNodeID + "\nSwitch ID: " + switchID);

        // num of mem cells
        int numMemCells = getNumberOfMemCells();
        out += ("\nNumber of MemCells: " + numMemCells + "\n");

        return out;
    }

    /**
     * returns the number of edges
     * @return number of edges
     */
    public int getNumberOfEdges(){
        return muxSize;
    }

    /**
     * returns the number of defect edges
     * @return number of defect edges
     */
    public int getNumOfDefectEdges(){
        return defectRREdges.size();
    }

    /**
     * returns the number of mem cells used by the MUX
     * @return number of mem cells used by the MUX
     */
    public int getNumberOfMemCells(){
        // Every edge has one path with one memory cell in first stage. Then add path of second stage.
        return muxSize + secondStageInDegree;
    }

}
