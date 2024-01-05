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
    List<Switch> switches;
    final int numOfSwitches;

    boolean[] FFpos, SA0pos, SA1pos, UDpos;
    int numOfFF, numOfSA0, numOfSA1, numOfUD = 0;

    List<RREdge> rrEdges;

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
            // Handle exceptions if necessary
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

    private void calculateFaults(){
        FFpos = new boolean[numOfSwitches];
        SA0pos = new boolean[numOfSwitches];
        SA1pos = new boolean[numOfSwitches];
        UDpos = new boolean[numOfSwitches];
        for (int i = 0; i < numOfSwitches; i++) {
            switch (switches.get(i).getFault()) {
                case FF:
                    FFpos[i] = true;
                    numOfFF ++;
                    break;
                case SA1:
                    SA1pos[i] = true;
                    numOfSA1 ++;
                    break;
                case SA0:
                    SA0pos[i] = true;
                    numOfSA0 ++;
                    break;
                case UD:
                    UDpos[i] = true;
                    numOfUD ++;
                    break;
            }
        }
    }

    public int getNumOfSwitches() {
        return numOfSwitches;
    }

    public Switch getSwitch(int i){
        if (i>=0 && i < switches.size()){
            return switches.get(i);
        } else {
            return null;
        }
    }

    public Fault getFault(int i){
        return getSwitch(i).getFault();
    }

    public boolean hasUD(){
        return numOfUD > 0;
    }
    public boolean hasSA1(){
        return numOfSA1 > 0;
    }
    /**
     * Returns true only if exactly one {@link Switch} of all switches has an SA1 fault
     * @return exactly one switch has an SA1 fault
     */
    public boolean hasOneSA1(){
        return numOfSA1 == 1;
    }
    public boolean hasMoreThanOneSA1(){
        return numOfSA1 > 1;
    }

    public RREdge getRREdge(int i){
        if (i>=0 && i < rrEdges.size()){
            return rrEdges.get(i);
        } else {
            return null;
        }
    }
}
