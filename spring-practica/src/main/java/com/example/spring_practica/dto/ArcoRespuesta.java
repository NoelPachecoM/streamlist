package com.example.spring_practica.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArcoRespuesta {

    private String mensaje;

    // Id de la solicitud ARCO que quedo registrada
    private Integer solicitudId;

    // Solo aparece si arco.devolver-codigo=true (modo pruebas con Postman)
    private String codigoDesarrollo;
}
