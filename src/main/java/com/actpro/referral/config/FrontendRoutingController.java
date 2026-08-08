package com.actpro.referral.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Frontend routing fallback controller.
 * Serves index.html for all non-API routes to allow Angular Router to handle client-side routing.
 * This controller only processes non-API routes (those that don't start with /api, /r, etc.)
 */
@Controller
public class FrontendRoutingController {

    /**
     * Fallback handler for frontend routes.
     * Returns index.html for any route that's not an API endpoint, allowing Angular Router
     * to handle client-side navigation (e.g., /login, /verify-email, /dashboard, etc.)
     */
    @GetMapping(value = {
            "/login",
            "/register",
            "/verify-email",
            "/dashboard/**",
            "/ambassador/**",
            "/join/**",
            "/accept-invitation"
    })
    public String index() {
        return "forward:/index.html";
    }
}
