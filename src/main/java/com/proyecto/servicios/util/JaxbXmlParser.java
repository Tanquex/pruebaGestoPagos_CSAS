package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;

@Slf4j
@Component
public class JaxbXmlParser {

    public <T> T unmarshal(String xml, Class<T> clazz) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new GestoPagoIntegrationException("El contenido XML recibido del proveedor está vacío");
        }
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xml);
            return clazz.cast(unmarshaller.unmarshal(reader));
        } catch (JAXBException e) {
            log.error("Error al deserializar XML con JAXB para la clase {}: {}", clazz.getSimpleName(), e.getMessage());
            throw new GestoPagoIntegrationException("Error en la estructura del XML retornado por el proveedor", e);
        }
    }
}
