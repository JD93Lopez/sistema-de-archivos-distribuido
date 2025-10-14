
package backend_cliente.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for arbolEspacio complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="arbolEspacio">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="espacioTotal" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="espacioUsado" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="raiz" type="{http://central/}nodoArbol" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "arbolEspacio", propOrder = {
    "espacioTotal",
    "espacioUsado",
    "raiz"
})
public class ArbolEspacio {

    protected long espacioTotal;
    protected long espacioUsado;
    protected NodoArbol raiz;

    /**
     * Gets the value of the espacioTotal property.
     * 
     */
    public long getEspacioTotal() {
        return espacioTotal;
    }

    /**
     * Sets the value of the espacioTotal property.
     * 
     */
    public void setEspacioTotal(long value) {
        this.espacioTotal = value;
    }

    /**
     * Gets the value of the espacioUsado property.
     * 
     */
    public long getEspacioUsado() {
        return espacioUsado;
    }

    /**
     * Sets the value of the espacioUsado property.
     * 
     */
    public void setEspacioUsado(long value) {
        this.espacioUsado = value;
    }

    /**
     * Gets the value of the raiz property.
     * 
     * @return
     *     possible object is
     *     {@link NodoArbol }
     *     
     */
    public NodoArbol getRaiz() {
        return raiz;
    }

    /**
     * Sets the value of the raiz property.
     * 
     * @param value
     *     allowed object is
     *     {@link NodoArbol }
     *     
     */
    public void setRaiz(NodoArbol value) {
        this.raiz = value;
    }

}
