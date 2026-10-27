package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones_servicio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "random_string", nullable = false, unique = true, length = 64)
    private String randomString;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "precio", nullable = false, precision = 15, scale = 2)
    private BigDecimal precio;

    @Column(name = "telefono_referencia", nullable = false, length = 100)
    private String telefonoReferencia;

    @Column(name = "bandera", nullable = false, length = 30)
    private String bandera;

    @Column(name = "distribuidor", nullable = false)
    private Integer distribuidor;

    @Column(name = "hora_local", nullable = false, length = 30)
    private String horaLocal;

    @Column(name = "payload_firmado_aes", columnDefinition = "TEXT")
    private String payloadFirmadoAes;

    @Column(name = "estatus", nullable = false, length = 30)
    private String estatus;

    @Column(name = "codigo_respuesta_proveedor", length = 20)
    private String codigoRespuestaProveedor;

    @Column(name = "mensaje_proveedor")
    private String mensajeProveedor;

    @Column(name = "folio_proveedor", length = 100)
    private String folioProveedor;

    @Column(name = "autorizacion_proveedor", length = 100)
    private String autorizacionProveedor;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
