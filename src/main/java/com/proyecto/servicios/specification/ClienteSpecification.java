package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClienteSpecification {

    public static Specification<Cliente> conFiltros(ClienteConsultaRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filtro.getId()));
            }

            if (filtro.getCurp() != null && !filtro.getCurp().isBlank()) {
                predicates.add(cb.equal(root.get("curp"), filtro.getCurp().trim().toUpperCase()));
            }

            if (filtro.getRfc() != null && !filtro.getRfc().isBlank()) {
                predicates.add(cb.equal(root.get("rfc"), filtro.getRfc().trim().toUpperCase()));
            }

            if (filtro.getCorreo() != null && !filtro.getCorreo().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("correo")), filtro.getCorreo().trim().toLowerCase()));
            }

            if (filtro.getNumeroCuenta() != null && !filtro.getNumeroCuenta().isBlank()) {
                Join<Cliente, Cuenta> joinCuentas = root.join("cuentas", JoinType.INNER);
                predicates.add(cb.equal(joinCuentas.get("numeroCuenta"), filtro.getNumeroCuenta().trim()));
            }

            if (filtro.getNombre() != null && !filtro.getNombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtro.getNombre().trim().toLowerCase() + "%"));
            }

            if (filtro.getApellidoPaterno() != null && !filtro.getApellidoPaterno().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + filtro.getApellidoPaterno().trim().toLowerCase() + "%"));
            }

            if (filtro.getApellidoMaterno() != null && !filtro.getApellidoMaterno().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellidoMaterno")), "%" + filtro.getApellidoMaterno().trim().toLowerCase() + "%"));
            }

            if (filtro.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), filtro.getActivo()));
            }

            if (filtro.getFechaInicio() != null && filtro.getFechaFin() != null) {
                predicates.add(cb.between(root.get("fechaCreacion"), filtro.getFechaInicio(), filtro.getFechaFin()));
            } else if (filtro.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), filtro.getFechaInicio()));
            } else if (filtro.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), filtro.getFechaFin()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
