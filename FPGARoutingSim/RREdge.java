public class RREdge {
    final int[] ids;                                    // IDs and RRIndex
    final RRNodeType nodeType, sinkNodeType;            // RR Graph Node Types

    public RREdge(int[] ids, RRNodeType nodeType, RRNodeType sinkNodeType){
        // set IDs
        this.ids = ids;

        // set node types
        this.nodeType = nodeType;
        this.sinkNodeType = sinkNodeType;
    }

    // returns node ID of RREdge
    public int getNodeID() {
        return ids[0];
    }

    // returns sink Node of the RREdge
    public int getSinkNodeID() {
        return ids[1];
    }

    // returns the switch ID of the switch contained in the edge
    public int getSwitchID() {
        return ids[2];
    }

    // returns Index of the edge in Routing Resource Graph
    public int getRRIndex() { return ids[3]; }

    // returns node type of the source node
    public RRNodeType getNodeType() {
        return nodeType;
    }

    // returns node type of the sink node
    public RRNodeType getSinkNodeType(){
        return sinkNodeType;
    }
}
