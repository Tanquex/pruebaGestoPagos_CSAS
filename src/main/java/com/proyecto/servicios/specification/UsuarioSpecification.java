package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.model.cliente.UsuarioConsultaRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UsuarioSpecification {

    public static Specification<Usuario> conFiltros(UsuarioConsultaRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filtro.getId()));
            }

            if (filtro.getClienteId() != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), filtro.getClienteId()));
            }

            if (filtro.getCorreo() != null && !filtro.getCorreo().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("correo")), filtro.getCorreo().trim().toLowerCase()));
            }

            if (filtro.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), filtro.getActivo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
