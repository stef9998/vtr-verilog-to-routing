/**
 * Representation for a memory cell.
 * Calculates the fault of the memory cell with given memristors.
 *
 * @author Stefan Reichel
 */
public interface MemCell {

    /**
     * Returns fault contained by the memory cell
     * @return fault contained by the memory cell
     */
    public Fault getCellFault();

    /**
     * Returns the number of faulty memristors in this memory cell
     * @return the number of faulty memristors
     */
    public int getNumOfFaultyMemristors();

}
