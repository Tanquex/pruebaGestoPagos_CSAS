package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoCatalogo", url = "${gestopago.catalogo.url}")
public interface GestoPagoCatalogoClient {

    @GetMapping(value = "/sistema/service/getProductList.do", produces = MediaType.TEXT_XML_VALUE)
    String getProductListXml(@RequestHeader("Authorization") String bearerToken);
}
