package com.example.securitymiddleware.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SecurityInfoService {

    public List<String> getRolesForUser(String idusuario, String idaplicacion) {

        // Deberiamos llamar al servicio SOAP webservice-security o similar para recoger los roles
        log.info("Llamando al servicio de roles para usuario {} y aplicación {}", idusuario, idaplicacion);
        // Como no tenemos acceso al servicio, vamos a simularlo
        return List.of("ADMIN", "USER");

    }
}
