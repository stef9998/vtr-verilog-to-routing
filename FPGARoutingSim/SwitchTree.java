import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bundle of Switches which connect together one end to build a multiplexer.
 * Is used in the calculation of the {@link MUX} class to build up to a bigger two-stage multiplexer.
 *
 * @author Stefan Reichel
 */
public class SwitchTree {
    private final List<Switch> switches;
    private final int numOfSwitches;

    private boolean[] FFPos, SA0Pos, SA1Pos, UDPos; //TODO create getter if needed
    private int numOfFF, numOfSA0, numOfSA1, numOfUD = 0;

    private List<RREdge> rrEdges;

    /**
     * Constructs a SwitchTree with the specified number of switches, the memory cell type for the switches and the fault rates of the memristors.
     *
     * @param numOfSwitches  number of switches to create
     * @param memCellType    class representing the type of memory cell to build the switches with
     * @param faultRates     fault rates used for the memristors in the memory cells
     * @param <E>            type of memory cell to build the switches with
     */
    public <E extends MemCell> SwitchTree(int numOfSwitches, Class<E> memCellType, FaultRates faultRates) {
        this.numOfSwitches = numOfSwitches;
        this.switches = new ArrayList<>(numOfSwitches);
        try {
            Constructor<E> constructor = memCellType.getDeclaredConstructor(FaultRates.class);
            for (int i = 0; i < numOfSwitches; i++) {
                switches.add(i, new Switch(constructor.newInstance(faultRates)));
            }
        } catch (ReflectiveOperationException e) {
            System.err.println("memCellType must be of Class MemCell");
            e.printStackTrace();
        }
        calculateFaults();
    }

    /**
     * Constructs a SwitchTree with the specified RR edges, the memory cell type for the switches and the fault rates of the memristors.
     *
     * <p>This constructor initializes the SwitchTree using the size of the provided RR edges list to calculate the number of switches.
     * It then calls the primary constructor {@link #SwitchTree(int, Class, FaultRates)} to perform the actual initialization.
     *
     * @param rrEdges       list of RR edges
     * @param memCellType   class representing the type of memory cell to build the switches with
     * @param faultRates    fault rates used for the memristors in the memory cells
     * @param <E>           type of memory cell to build the switches with
     */
    public <E extends MemCell> SwitchTree(List<RREdge> rrEdges,  Class<E> memCellType, FaultRates faultRates) {
        this(rrEdges.size(), memCellType, faultRates);
        this.rrEdges = rrEdges;
    }

    public SwitchTree(List<Switch> switches){
        this.switches = switches;
        this.numOfSwitches = switches.size();
        calculateFaults();
    }

    /**
     * Calculates information for the switches.
     * <p>
     * The number each different fault types occurs and
     * initializes arrays to track which switch has which fault.
     */
    private void calculateFaults(){
        FFPos = new boolean[numOfSwitches];
        SA0Pos = new boolean[numOfSwitches];
        SA1Pos = new boolean[numOfSwitches];
        UDPos = new boolean[numOfSwitches];
        for (int i = 0; i < numOfSwitches; i++) {
            switch (switches.get(i).getFault()) {
                case FF:
                    FFPos[i] = true;
                    numOfFF ++;
                    break;
                case SA1:
                    SA1Pos[i] = true;
                    numOfSA1 ++;
                    break;
                case SA0:
                    SA0Pos[i] = true;
                    numOfSA0 ++;
                    break;
                case UD:
                    UDPos[i] = true;
                    numOfUD ++;
                    break;
            }
        }
    }

    /**
     * Gets the number of switches in the SwitchTree.
     *
     * @return the number of switches
     */
    public int getNumOfSwitches() {
        return numOfSwitches;
    }

    /**
     * Gets the switch at the specified index.
     *
     * @param i index of the switch to get
     * @return switch at the specified index, or null if the index is out of bounds
     */
    public Switch getSwitch(int i){
        if (i>=0 && i < switches.size()){
            return switches.get(i);
        } else {
            return null;
        }
    }

    /**
     * Gets the fault type of the switch at the specified index.
     *
     * @param i the index of the switch
     * @return the fault type of the switch at the specified index, or null if the index is out of bounds
     */
    public Fault getFault(int i){
        Switch aSwitch = getSwitch(i);
        return (aSwitch != null) ? aSwitch.getFault() : null;
    }

    /**
     * Checks if there are any switches with the "UD" fault.
     *
     * @return true if there is at least one switch with the "UD" fault, false otherwise
     */
    public boolean hasUD(){
        return numOfUD > 0;
    }

    /**
     * Checks if there are any switches with the "SA1" fault.
     *
     * @return true if there is at least one switch with the "SA1" fault, false otherwise
     */
    public boolean hasSA1(){
        return numOfSA1 > 0;
    }

    /**
     * Checks if there is exactly one switch with the "SA1" fault.
     *
     * @return true if there is exactly one switch with the "SA1" fault, false otherwise
     */
    public boolean hasOneSA1(){
        return numOfSA1 == 1;
    }

    /**
     * Checks if there is more than one switch with the "SA1" fault.
     *
     * @return true if there is more than one switch with the "SA1" fault, false otherwise
     */
    public boolean hasMoreThanOneSA1(){
        return numOfSA1 > 1;
    }

    /**
     * Gets the RR edge at the specified index.
     *
     * @param i the index of the RR edge
     * @return the RR edge at the specified index, or null if the index is out of bounds
     */
    public RREdge getRREdge(int i){
        if (i>=0 && i < rrEdges.size()){
            return rrEdges.get(i);
        } else {
            return null;
        }
    }
}
