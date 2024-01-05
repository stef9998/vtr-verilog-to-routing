import java.util.*;
import org.jgrapht.*;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * @author Lukas Freiberger
 */
public class MUXLukas {

    final int switchID, muxSize, blockSize;                                     // Index of sink node, ID of the switch, Number of source nodes, Maximum size of one input block
    final HashMap<Integer, RRNodeType> srcNodeTypes = new HashMap<>();          // Hash Map with Source Node Types
    final RRNodeType sinkRRNodeType;                                            // Node Type of Sink Node

    final Vertex sinkVertex;                                                    // Sink Vertex of the Graph
    ArrayList<Vertex> srcVertices = new ArrayList<>();                          // Source Vertex of the Graph
    Graph<Vertex, Switch> muxGraph = new SimpleDirectedGraph<>(Switch.class);   // Graph containing nodes and edges representing the MUX architecture
    final Switch[] frstStageSwitches;                                           // switches in first stage
    final Switch[] scndStageSwitches;                                           // switches in second stage

    ArrayList<GraphPath<Vertex, Switch>> defectPaths = new ArrayList<>();       // array list of defect paths/edges in the mux

    // Constructor
    public MUXLukas(ArrayList<RREdge> rrEdges, FaultRates faultRates){
        // Set switchID, muxSize and maxBlockSize
        this.switchID = rrEdges.get(0).getSwitchID();
        this.muxSize = rrEdges.size();
        this.blockSize = calcBlockSize();

        // Sink and Source Node Types for print out
        this.sinkRRNodeType = rrEdges.get(0).getSinkNodeType();

        for (RREdge rrEdge : rrEdges) {
            this.srcNodeTypes.put(rrEdge.getNodeID(), rrEdge.getNodeType());
        }


        /* ---------------------------------------- MUX GRAPH ---------------------------------------------- */

        // Instantiate a new object of class Node to represent the sink node
        this.sinkVertex = new Vertex(rrEdges.get(0).getSinkNodeID(), VertexType.SINKNODE, -1);

        // Add the sink Node to the graph
        muxGraph.addVertex(sinkVertex);

        // Fill the array with source nodes and add them to the graph
        int i = 0;
        for(RREdge node: rrEdges){
            srcVertices.add(new Vertex(node.getNodeID(), VertexType.SRCNODE, node.getRRIndex()));
            muxGraph.addVertex(srcVertices.get(i));
            i++;
        }

        // Instantiate an array of Nodes containing the inter nodes
        Vertex[] intVertices = new Vertex[muxSize <= blockSize ? 0 : (muxSize/ blockSize) + (muxSize% blockSize != 0 ? 1 : 0)];
        // Instantiate an array of Switches to represent the edges between the sink and the inter nodes
        scndStageSwitches = new Switch[intVertices.length];
        // fill the array with Nodes and Switches and add them to the graph
        for (int j = 0; j < intVertices.length; j++){
            intVertices[j] = new Vertex(j, VertexType.INTERNODE, -1);
            scndStageSwitches[j] = new Switch(new MemCell2T2R(faultRates));
            muxGraph.addVertex(intVertices[j]);
            muxGraph.addEdge(intVertices[j], sinkVertex, scndStageSwitches[j]);
        }

        // Instantiate an array of MemCells to control the edges between the source and the inter nodes
        MemCell[] frstStageCells = new MemCell[Math.min(muxSize, blockSize)];
        // fill the array with MemCells
        for (int cellIndex = 0; cellIndex < frstStageCells.length; cellIndex ++){
            frstStageCells[cellIndex] = new MemCell2T2R(faultRates);
        }

        // Instantiate an array of Switches to represent the edges between the inter and the source nodes
        frstStageSwitches = new Switch[muxSize];
        // if muxSize is lower than maxBlockSize
        if(muxSize <= blockSize) {
            // Connect the Source Nodes with the Sink Node
            int k = 0;
            for (Vertex srcVertex: srcVertices){
                frstStageSwitches[k] = new Switch(frstStageCells[k]);
                muxGraph.addEdge(srcVertex, sinkVertex, frstStageSwitches[k]);
                k ++;
            }
        } else{
            // Connect the Source Nodes to the Inter Nodes
            int l = 0;
            for (Vertex srcVertex: srcVertices) {
                frstStageSwitches[l] = new Switch(frstStageCells[l % blockSize]);
                muxGraph.addEdge(srcVertex, intVertices[l / blockSize], frstStageSwitches[l]);
                l ++;
            }
        }
    }

    public void calculateUsability(){

        // Lists of paths in the graph
        ArrayList<GraphPath<Vertex, Switch>> completeBlockPaths = new ArrayList<>();
        ArrayList<GraphPath<Vertex, Switch>> incompleteBlockPaths = new ArrayList<>();
        AllDirectedPaths<Vertex, Switch> paths = new AllDirectedPaths<>(muxGraph);

        // fill complete block paths list
        for (Vertex srcVertex : srcVertices) {
            completeBlockPaths.addAll(paths.getAllPaths(srcVertex, sinkVertex, true, 2));
        }

        // check MUX's depth (if true, mux has two stages)
        boolean depth = completeBlockPaths.get(0).getEdgeList().size() == 2;

        // check if MUX contains incomplete block
        int rest = muxSize % blockSize;

        // add paths of incomplete block paths to list and remove them from complete block paths list
        int lastPathIndex = completeBlockPaths.size() - 1;
        for (int index = lastPathIndex; index > lastPathIndex - rest; index--) {
            incompleteBlockPaths.add(completeBlockPaths.get(index));
            completeBlockPaths.remove(index);
        }

        // count number of SA1 or UD faults in sink stage of complete block paths
        int numOfSA1Snk = 0, numOfUDSnk = 0;                                    // number of SA1 and UD in sink stage of complete block paths
        Fault sinkFault;                                                        // the sink fault type
        int sinkEdgeInd = completeBlockPaths.get(0).getEdgeList().size() - 1;   // index of sink edge
        for (int i = 0; i < completeBlockPaths.size(); i = i + (depth ? blockSize : 1)){
            sinkFault = completeBlockPaths.get(i).getEdgeList().get(sinkEdgeInd).getFault();
            if (sinkFault == Fault.SA1){
                numOfSA1Snk++;
            } else if (sinkFault == Fault.UD){
                numOfUDSnk++;
            }
        }

        // if MUX has two stages count number of SA1 or UD faults in source stage od complete block paths
        int numOfSA1Src = 0, numOfUDSrc = 0;                                    // number of SA1 and UD in source stage of complete block paths
        if (depth){
            Fault sourceFault;                                                  // the source fault type
            for (int i = 0; i < blockSize; i++){
                sourceFault = completeBlockPaths.get(i).getEdgeList().get(0).getFault();
                if (sourceFault == Fault.SA1){
                    numOfSA1Src++;
                } else if (sourceFault == Fault.UD){
                    numOfUDSrc++;
                }
            }
        }

        // get sink fault of incomplete block paths
        Fault snkFaultIncomPaths;                                                // the sink fault type of incomplete block paths
        if (!incompleteBlockPaths.isEmpty()) {
            snkFaultIncomPaths = incompleteBlockPaths.get(0).getEdgeList().get(sinkEdgeInd).getFault();
        } else {
            snkFaultIncomPaths = Fault.SA0;
        }

        // count the number of SA1 and UD in src stage of the incomplete block paths
        int numOfSA1SrcIncomPaths = 0, numOfUDSrcIncomPaths = 0;                   // number of SA1 and UD in source stage of incomplete block paths
        for (GraphPath<Vertex, Switch> path: incompleteBlockPaths){
            if (path.getEdgeList().get(0).getFault() == Fault.SA1){
                numOfSA1SrcIncomPaths++;
            } else if (path.getEdgeList().get(0).getFault() == Fault.UD){
                numOfUDSrcIncomPaths++;
            }
        }

        // catch cases, where the mux isn't usable anymore
        if (numOfSA1Snk > 1 || numOfUDSnk > 0 || numOfUDSrcIncomPaths > 0 || numOfSA1SrcIncomPaths > 1
                || (numOfSA1Snk == 1 && (numOfSA1Src > 1 || numOfUDSrc > 0))
                || (snkFaultIncomPaths == Fault.UD && (numOfSA1SrcIncomPaths > 0))
                || (snkFaultIncomPaths == Fault.UD && (numOfUDSrc > 0 || numOfSA1Src > 1))
                || (snkFaultIncomPaths == Fault.SA1 && numOfSA1SrcIncomPaths == 1 && numOfSA1Snk == 1)){
            defectPaths.addAll(completeBlockPaths);
            defectPaths.addAll(incompleteBlockPaths);
        }

        /*
         * If the sink of the complete block paths contains exactly one SA1-Fault it's not possible, that there is an UD-Fault
         * and more than one SA1-Fault in source of the complete block paths. Otherwise the MUX isn't usable anymore.
         */
        else if (numOfSA1Snk == 1){

            // add all incomplete block paths to the defect path list
            defectPaths.addAll(incompleteBlockPaths);

            /*
             * If the sink of the incomplete block paths contains an UD-Fault it's not possible, that there is a SA1-Fault in source of
             * the incomplete block paths and more than one SA1-Fault in source of the complete block paths. It's also not possible, that
             * there is any UD-Fault in source of the complete block paths or the incomplete block paths.
             */

            if (snkFaultIncomPaths == Fault.UD){
                // if the source of complete block paths contains one SA1-Fault
                if (numOfSA1Src == 1){
                    // iterate over complete block paths and add those paths to the defect paths which are not the SA1-path
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (!(path.getEdgeList().get(0).getFault() == Fault.SA1 && path.getEdgeList().get(sinkEdgeInd).getFault() == Fault.SA1)){
                            defectPaths.add(path);
                        }
                    }
                }
                // else the number of SA1-Faults in source is zero
                else{
                    // iterate over the complete block paths list and add those paths to the defect paths which not contain an SA1-Fault in sink
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths) {
                        if (path.getEdgeList().get(sinkEdgeInd).getFault() != Fault.SA1){
                            defectPaths.add(path);
                        }
                    }
                    // iterate over the complete block paths list and add those paths to the defect paths which share a switch with any of the rest paths
                    for (int i = 0; i < incompleteBlockPaths.size(); i++) {
                        for (int j = i; j < completeBlockPaths.size(); j = j + blockSize) {
                            if (!defectPaths.contains(completeBlockPaths.get(j))) {
                                defectPaths.add(completeBlockPaths.get(j));
                            }
                        }
                    }
                }
            }

            /*
             * The sink of the incomplete block paths contains a SA1-Fault. Besides that it is not possible that there is one
             * UD-Fault or more than one SA1-Fault in source of the incomplete block paths.
             */
            else if (snkFaultIncomPaths == Fault.SA1){
                // If the source of the complete block paths contains no SA1-Fault the source of the incomplete block paths also contains no SA1-Fault
                if (numOfSA1Src == 0){
                    // iterate over the complete paths and add those paths to defect paths which does not have a SA1-Fault in sink
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (path.getEdgeList().get(sinkEdgeInd).getFault() != Fault.SA1){
                            defectPaths.add(path);
                        }
                    }
                    // iterate over the complete block paths and add those paths to the defect paths which share a switch with any of the incomplete block paths
                    for (int i = 0; i < incompleteBlockPaths.size(); i++) {
                        for (int j = i; j < completeBlockPaths.size(); j = j + blockSize) {
                            if (!defectPaths.contains(completeBlockPaths.get(j))) {
                                defectPaths.add(completeBlockPaths.get(j));
                            }
                        }
                    }
                }
                // else if the source of the complete block paths contains one SA1-Fault and the incomplete block paths do not contain a SA1-Fault in source
                else if (numOfSA1Src == 1 && numOfSA1SrcIncomPaths == 0){
                    // iterate over the complete block paths list and add those paths to defect paths which are not the SA1 path
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (!(path.getEdgeList().get(sinkEdgeInd).getFault() == Fault.SA1 && path.getEdgeList().get(0).getFault() == Fault.SA1)){
                            defectPaths.add(path);
                        }
                    }
                }
            }

            /*
             * The sink of the incomplete block paths is fault-free. Besides that it is not possible that the source of the incomplete block paths
             * contains an UD-Fault or more than one SA1-Fault.
             */
            else if (snkFaultIncomPaths == Fault.FF){
                // if the source of the incomplete block paths contains exactly one SA1-Fault and with that the source of the complete block paths
                if (numOfSA1SrcIncomPaths == 1){
                    // iterate over complete block paths list and add those paths to defect paths which are not the SA1 path
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (!(path.getEdgeList().get(sinkEdgeInd).getFault() == Fault.SA1 && path.getEdgeList().get(0).getFault() == Fault.SA1)){
                            defectPaths.add(path);
                        }
                    }
                }
                // else if the source of the complete block paths contains exactly one SA1-Fault but the source of the incomplete block paths does not contain a SA1-Fault
                else if (numOfSA1Src == 1 && numOfSA1SrcIncomPaths == 0){
                    // iterate over complete block paths list and add those paths to defect paths which are not the SA1 path
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (!(path.getEdgeList().get(sinkEdgeInd).getFault() == Fault.SA1 && path.getEdgeList().get(0).getFault() == Fault.SA1)){
                            defectPaths.add(path);
                        }
                    }
                }
                // else if the source of complete block paths does not contain any SA1-Fault and with that also the source of incomplete block paths
                else if (numOfSA1Src == 0){
                    // iterate over complete block paths list and add those paths to defect paths which do not contain a SA1-Fault in the sink
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (path.getEdgeList().get(sinkEdgeInd).getFault() != Fault.SA1){
                            defectPaths.add(path);
                        }
                    }
                }
            }

            /*
             * The sink of the incomplete block paths contains a SA0-Fault. For this we do not have to look at the source faults of
             * these paths.
             */
            else if (snkFaultIncomPaths == Fault.SA0){
                // if the source of the complete block paths contains exactly one SA1-Fault
                if (numOfSA1Src == 1){
                    // iterate over complete block paths and add those paths to defect paths which are not the SA1 path
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (!(path.getEdgeList().get(sinkEdgeInd).getFault() == Fault.SA1 && path.getEdgeList().get(0).getFault() == Fault.SA1)){
                            defectPaths.add(path);
                        }
                    }
                }
                // else if there is no SA1 in source of the complete block paths
                else {
                    // iterate over the complete block paths list and add those paths to defect paths which does not have a SA1-Fault in sink
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (path.getEdgeList().get(sinkEdgeInd).getFault() != Fault.SA1){
                            defectPaths.add(path);
                        }
                    }
                }
            }
        }

        /*
         * Else if the sink of the complete block paths contains no UD- or SA1-Fault, it is only possible that there is a
         * SA0-Fault or the sink is fault-free. For the following block we think
         * of it as fault-free and if it contains a SA0, we add those paths later to the defect path list, because they
         * do not have any effects on other paths.
         */
        else{
            /*
             * If the sink of the incomplete block paths contains a UD-Fault it is not possible, that there is a SA1- or UD-Fault
             * in source of these paths. It is also not possible that there is a UD-Fault or more than one SA1-Fault
             * in the source of the complete block paths.
             */
            if (snkFaultIncomPaths == Fault.UD){
                // add all incomplete block paths to the defect paths list
                defectPaths.addAll(incompleteBlockPaths);

                // if there is exactly one SA1-Fault in source of complete block paths
                if (numOfSA1Src == 1){
                    // iterate over complete block paths and add those paths to defect paths which not contain a SA1-Fault in source
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (path.getEdgeList().get(0).getFault() != Fault.SA1){
                            defectPaths.add(path);
                        }
                    }
                }
                // else if there is no SA1-Fault in source of the complete block paths
                else {
                    // iterate over complete block paths and add those paths to defect paths which share a switch with any of the incomplete block paths
                    for (int i = 0; i < incompleteBlockPaths.size(); i++) {
                        for (int j = i; j < completeBlockPaths.size(); j = j + blockSize) {
                            defectPaths.add(completeBlockPaths.get(j));
                        }
                    }
                }
            }

            /*
             * The sink of the incomplete block paths contains a SA1-Fault. Besides that it is not possible, that there is a UD-Fault
             * or more than one SA1-Fault in source of these paths.
             */
            else if (snkFaultIncomPaths == Fault.SA1){
                // if there is exactly one SA1-Fault in source of the incomplete block paths
                if (numOfSA1SrcIncomPaths == 1){
                    // add all paths of complete block paths to the defect paths
                    defectPaths.addAll(completeBlockPaths);

                    // iterate over the incomplete block paths and add those paths to defect paths which does not have the SA1-Fault in source
                    for (GraphPath<Vertex, Switch> path: incompleteBlockPaths){
                        if (path.getEdgeList().get(0).getFault() != Fault.SA1){
                            defectPaths.add(path);
                        }
                    }
                }
                // else if there is no SA1-Fault in source of the incomplete block paths
                else {
                    // if there is a UD-Fault or more than one SA1-Fault in source of the complete block paths
                    if (numOfUDSrc > 0 || numOfSA1Src > 1){
                        // add all paths of complete block paths list to defect paths
                        defectPaths.addAll(completeBlockPaths);
                    }
                    // else if there is exactly one SA1-Fault in source of the complete block paths
                    else if (numOfSA1Src == 1){
                        // iterate over complete block paths and add those paths to defect paths which do not contain a SA1-Fault in source
                        for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                            if (path.getEdgeList().get(0).getFault() != Fault.SA1){
                                defectPaths.add(path);
                            }
                        }
                    }
                    // else if there is no SA1- or UD-Fault in source of the complete block paths
                    else {
                        // iterate over complete block paths and add those paths to defect paths which share a switch to any of the incomplete block paths
                        for (int i = 0; i < incompleteBlockPaths.size(); i++) {
                            for (int j = i; j < completeBlockPaths.size(); j = j + blockSize) {
                                defectPaths.add(completeBlockPaths.get(j));
                            }
                        }
                    }
                }
            }

            /*
             * The sink of the incomplete block paths is fault-free.
             */
            else if (snkFaultIncomPaths == Fault.FF){
                // if there is exactly one SA1-Fault in source of the incomplete block paths and within exactly one SA1-Fault in source of the complete block paths
                if (numOfSA1SrcIncomPaths == 1) {
                    // iterate over incomplete block paths and add those paths to defect paths list which have no SA1-Fault in source
                    for (GraphPath<Vertex, Switch> path : incompleteBlockPaths) {
                        if (path.getEdgeList().get(0).getFault() != Fault.SA1) {
                            defectPaths.add(path);
                        }
                    }
                }
                // if there is a UD-Fault or more than one SA1-Fault in source of complete block paths
                if (numOfUDSrc > 0 || numOfSA1Src > 1){
                    // add all complete block paths to defect paths list
                    defectPaths.addAll(completeBlockPaths);
                }
                // else if there is exactly one SA1-Fault in source of complete block paths
                else if (numOfSA1Src == 1){
                    // iterate over complete block paths and add those paths to defect paths list which do not have a SA1-Fault in source
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (path.getEdgeList().get(0).getFault() != Fault.SA1){
                            defectPaths.add(path);
                        }
                    }
                }
            }

            /*
             * The sink of the incomplete block paths contains a SA0-Fault. In this case all rest paths can be ignored and added to
             * the defect paths list.
             */
            else if (snkFaultIncomPaths == Fault.SA0){
                // add all incomplete block paths to defect path list
                defectPaths.addAll(incompleteBlockPaths);

                // if there is a UD-Fault or more than one SA1-Fault in source of the complete block paths
                if (numOfUDSrc > 0 || numOfSA1Src > 1){
                    // add all complete block paths to defect path list
                    defectPaths.addAll(completeBlockPaths);
                }
                // else is there is exactly one SA1-Fault in source of complete block paths
                else if (numOfSA1Src == 1){
                    // iterate over complete block paths and add those paths to defect paths list which do not contain a SA1-Fault in source
                    for (GraphPath<Vertex, Switch> path: completeBlockPaths){
                        if (path.getEdgeList().get(0).getFault() != Fault.SA1){
                            defectPaths.add(path);
                        }
                    }
                }
            }
        }

        // iterate over complete block paths and add all paths to defect path list which contain a SA0-Fault and are not already in defect paths list
        for (GraphPath<Vertex, Switch> path: completeBlockPaths){
            if ((path.getEdgeList().get(0).getFault() == Fault.SA0 || path.getEdgeList().get(sinkEdgeInd).getFault() == Fault.SA0)
                    && !defectPaths.contains(path)){
                defectPaths.add(path);
            }
        }

        // iterate over incomplete block paths and add all paths to defect path list which contain a SA0-Fault and are not already in defect paths list
        for (GraphPath<Vertex, Switch> path: incompleteBlockPaths){
            if ((path.getEdgeList().get(0).getFault() == Fault.SA0 || path.getEdgeList().get(sinkEdgeInd).getFault() == Fault.SA0)
                    && !defectPaths.contains(path)){
                defectPaths.add(path);
            }
        }
    }

    /**
     * returns the list with defect paths
     * @return [sourceID, sinkID, rrIndex]
     */
    public ArrayList<int[]> getRREdgeDeleteList(){
        ArrayList<int[]> edges = new ArrayList<>();

        // extract relevant information for XMLReader/Writer
        for (GraphPath<Vertex, Switch> path: defectPaths){
            edges.add(new int[]{path.getStartVertex().getVertexID(), path.getEndVertex().getVertexID(), path.getStartVertex().getRRIndex()});
        }
        return edges;
    }

    // calculates the block size such that number of mem cells is minimal
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
     * returns a readable representation of the MUX Graph
     * @return readable representation of the MUX Graph
     */
    public String printGraph(){
        StringBuilder out = new StringBuilder();

        // print graph in String Builder
        Set<Switch> edges = muxGraph.edgeSet();
        for(Switch edge: edges){
            out.append("Edge Source: (").append(muxGraph.getEdgeSource(edge).getVertexID()).append(", ").append(muxGraph.getEdgeSource(edge).getVertexType()).append("); Fault: ").append(edge.getFault()).append("; Edge Target: (").append(muxGraph.getEdgeTarget(edge).getVertexID()).append(", ").append(muxGraph.getEdgeTarget(edge).getVertexType()).append(")\n");
        }
        return out.toString();
    }

    /**
     * Returns some statistics about the multiplexer as a String.
     * For printout and logging
     *
     * @return statistics about the multiplexer
     */
    public String printStats(){
        // general information on MUX
        String out = ("Mux Size: " + muxSize + " Inputs\nSink Node: " + sinkVertex.getVertexID() + "\nSwitch ID: " + switchID);

        // num of mem cells
        int numMemCells = getNumberOfMemCells();
        out += ("\nNumber of MemCells: " + numMemCells);

        // num of faults SA0, SA1 and UD
        int[] faults = getNumberOfFaults();
        out += ("\nNumber of Faults:\nNum. of UD: " + faults[2] + " Faults\nNum. of SA1: " + faults[1] + " Faults\nNum. of SA0: " + faults[0] + " Faults\n");

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
        return defectPaths.size();
    }

    /**
     * returns the number of mem cells used by the MUX
     * @return number of mem cells used by the MUX
     */
    public int getNumberOfMemCells(){
        return (muxSize > blockSize) ? ((muxSize/blockSize) + ((muxSize % blockSize != 0) ? 1 : 0) + blockSize) : muxSize;
    }

    /**
     * returns number of the faults SA0, SA1 and UD
     * @return [numOfSA0, numOfSA1, numOfUD]
     */
    public int[] getNumberOfFaults(){
        // how much switches contain UD, SA1 or SA0
        int numUD= 0, numSA1 = 0, numSA0 = 0;

        // if mux size is bigger than block size
        if(muxSize > blockSize) {
            // count number of faults in first stage
            for (int i = 0; i < blockSize; i++) {
                if (frstStageSwitches[i].getFault() == Fault.UD) {
                    numUD++;
                } else if (frstStageSwitches[i].getFault() == Fault.SA1) {
                    numSA1++;
                } else if (frstStageSwitches[i].getFault() == Fault.SA0) {
                    numSA0++;
                }
            }

            // count number of faults in second stage
            for (Switch scndStageSwitch : scndStageSwitches) {
                if (scndStageSwitch.getFault() == Fault.UD) {
                    numUD++;
                } else if (scndStageSwitch.getFault() == Fault.SA1) {
                    numSA1++;
                } else if (scndStageSwitch.getFault() == Fault.SA0) {
                    numSA0++;
                }
            }
        }
        // else if mux has only one stage
        else {
            // count number of faults in first stage
            for (Switch frstStageSwitch : frstStageSwitches) {
                if (frstStageSwitch.getFault() == Fault.UD) {
                    numUD++;
                } else if (frstStageSwitch.getFault() == Fault.SA1) {
                    numSA1++;
                } else if (frstStageSwitch.getFault() == Fault.SA0) {
                    numSA0++;
                }
            }
        }

        return new int[]{numSA0, numSA1, numUD};
    }

    /**
     * returns the number of resistors with a fault
     * @return number of resistors with a fault
     */
    public int getNumberOfFaultyMemristors(){
        // how much switches contain UD, SA1 or SA0
        int numFault = 0;

        // if mux size is bigger than block size
        if(muxSize > blockSize) {
            // count number of faults in first stage
            for (int i = 0; i < blockSize; i++) {
                if (((MemCell2T2R) frstStageSwitches[i].getControlCell()).puResContFault()) {
                    numFault++;
                }
                if (((MemCell2T2R) frstStageSwitches[i].getControlCell()).pdResContFault()) {
                    numFault++;
                }
            }

            // count number of faults in second stage
            for (Switch scndStageSwitch : scndStageSwitches) {
                if (((MemCell2T2R) scndStageSwitch.getControlCell()).puResContFault()) {
                    numFault++;
                }
                if (((MemCell2T2R) scndStageSwitch.getControlCell()).pdResContFault()) {
                    numFault++;
                }
            }
        }
        // else if mux has only one stage
        else {
            // count number of faults in first stage
            for (Switch frstStageSwitch : frstStageSwitches) {
                if (((MemCell2T2R) frstStageSwitch.getControlCell()).puResContFault()) {
                    numFault++;
                }
                if (((MemCell2T2R) frstStageSwitch.getControlCell()).pdResContFault()) {
                    numFault++;
                }
            }
        }

        return numFault;
    }
}
