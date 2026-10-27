package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.TransaccionServicio;
import com.proyecto.servicios.model.cliente.TransaccionConsultaRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransaccionServicioSpecification {

    public static Specification<TransaccionServicio> conFiltros(TransaccionConsultaRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filtro.getId()));
            }

            if (filtro.getRandomString() != null && !filtro.getRandomString().isBlank()) {
                predicates.add(cb.equal(root.get("randomString"), filtro.getRandomString().trim()));
            }

            if (filtro.getNumCuenta() != null && !filtro.getNumCuenta().isBlank()) {
                Join<TransaccionServicio, Cuenta> joinCuenta = root.join("cuenta", JoinType.INNER);
                predicates.add(cb.equal(joinCuenta.get("numeroCuenta"), filtro.getNumCuenta().trim()));
            }

            if (filtro.getIdServicio() != null) {
                predicates.add(cb.equal(root.get("idServicio"), filtro.getIdServicio()));
            }

            if (filtro.getIdProducto() != null) {
                predicates.add(cb.equal(root.get("idProducto"), filtro.getIdProducto()));
            }

            if (filtro.getEstatus() != null && !filtro.getEstatus().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("estatus")), filtro.getEstatus().trim().toUpperCase()));
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
