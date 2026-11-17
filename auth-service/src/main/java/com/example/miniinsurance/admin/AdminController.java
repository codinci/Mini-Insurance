package com.example.miniinsurance.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Operation(
            summary = "Get admin dashboard",
            description = "Returns dashboard information for the currently authenticated administrator"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Admin dashboard retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required or JWT token is invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have administrator privileges"
            )
    })
    @GetMapping("/dashboard")
    public ResponseEntity<String> dashboard(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                "Authenticated administrator: "
                        + authentication.getName()
        );
    }
}