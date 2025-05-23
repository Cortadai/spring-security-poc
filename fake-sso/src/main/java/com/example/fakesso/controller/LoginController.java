package com.example.fakesso.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Controller
public class LoginController {

    // Muestra el formulario de login
    @GetMapping("/login.html")
    public String login() {
        return "login";
    }

    // Muestra el formulario con error (si hiciera falta validaciones reales)
    @GetMapping("/login-error.html")
    public String loginError(Model model) {
        model.addAttribute("loginError", true);
        return "login";
    }

    @PostMapping("/login.html")
    public void doLogin(
            @RequestParam String username,
            @RequestParam(required = false) String idAplicacion,
            HttpServletResponse response
    ) {
        // Simulamos un usuario fijo
        username = "alan@turing.com";

        // 1. Crear cookie COOKIESSO (no segura)
        Cookie ssoCookie = new Cookie("COOKIESSO", username);
        ssoCookie.setHttpOnly(false);
        ssoCookie.setSecure(false);
        ssoCookie.setPath("/");
        response.addCookie(ssoCookie);

        // 2. Llamar al middleware para obtener Sessiontmp
        try {
            String url = "http://localhost:7777/loginBegin?idaplicacion=" + idAplicacion;

            HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Cookie", "COOKIESSO=" + username);
            headers.add("X-Cert-Auth", "FAKE-CERT-FOR-POC");

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> middlewareResponse =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // Reenviar cookies del middleware
            List<String> setCookies = middlewareResponse.getHeaders().get("Set-Cookie");
            if (setCookies != null) {
                for (String c : setCookies) {
                    response.addHeader("Set-Cookie", c);
                }
            }
        } catch (Exception e) {
            // En caso de error, responde con 500
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // 3. Redirigir a la SPA
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", "http://localhost:4300/");
    }

}
