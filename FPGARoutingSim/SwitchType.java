/**
 * switch type of switches in Routing Resource Graph
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public enum SwitchType {
    /**
     * an isolating, configurable multiplexer
     */
    mux,
    /**
     * an isolating, configurable tristate-able buffer
     */
    tristate,
    /**
     * a non-isolating, configurable pass gate
     */
    pass_gate,
    /**
     * a non-isolating, non-configurable electrical short
     */
    buffer,
    /**
     * an isolating, non-configurable non-tristate-able buffer
     */
    shorts
}
