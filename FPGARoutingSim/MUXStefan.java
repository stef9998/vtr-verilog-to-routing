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
    private final Set<RREdge> defectRREdges = new HashSet<>(); //TODO change to TreeSet maybe -> maybe not, as it will be sorted later anyways. And does not seem to be that slow. And we can change later how it will be sorted!
    private ArrayList<Vertex> srcVertices = new ArrayList<>();                          // Source Vertex of the Graph
//    Graph<Vertex, Switch> muxGraph = new SimpleDirectedGraph<>(Switch.class);   // Graph containing nodes and edges representing the MUX architecture //TODO if I want something for printout, do something close to this

    private SwitchTree secondStageNeighborhood;
    private final List<SwitchTree> firstStageNeighborhoods = new ArrayList<>(); //TODO maybe for printout. But I might still want to add a Neighborhood-Class to bundle everything
    private int secondStageInDegree;
    private ArrayList<GraphPath<Vertex, Switch>> defectPaths = new ArrayList<>();       // array list of defect paths/edges in the mux

    // Constructor
    public MUXStefan(ArrayList<RREdge> rrEdges, FaultRates faultRates){
        this.faultRates = faultRates;
//        this.rrEdges = rrEdges;

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
        List<List<RREdge>> firstStageRREdgeNeighborhoods = ListSplitter.splitList(rrEdges, blockSize);
        secondStageInDegree = firstStageRREdgeNeighborhoods.size();

        secondStageNeighborhood = new SwitchTree(secondStageInDegree, MemCell4T1R.class, faultRates);

        for (int i = 0; i < secondStageInDegree; i++) {
            List<RREdge> firstStageRREdgeNeighborhood = firstStageRREdgeNeighborhoods.get(i);

            SwitchTree firstStageNeighborhood = calculateFirstStageDefects(firstStageRREdgeNeighborhood);
            firstStageNeighborhoods.add(i, firstStageNeighborhood);
        }
        //TODO Sonderfall secondStageInDegree == 1
        int unDisconnectableSA1s = 0;
        mux_utility_calc: for (int i = 0; i < secondStageInDegree; i++) {
            // for every second stage switch
            // calculate the corresponding neighborhood
            Fault secondStageFault = secondStageNeighborhood.getFault(i);
            List<RREdge> firstStageRREdgeNeighborhood = firstStageRREdgeNeighborhoods.get(i);

            SwitchTree firstStageNeighborhood = firstStageNeighborhoods.get(i);

            // depending on fault state of the second stage the corresponding first stage
            switch (secondStageFault) {
                case FF:
                    break;
                case SA0:
                    defectRREdges.addAll(firstStageRREdgeNeighborhood);
                    break;
                case UD:
                    if (firstStageNeighborhood.hasUD() || firstStageNeighborhood.hasSA1()) {
                        deleteEveryEdge(firstStageRREdgeNeighborhoods);
                        break mux_utility_calc;
                    } else {
                        defectRREdges.addAll(firstStageRREdgeNeighborhood);
                    }
                    break;
                case SA1:
                    if (firstStageNeighborhood.hasUD() || firstStageNeighborhood.hasMoreThanOneSA1()) {
                        deleteEveryEdge(firstStageRREdgeNeighborhoods);
                        break mux_utility_calc;
                    } else if (firstStageNeighborhood.hasOneSA1()) {
                        unDisconnectableSA1s ++;
                        // TODO
                    } else {
                        defectRREdges.addAll(firstStageRREdgeNeighborhood);
                    }
                    break;
            }
        }

        if (unDisconnectableSA1s > 1) {
            //TODO now that there is a SwitchTree class, I can make the calculation efficient
            deleteEveryEdge(firstStageRREdgeNeighborhoods);
        }
    }

    private SwitchTree calculateFirstStageDefects(List<RREdge> rrEdges){
        // create a new switch for every rrEdge
        SwitchTree switchTree = new SwitchTree(rrEdges, MemCell4T1R.class, faultRates);
        int numOfSwitches = switchTree.getNumOfSwitches();

        // UD or more than one SA1
        if (switchTree.hasUD() || switchTree.hasMoreThanOneSA1()) {
            defectRREdges.addAll(rrEdges);
        }
        // one SA1
        else if (switchTree.hasOneSA1()){
            // remove everything except the SA1
            for (int i = 0; i < numOfSwitches; i++) {
                Fault memCellFault = switchTree.getFault(i);
                if (memCellFault != Fault.SA1){
                    defectRREdges.add(rrEdges.get(i));
                }
            }
        }
        // no UD or SA1
        else {
            for (int i = 0; i < numOfSwitches; i++) {
                Fault memCellFault = switchTree.getFault(i);
                if (memCellFault == Fault.SA0) {
                    defectRREdges.add(rrEdges.get(i));
                }
            }
        }
        return switchTree;
    }

    private void deleteEveryEdge(List<List<RREdge>> firstStageRREdgeNeighborhood){
        if (secondStageInDegree == firstStageRREdgeNeighborhood.size()){
            for (int i = 0; i < secondStageInDegree; i++) {
                defectRREdges.addAll(firstStageRREdgeNeighborhood.get(i));
            }
        } else {
            System.err.println("Error in Mux-Calculation: secondStageInDegree in not equal to firstStageRREdgeNeighborhood.size()\n" +
                    "firstStageNeigborhoods had been changed somewhere");
            System.exit(-1);
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

    public int getNumberOfFaultyMemristors(){
        int faultyMemristors = 0;
        for (SwitchTree switchTree : firstStageNeighborhoods) {
            faultyMemristors += switchTree.getNumOfMemristorFaults();
        }
        faultyMemristors += secondStageNeighborhood.getNumOfMemristorFaults();
        return faultyMemristors;
    }

}
