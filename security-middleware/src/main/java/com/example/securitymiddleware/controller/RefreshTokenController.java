package com.example.securitymiddleware.controller;

import com.example.securitymiddleware.exception.ParameterNotFoundException;
import com.example.securitymiddleware.service.RefreshService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static com.example.securitymiddleware.constants.ApplicationConstants.XCERTAUTH;
import static com.example.securitymiddleware.constants.ApplicationConstants.XIDSESSION;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshService refreshService;

    @GetMapping("/refresco1")
    public ResponseEntity<?> refrescarAcceso(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = XIDSESSION, required = false) String idsession,
            @RequestHeader(value = XCERTAUTH, required = false) String certAuth
    ) {
        String clientIp = request.getRemoteAddr();

        log.info("Solicitud de refresco de token: sesion={}, IP={}", idsession, clientIp);

        if (idsession == null || idsession.isBlank()) {
            log.warn("Intento de refresco sin idsession: IP={}", clientIp);
            throw new ParameterNotFoundException("Falta la cabecera X-Idsession");
        }
        if (certAuth == null || certAuth.isBlank()) {
            log.warn("Intento de refresco sin certificado: sesion={}, IP={}", idsession, clientIp);
            throw new ParameterNotFoundException("Falta la cabecera X-Cert-Auth");
        }

        try {
            log.debug("Procesando refresco de token: sesion={}, IP={}", idsession, clientIp);
            refreshService.procesarRefresco(request, response, idsession, certAuth);
            log.info("Token refrescado exitosamente: sesion={}, IP={}", idsession, clientIp);
        } catch (Exception e) {
            log.warn("Error durante refresco de token: sesion={}, IP={}, error={}", 
                    idsession, clientIp, e.getMessage());
            throw e; // Re-lanzamos la excepción para que la maneje el controlador global
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
