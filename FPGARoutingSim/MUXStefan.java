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

    private int numOfFaultyMemristors;
    // Faults of Switches/MemoryCells
    private int numOfFF, numOfSA0, numOfSA1, numOfUD;

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
        fillSwitchesInfo();
    }

    private void fillSwitchesInfo(){
        int faultyMemristors = 0;
        int numOfFF = 0, numOfSA0 = 0, numOfSA1 = 0, numOfUD = 0;
        for (SwitchTree switchTree : firstStageNeighborhoods) {
            faultyMemristors += switchTree.getNumOfMemristorFaults();
            numOfFF  += switchTree.getNumOfFFFaults();
            numOfSA0 += switchTree.getNumOfSA0Faults();
            numOfSA1 += switchTree.getNumOfSA1Faults();
            numOfUD  += switchTree.getNumOfUDFaults();
        }
        faultyMemristors += secondStageNeighborhood.getNumOfMemristorFaults();
        numOfFF  += secondStageNeighborhood.getNumOfFFFaults();
        numOfSA0 += secondStageNeighborhood.getNumOfSA0Faults();
        numOfSA1 += secondStageNeighborhood.getNumOfSA1Faults();
        numOfUD  += secondStageNeighborhood.getNumOfUDFaults();

        this.numOfFaultyMemristors = faultyMemristors;
        this.numOfFF  = numOfFF;
        this.numOfSA0 = numOfSA0;
        this.numOfSA1 = numOfSA1;
        this.numOfUD  = numOfUD;
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
    //TODO change naming maybe, as I am not using a graph anymore
    // maybe leave for backwards compatibility. But make it @deprecated?
    /**
     * returns a readable representation of the MUX Graph
     * @return representation of the MUX Graph
     */
    public String printGraph(){
        // last sourceID is the biggest number, so also the longest. This length is used for padding the printout
        int lastSourceID = firstStageNeighborhoods.get(secondStageInDegree-1).getRREdge(firstStageNeighborhoods.get(secondStageInDegree-1).getNumOfSwitches()-1).getNodeID();
        final int srcIDStrLen = Integer.toString(lastSourceID).length();

        StringBuilder out = new StringBuilder();

        // print graph in String Builder
        for (int i = 0; i < secondStageInDegree; i++) {
            SwitchTree firstStageTree = firstStageNeighborhoods.get(i);
            out.append(String.format("%" + (15 + srcIDStrLen) + "s", "")).append("-----").append("\n");
            for (int j = 0; j < firstStageTree.getNumOfSwitches(); j++) {
                out.append("(")
                        .append(String.format("%" + srcIDStrLen + "s", firstStageTree.getRREdge(j).getNodeID()))
                        .append(", SRCNODE) --| ")
                        .append(String.format("%-" + 3 + "s", firstStageTree.getFault(j)))
                        .append(" |\n");
            }
            out.deleteCharAt(out.length()-1);
            out.append("-- (Link")
                    .append(String.format("%" + 2 + "s", i))
                    .append(")\n");
        }
        out.append(String.format("%" + (15 + srcIDStrLen) + "s", "")).append("-----").append("\n");
        out.append(String.format("%" + (12) + "s", "")).append("-----\n");
        for (int i = 0; i < secondStageInDegree; i++) {
            out.append("(Link")
                    .append(String.format("%" + 2 + "s", i))
                    .append(") --| ")
                    .append(String.format("%-" + 3 + "s", secondStageNeighborhood.getFault(i)))
                    .append(" |\n");
        }
        out.deleteCharAt(out.length()-1);
        out.append("-- (").append(sinkNodeID).append(", SINKNODE)\n");
        out.append(String.format("%" + (12) + "s", "")).append("-----\n");
        return out.toString();
    }

    /**
     * prints some statistics about the multiplexer
     * @return statistics about the multiplexer
     */
    public String printStats(){
        // general information on MUX
        StringBuilder out = new StringBuilder();
        out.append("Mux Size: ").append(muxSize).append(" Inputs\n")
                .append("Sink Node: ").append(sinkNodeID).append("\n")
                .append("Switch ID: ").append(switchID).append("\n")
                .append("Number of MemCells: ").append(getNumberOfMemCells()).append("\n")
                .append("Number of Faults:\n")
                .append("Num. of SA0: ").append(numOfSA0).append(" Faults\n")
                .append("Num. of SA1: ").append(numOfSA1).append(" Faults\n")
                .append("Num. of UD:  ").append(numOfUD).append(" Faults\n");
        return out.toString();
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
        return numOfFaultyMemristors;
    }

    /**
     * returns number of the individual faults SA0, SA1 and UD as an array
     * @return [numOfSA0, numOfSA1, numOfUD]
     */
    public int[] getNumberOfFaultsPerType(){
        return new int[]{getNumOfSA0(), getNumOfSA1(), getNumOfUD()};
    }
    public int getNumOfFF() {
        return numOfFF;
    }

    public int getNumOfSA0() {
        return numOfSA0;
    }

    public int getNumOfSA1() {
        return numOfSA1;
    }

    public int getNumOfUD() {
        return numOfUD;
    }
}
