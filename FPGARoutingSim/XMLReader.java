import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * @author Lukas Freiberger
 * @author Stefan Reichel
 */
public class XMLReader{

    RREdge[] rrEdges;
    private int[][] edges;
    HashMap<Integer, SwitchType> switchTypes;
    File file;
    Document doc;
    NodeList edgeList;

    /**
     * parse the xml file
     */
    private Document parseFile (File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = docFactory.newDocumentBuilder();
        return db.parse(file);
    }

    /**
     * write into the file
     */
    private void writeFile (Document doc, File file) throws TransformerException {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer trans = transFactory.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "no");
        trans.setOutputProperty(OutputKeys.METHOD, "xml");
        trans.transform(new DOMSource(doc), new StreamResult(file));
    }

    /**
     * reading the xml-file
     */
    public void readXML(String filename){
        try {
            // parse file to Document
            file = new File(filename);
            doc = parseFile(file);

            /*
             * Read Edges out of xml file
             */

            // get all <edge> elements and instantiate array to save edges
            edgeList = doc.getElementsByTagName("edge");
            edges = new int[edgeList.getLength()][4];

            // read first edge and save it in array of edges (save also the index in edgeList) //TODO also only needed because of insertion sort I think
            Node frstEdge = edgeList.item(0);
            if (frstEdge.getNodeType() == Node.ELEMENT_NODE) {
                Element firstElem = (Element) frstEdge;
                edges[0] = new int[]{Integer.parseInt(firstElem.getAttribute("src_node")),
                        Integer.parseInt(firstElem.getAttribute("sink_node")),
                        Integer.parseInt(firstElem.getAttribute("switch_id")),
                        0
                };
            }

            // save the rest of the edges while sort them by sink node ID with insertion sort
//            for (int i = 1; i < edgeList.getLength(); i++) {
            int edgeListLength = edgeList.getLength();
            for (int i = 1; i < edgeListLength; i++) {
                Node node = edgeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // get edge's attributes
                    int[] newEdge = new int[]{Integer.parseInt(element.getAttribute("src_node")),
                            Integer.parseInt(element.getAttribute("sink_node")),
                            Integer.parseInt(element.getAttribute("switch_id")),
                            i //TODO might only be used for insertion sort and might be removable when sorting is done different
                    };

                    // check array if there is an edge with the sink node id bigger than the sink node id of new edge
                    int j = i;
//                    while (j > 0 && edges[j-1][1] > newEdge[1]){
//                        edges[j] = edges[j-1]; //TODO takes 33% of time of method with 15x15. Needs to be checked
//                        j--;
//                    }

                    // save new edge
                    edges[j] = newEdge;
                }
            }

//            Arrays.sort(edges, Comparator.comparingInt(a -> a[1]));
            Arrays.parallelSort(edges, Comparator.comparingInt(a -> a[1]));

            /*
             * Read Nodes out of xml file
             */

            // get all <node> elements and instantiate array to save nodes
            NodeList nodeList = doc.getElementsByTagName("node");
            RRNodeType[] RRNodeTypes = new RRNodeType[nodeList.getLength()];

            // Iterate over node elements
//            for (int i = 0; i < nodeList.getLength(); i++){
            int nodeListLength = nodeList.getLength();
            for (int i = 0; i < nodeListLength; i++){
                Node node = nodeList.item(i);

                if(node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // get node's attributes
                    int id = Integer.parseInt(element.getAttribute("id"));
                    String type = element.getAttribute("type");

                    // add attributes to Array nodes
                    RRNodeTypes[id] = RRNodeType.valueOf(type);
                }
            }

            // instantiate new array of RREdges
            rrEdges = new RREdge[edges.length];

            // fill array with RREdges and save information on node type
            for(int i = 0; i < edges.length; i++){
                    rrEdges[i] = new RREdge(edges[i], RRNodeTypes[edges[i][0]], RRNodeTypes[edges[i][1]]);
            }


            /*
             * Read Switches out of xml file
             */

            // get all <switch> elements and instantiate HashMap to save switches
            NodeList switchList = doc.getElementsByTagName("switch");
            switchTypes = new HashMap<>();

            // fill HashMap with switch IDs and switch types
            for (int i = 0; i < switchList.getLength(); i++) {
                Node node = switchList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    if (!element.getAttribute("type").equals("short")) {
                        switchTypes.put(Integer.parseInt(element.getAttribute("id")), SwitchType.valueOf(element.getAttribute("type")));
                    } else {
                        switchTypes.put(Integer.parseInt(element.getAttribute("id")), SwitchType.shorts);
                    }
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public void writeXML(int[][] defectEdges){

        Node rrEdges = doc.getElementsByTagName("rr_edges").item(0);
        int sinkNode, srcNode;

        // delete the defect edges in the sorted array from the document
        for (int[] defectEdge: defectEdges){
            Node node = edgeList.item(defectEdge[2]);

            if (node.getNodeType() == Node.ELEMENT_NODE){
                Element element = (Element) node;

                sinkNode = Integer.parseInt(element.getAttribute("sink_node"));
                srcNode = Integer.parseInt(element.getAttribute("src_node"));

                if (defectEdge[0] == srcNode && defectEdge[1] == sinkNode){
                    rrEdges.removeChild(node);
                } else {
                    System.out.print("Wrong Index!");
                }
            }
        }
    }

    /**
     * finalize the writing process
     */
    public void finalizeWriting(){
        try {
            writeFile(doc, file);
        } catch (TransformerException e){
            e.printStackTrace();
        }
    }

    /**
     * returns the rr edges
     */
    public RREdge[] getRrNodes() {
        return rrEdges;
    }

    /**
     * returns the switch IDs and the belonging switch types
     */
    public HashMap<Integer, SwitchType> getSwitchTypes() {
        return switchTypes;
    }
}
