public class Vertex {
    private final VertexType vertexType;    // type of the vertex
    private final int nodeID;               // node ID in mux or Routing Resource Graph if node type is SRCNODE or SINKNODE
    private final int RRIndex;              // index of the node in Routing Resource Graph

    // Constructor
    public Vertex(int nodeID, VertexType vertexType, int RRIndex){
        // set nodeID, vertexType and RRIndex
        this.nodeID = nodeID;
        this.vertexType = vertexType;
        this.RRIndex = RRIndex;
    }

    // returns the ID of the vertex
    public int getVertexID() {
        return nodeID;
    }

    // returns the type of the vertex
    public VertexType getVertexType() {
        return vertexType;
    }

    // returns the Index in Routing Resource Graph
    public int getRRIndex() { return RRIndex; }
}
