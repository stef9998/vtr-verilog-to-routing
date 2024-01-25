import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class FPGARoutingSim {
    /* CODE_SELECTOR
    Used to select which Code should run
        0 = Lukas
        1 = Stefan
     */
    static final int CODE_SELECTOR = 1;
    public static final boolean RUN_LUKAS_CODE;
    public static final boolean RUN_STEFAN_CODE;

    static {
        RUN_LUKAS_CODE = (CODE_SELECTOR == 0);
        RUN_STEFAN_CODE = (CODE_SELECTOR == 1);
    }

    public static final int VERBOSITY = 2;
    public static final boolean QUIET = false;

    static ArrayList<MUX> muxes = new ArrayList<>();              // array list containing the muxes
    static ArrayList<int[]> defectEdges = new ArrayList<>();            // array list containing the defect edges
    static int[][] deleteList;                                          // array containing the delete list with defect edges
    static XMLReader reader = new XMLReader();                          // object of class XMLReader
    static FaultRates faultRates;                                       // object of class FaultRates

    public static void main(String[] args) {
        final long timeStart = System.currentTimeMillis();              // start time of simulation
        int numOfEdges = 0, numOfDefectEdges = 0, numOfMemCells = 0, numOfFaultyMemristors = 0;    // total number of Edges, defect Edges and MemCells to configure Muxes in FPGA
        int numOfSA0Faults = 0, numOfSA1Faults = 0, numOfUDFaults = 0;        // total number of faults in MemCells to configure Muxes of FPGA
        StringBuilder output = new StringBuilder();                     // String Builder for program output

        // get file name, read file and instantiate fault rates
        String file = args[0];
        reader.readXML(file);
        faultRates = new FaultRates( new double[]{ Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]) } );

        if (!QUIET)
            System.out.println("Edges read");

        fillMuxArray();
        if (!QUIET)
            System.out.println("MUX Array filled");

        for (MUX mux : muxes){
            defectEdges.addAll(mux.getDefectRREdgesList());

            // increase the overall number of edges, defect edges and memory cells
            numOfEdges += mux.getNumberOfEdges();
            numOfDefectEdges += mux.getNumOfDefectEdges();
            numOfMemCells += mux.getNumberOfMemCells();
            numOfFaultyMemristors += mux.getNumberOfFaultyMemristors();

            // increase the overall number of SA0, SA1 and UD
            numOfSA0Faults += mux.getNumOfSA0();
            numOfSA1Faults += mux.getNumOfSA1();
            numOfUDFaults  += mux.getNumOfUD();
        }

        if (VERBOSITY >= 1) {
        output.append("--- MUX Information -------------------------------------------------------------------------\n");
            for (int i = 0; i < muxes.size(); i++) {
                output.append("--- MUX No. ").append(i).append(" begin printout -----------------------------------------------------------\n");
                output.append(muxes.get(i).printStats()).append("\n");
                if (VERBOSITY >= 2) {
                    output.append(muxes.get(i).printGraph());
                }
                output.append("--------------------------------------------------------------------------------------------\n");
            }
        } //TODO output string: might run out of heap space for big circuits / rr-graphs

        if (!QUIET)
            System.out.println("MUX Usabilities calculated");

        // List to Array //TODO might be possible to remove this intermediate step
        deleteList = defectEdges.toArray(new int[defectEdges.size()][3]);

        // write data into xml file
        reader.writeXML(deleteList);
        reader.finalizeWriting();

        if (!QUIET)
            System.out.println("XML written");

        if (!QUIET)
            System.out.println("\nFPGARoutingSim ran successfully!\n"); // can probably be deleted as is redundant if next output will be printed out

        // add some overall output data
        output.append("Circuit has ").append(muxes.size()).append(" Multiplexers\n");
        output.append("Number of configurable Edges in FPGA: ").append(numOfEdges).append("\n");
        output.append("Number of defect Edges in FPGA after Fault Sim: ").append(numOfDefectEdges).append("\n");
        output.append("Number of Memory Cells to set the Edges in FPGA: ").append(numOfMemCells).append("\n");
        output.append("Number of SA0 in Memory Cells: ").append(numOfSA0Faults).append("\n");
        output.append("Number of SA1 in Memory Cells: ").append(numOfSA1Faults).append("\n");
        output.append("Number of UD in Memory Cells: ").append(numOfUDFaults).append("\n");
        output.append("Number of faulty Memristors: ").append(numOfFaultyMemristors).append("\n");
        output.append("Number of total faults in Memory Cells: ").append(numOfSA0Faults + numOfSA1Faults + numOfUDFaults).append("\n");
        // set end time of the simulation and append overall time to output
        final long timeEnd = System.currentTimeMillis();
        output.append("Needed ").append((timeEnd - timeStart)/1000).append(" s");

        // print output of the simulation
        System.out.print(output);
    }

    // fills the array list of muxes with muxes created by information out of the rr-graph
    private static void fillMuxArray(){
        // Array containing all Nodes from Routing Resource Graph
        RREdge[] rrEdges = reader.getRrNodes();
        HashMap<Integer, SwitchType> switchTypes = reader.getSwitchTypes();

        ArrayList<RREdge> muxSrcNodes = new ArrayList<>();      // ArrayList containing all Source Nodes of a MUX and
        RREdge lastNode = rrEdges[0];                           // last Node read of the Array

        // Iterate over Array of all Nodes
        for (RREdge rrEdge : rrEdges){
            // If Sink Node of actual Node is not the same as Sink Node of the last Node
            if (rrEdge.getSinkNodeID() != lastNode.getSinkNodeID()) {
                // If Array List does not contain Nodes of RRNodeType SOURCE and those with Sink RRNodeType SINK
                if (muxSrcNodes.get(0).getNodeType() != RRNodeType.SOURCE && muxSrcNodes.get(0).getSinkNodeType() != RRNodeType.SINK
                        && switchTypes.get(muxSrcNodes.get(0).getSwitchID()) == SwitchType.mux) {
                    switch (CODE_SELECTOR) {
                        case 0:
                            muxes.add(new MUXLukas(muxSrcNodes, faultRates));          // Add new MUX
                            break;
                        case 1:
                            muxes.add(new MUXStefan(muxSrcNodes, faultRates));          // Add new MUX
                            break;
                        default:
                            System.err.println("One of the mux types needs to be selected.");
                            System.exit(-1);
                    }
                }
                // Clear the List of MUX Source Nodes
                muxSrcNodes.clear();
            }

            // add Node to ArrayList for MUX Source Nodes and refresh last Node
            muxSrcNodes.add(rrEdge);
            lastNode = rrEdge;
        }
    }
}
