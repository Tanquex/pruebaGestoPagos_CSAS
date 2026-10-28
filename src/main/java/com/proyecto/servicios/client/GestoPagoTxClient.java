package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoTx", url = "${gestopago.tx.url}")
public interface GestoPagoTxClient {

    @PostMapping(value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.ALL_VALUE)
    String enviarTransaccionJson(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody String jsonPayload
    );
}
