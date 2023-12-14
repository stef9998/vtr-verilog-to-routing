/**
 * node type of the node in Routing Resource Graph
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public enum RRNodeType {
    /**
     * y-channel
     */
    CHANY,
    /**
     * x-channel
     */
    CHANX,
    /**
     * sink of a net
     */
    SINK,
    /**
     * source of a net
     */
    SOURCE,
    /**
     * input pin of a block
     */
    IPIN,
    /**
     * output pin of a block
     */
    OPIN
}
