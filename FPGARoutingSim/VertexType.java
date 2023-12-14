/**
 * vertex types in MUX are source node (SRCNODE), sink node (SINKNODE) and inter node (INTERNODE)
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public enum VertexType {
    /**
     * source nodes of the MUX
     */
    SRCNODE,
    /**
     * sink node of the MUX
     */
    SINKNODE,
    /**
     * node between first and second stage in the MUX
     */
    INTERNODE
}
