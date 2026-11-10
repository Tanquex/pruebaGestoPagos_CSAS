package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClienteSpecification {

    private static final String FIELD_FECHA_CREACION = "fechaCreacion";

    private ClienteSpecification() {}

    public static Specification<Cliente> conFiltros(ClienteConsultaRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            agregarFiltrosIdentificacion(predicates, root, query, cb, filtro);
            agregarFiltrosNombre(predicates, root, cb, filtro);
            agregarFiltrosFechas(predicates, root, cb, filtro);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void agregarFiltrosIdentificacion(List<Predicate> predicates, Root<Cliente> root, CriteriaQuery<?> query, CriteriaBuilder cb, ClienteConsultaRequest filtro) {
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
            query.distinct(true);
            predicates.add(cb.equal(joinCuentas.get("numeroCuenta"), filtro.getNumeroCuenta().trim()));
        }
        if (filtro.getActivo() != null) {
            predicates.add(cb.equal(root.get("activo"), filtro.getActivo()));
        }
    }

    private static void agregarFiltrosNombre(List<Predicate> predicates, Root<Cliente> root, CriteriaBuilder cb, ClienteConsultaRequest filtro) {
        if (filtro.getNombre() != null && !filtro.getNombre().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtro.getNombre().trim().toLowerCase() + "%"));
        }
        if (filtro.getApellidoPaterno() != null && !filtro.getApellidoPaterno().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + filtro.getApellidoPaterno().trim().toLowerCase() + "%"));
        }
        if (filtro.getApellidoMaterno() != null && !filtro.getApellidoMaterno().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("apellidoMaterno")), "%" + filtro.getApellidoMaterno().trim().toLowerCase() + "%"));
        }
    }

    private static void agregarFiltrosFechas(List<Predicate> predicates, Root<Cliente> root, CriteriaBuilder cb, ClienteConsultaRequest filtro) {
        if (filtro.getFechaInicio() != null && filtro.getFechaFin() != null) {
            predicates.add(cb.between(root.get(FIELD_FECHA_CREACION), filtro.getFechaInicio(), filtro.getFechaFin()));
        } else if (filtro.getFechaInicio() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(FIELD_FECHA_CREACION), filtro.getFechaInicio()));
        } else if (filtro.getFechaFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get(FIELD_FECHA_CREACION), filtro.getFechaFin()));
        }
    }
}
