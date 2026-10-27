package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "gestoPagoTx", url = "${gestopago.tx.url}")
public interface GestoPagoTxClient {

    @PostMapping(value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE, MediaType.ALL_VALUE})
    String enviarTransaccionJson(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody String jsonPayload
    );

    @PostMapping(value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE, MediaType.ALL_VALUE})
    String enviarTransaccionForm(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody Map<String, ?> formParams
    );
}
