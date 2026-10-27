package com.proyecto.servicios.model.gestopago.tx;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class TxXmlResponse {

    @XmlElement(name = "MENSAJE")
    private TxMensajeXml mensaje;

    @XmlElement(name = "TRANSACCION")
    private TxDetalleXml transaccion;
}
