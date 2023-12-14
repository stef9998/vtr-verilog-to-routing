/**
 * Fault types of memristors and memory cells
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public enum Fault {
    /**
     * fault free cell/memristor
     */
    FF,
    /**
     * stuck at one cell/memristor
     */
    SA1,
    /**
     * stuck at zero cell/memristor
     */
    SA0,
    /**
     * undefined cell/memristor
     */
    UD
}
