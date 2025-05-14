package com.entelgy.fakesso.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // Procesa el login simulado
    @PostMapping("/login.html")
    public void doLogin(
            @RequestParam String username,
            @RequestParam(required = false) String idAplicacion,
            HttpServletResponse response
    ) {
        // Cambiamos el username por gusto
        username = "alan@turing.com";

        // 1. Crear cookie SSO simulada (esta cookie NO es segura)
        Cookie ssoCookie = new Cookie("COOKIESSO", username);
        ssoCookie.setHttpOnly(false); // ❗ Debe ser visible desde JS
        ssoCookie.setSecure(false);   // ✅ Puede ir por HTTP
        ssoCookie.setPath("/");
        response.addCookie(ssoCookie);

        // 2. Crear cookie Sessiontmp simulada (esta cookie SI es segura)
        String idSession = "123";
        String sessiontmp = String.format("user=%s|app=%s|sid=%s", username, idAplicacion, idSession);
        Cookie sessiontmpCookie = new Cookie("Sessiontmp", sessiontmp);
        sessiontmpCookie.setHttpOnly(true); // ❗ Invisible para JS
        sessiontmpCookie.setSecure(false);  // ✅ Hay que poner true si usaremos HTTPS
        sessiontmpCookie.setPath("/");
        sessiontmpCookie.setMaxAge(30);     // caduca en 30 segundos
        response.addCookie(sessiontmpCookie);

        // 3. Redirigir directamente a la SPA de prueba (bank-app-ui)
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", "http://localhost:4200/");
    }

}
