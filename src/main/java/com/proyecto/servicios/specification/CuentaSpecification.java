package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.model.cliente.CuentaConsultaRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CuentaSpecification {

    public static Specification<Cuenta> conFiltros(CuentaConsultaRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getNumeroCuenta() != null && !filtro.getNumeroCuenta().isBlank()) {
                predicates.add(cb.equal(root.get("numeroCuenta"), filtro.getNumeroCuenta().trim()));
            }

            if (filtro.getClienteId() != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), filtro.getClienteId()));
            }

            if (filtro.getEstatus() != null && !filtro.getEstatus().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("estatus")), filtro.getEstatus().trim().toUpperCase()));
            }

            if (filtro.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), filtro.getActivo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
