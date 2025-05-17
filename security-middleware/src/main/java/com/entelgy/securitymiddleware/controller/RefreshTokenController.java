package com.entelgy.securitymiddleware.controller;

import com.entelgy.securitymiddleware.service.RefrescoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefrescoService refrescoService;

    @GetMapping("/refresco1")
    public ResponseEntity<Void> refrescarAcceso(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader("X-Idsession") String idsession,
            @RequestHeader("X-Cert-Auth") String certAuth
    ) {
        log.info("Solicitado refresco de token para idsession: {}", idsession);

        try {
            refrescoService.procesarRefresco(request, response, idsession, certAuth);
            return ResponseEntity.status(201).build();
        } catch (SecurityException se) {
            log.warn("Refresco fallido: {}", se.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("Error inesperado durante el refresco", e);
            return ResponseEntity.status(500).build();
        }
    }
}
