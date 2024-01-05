import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    static ArrayList<MUXLukas> muxesLukas = new ArrayList<>();          // array list containing the muxes
    static ArrayList<MUXStefan> muxes = new ArrayList<>();              // array list containing the muxes
    static ArrayList<int[]> defectEdges = new ArrayList<>();            // array list containing the defect edges
    static ArrayList<int[]> defectEdgesStefan = new ArrayList<>();            // array list containing the defect edges
    static int[][] deleteList;                                          // array containing the delete list with defect edges
    static int[][] deleteListStefan;                                          // array containing the delete list with defect edges
    static XMLReader reader = new XMLReader();                          // object of class XMLReader
    static FaultRates faultRates;                                       // object of class FaultRates

    public static void main(String[] args) {
        final long timeStart = System.currentTimeMillis();              // start time of simulation
        int numOfEdges = 0, numOfDefectEdges = 0, numOfMemCells = 0, numOfFaultyMemristors = 0;    // total number of Edges, defect Edges and MemCells to configure Muxes in FPGA
        int numOfEdgesStefan = 0, numOfDefectEdgesStefan = 0, numOfMemCellsStefan = 0, numOfFaultyMemristorsStefan = 0;    // total number of Edges, defect Edges and MemCells to configure Muxes in FPGA
        int[] numOfFaults = new int[]{0, 0, 0};                         // total number of faults in order [SA0, SA1, UD] in MemCells to configure Muxes of FPGA
        StringBuilder output = new StringBuilder();                     // String Builder for program output
        int[] faults;

        // get file name, read file and instantiate fault rates
        String file = args[0];
        reader.readXML(file);
        faultRates = new FaultRates( new double[]{ Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]) } );

        System.out.println("Edges read!");

        // fill ArrayList<MUXLukas> muxes with multiplexers
        fillMuxArray();

        System.out.println("MUX Array filled!");

        // for every mux in array list of muxes
        for (MUXLukas muxLukas : muxesLukas){
            // add defect paths to defect edges
            defectEdges.addAll(muxLukas.getRREdgeDeleteList());

            // increase the overall number of edges, defect edges and memory cells
            numOfEdges += muxLukas.getNumberOfEdges();
            numOfDefectEdges += muxLukas.getNumOfDefectEdges();
            numOfMemCells += muxLukas.getNumberOfMemCells();
            numOfFaultyMemristors += muxLukas.getNumberOfFaultyMemristors();

            // increase the overall number of SA0, SA1 and UD
            faults = muxLukas.getNumberOfFaultsPerType();
            for (int i = 0; i < faults.length; i++){
                numOfFaults[i] += faults[i];
            }

            // append the graph and stats of every mux to the output of the program
            output.append(muxLukas.printStats());
            output.append(muxLukas.printGraph());
            output.append("--------------------------------------------------------------------------------------------\n");
        }
        for (MUXStefan muxStefan : muxes){
            defectEdgesStefan.addAll(muxStefan.getDefectRREdgesList());
            numOfEdgesStefan += muxStefan.getNumberOfEdges();
            numOfDefectEdgesStefan += muxStefan.getNumOfDefectEdges();
            numOfMemCellsStefan += muxStefan.getNumberOfMemCells();
            numOfFaultyMemristorsStefan += muxStefan.getNumberOfFaultyMemristors();

            output.append("---MUX Stefan - Begin-----------------------------------------------------------------------\n");
            output.append(muxStefan.printStats());
            output.append(muxStefan.printGraph());
            output.append("---MUX Stefan - End-------------------------------------------------------------------------\n");
        }

        System.out.println("MUX Usabilities calculated!");

        // List to Array //TODO might be possible to remove zwischenstep
        deleteList = defectEdges.toArray(new int[defectEdges.size()][3]);
        deleteListStefan = defectEdgesStefan.toArray(new int[defectEdgesStefan.size()][3]);

        // Sort defect edges by RRGraph Index (the third element) in descending order
        Arrays.sort(deleteList, Comparator.comparingInt((int[] a) -> a[2]).reversed());
//        Arrays.parallelSort(deleteList, Comparator.comparingInt((int[] a) -> a[2]).reversed());
        Arrays.sort(deleteListStefan, Comparator.comparingInt((int[] a) -> a[2]).reversed());

        System.out.println("Defect Edges sorted!");

        // write data into xml file
        if (RUN_LUKAS_CODE) {
            reader.writeXML(deleteList);
        }
        if (RUN_STEFAN_CODE) {
            reader.writeXML(deleteListStefan);
        }
        reader.finalizeWriting();

        System.out.println("XML written!");

        // add some overall output data
        output.append("FPGARoutingSim ran successfully!\n");
        output.append("It has ").append(muxesLukas.size()).append(" Multiplexers\n");
        output.append("Number of configurable Edges in FPGA: ").append(numOfEdges).append("\n");
        output.append("Number of defect Edges in FPGA after Fault Sim: ").append(numOfDefectEdges).append("\n");
        output.append("Number of Memory Cells to set the Edges in FPGA: ").append(numOfMemCells).append("\n");
        output.append("Number of SA0 in Memory Cells: ").append(numOfFaults[0]).append("\n");
        output.append("Number of SA1 in Memory Cells: ").append(numOfFaults[1]).append("\n");
        output.append("Number of UD in Memory Cells: ").append(numOfFaults[2]).append("\n");
        output.append("Number of faulty Memristors: ").append(numOfFaultyMemristors).append("\n");
        output.append("Number of total faults in Memory Cells: ").append(numOfFaults[0] + numOfFaults[1] + numOfFaults[2]).append("\n");
        // set end time of the simulation and append overall time to output
        final long timeEnd = System.currentTimeMillis();
        output.append("Needed ").append((timeEnd - timeStart)/1000).append(" s\n");

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
                    muxesLukas.add(new MUXLukas(muxSrcNodes, faultRates));      // Add new MUX
                    muxes.add(new MUXStefan(muxSrcNodes, faultRates));          // Add new MUX
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
