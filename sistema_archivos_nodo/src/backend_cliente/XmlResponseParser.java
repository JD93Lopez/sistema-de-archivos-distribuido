package backend_cliente;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class XmlResponseParser {

    public static String extractToken(String soapResponse) {
        Document doc = parseXml(soapResponse);
        Node returnNode = getReturnNode(doc);
        if (returnNode == null || returnNode.getTextContent() == null) {
            throw new RuntimeException("No se encontró token en respuesta SOAP");
        }
        return returnNode.getTextContent().trim();
    }

    public static Archivo extractArchivo(String soapResponse) {
        Document doc = parseXml(soapResponse);
        Node returnNode = getReturnNode(doc);
        if (returnNode == null) throw new RuntimeException("No se encontró archivo en respuesta");

        Archivo archivo = new Archivo();
        archivo.setNombre(getChildText(returnNode, "nombre"));
        archivo.setRuta(getChildText(returnNode, "ruta"));
        String contenidoB64 = getChildText(returnNode, "contenido");
        if (contenidoB64 != null) {
            archivo.setContenido(java.util.Base64.getDecoder().decode(contenidoB64));
        }
        String idStr = getChildText(returnNode, "id");
        if (idStr != null) archivo.setId(Integer.parseInt(idStr));
        String idUsuarioStr = getChildText(returnNode, "idUsuario");
        if (idUsuarioStr != null) archivo.setIdUsuario(Integer.parseInt(idUsuarioStr));
        return archivo;
    }

    public static ArbolEspacio extractArbolEspacio(String soapResponse) {
        Document doc = parseXml(soapResponse);
        Node returnNode = getReturnNode(doc);
        if (returnNode == null) throw new RuntimeException("No se encontró <return>");

        ArbolEspacio arbol = new ArbolEspacio();
        String totalStr = getChildText(returnNode, "espacioTotal");
        String usadoStr = getChildText(returnNode, "espacioUsado");
        if (totalStr != null) arbol.setEspacioTotal(Long.parseLong(totalStr));
        if (usadoStr != null) arbol.setEspacioUsado(Long.parseLong(usadoStr));

        Node raizNode = getChildNode(returnNode, "raiz");
        if (raizNode != null) {
            arbol.setRaiz(parseNodoArbol(raizNode));
        }
        return arbol;
    }

    private static NodoArbol parseNodoArbol(Node node) {
        NodoArbol nodo = new NodoArbol();
        nodo.setNombre(getChildText(node, "nombre"));
        String tamanoStr = getChildText(node, "tamano");
        if (tamanoStr != null) {
            nodo.setTamano(Long.parseLong(tamanoStr));
        }

        Node hijosNode = getChildNode(node, "hijos");
        if (hijosNode != null) {
            List<NodoArbol> hijos = new ArrayList<>();
            NodeList hijoNodes = hijosNode.getChildNodes();
            for (int i = 0; i < hijoNodes.getLength(); i++) {
                Node hijoNode = hijoNodes.item(i);
                if (hijoNode.getNodeType() == Node.ELEMENT_NODE && "hijo".equals(hijoNode.getNodeName())) {
                    hijos.add(parseNodoArbol(hijoNode));
                }
            }
            nodo.setHijos(hijos);
        } else {
            nodo.setHijos(new ArrayList<>());
        }
        return nodo;
    }

    private static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Error parsing XML", e);
        }
    }

    private static Node getReturnNode(Document doc) {
        NodeList list = doc.getElementsByTagName("return");
        if (list.getLength() == 0) return null;
        return list.item(0);
    }

    private static Node getChildNode(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    private static String getChildText(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return child.getTextContent();
            }
        }
        return null;
    }
}