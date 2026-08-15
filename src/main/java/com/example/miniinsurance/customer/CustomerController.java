package com.example.miniinsurance.customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    @Operation(
            summary = "Get customer profile",
            description = "Returns the profile information for the currently authenticated customer"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer profile retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required or JWT token is invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have permission to access this resource"
            )
    })
    @GetMapping("/profile")
    public ResponseEntity<String> profile(Authentication authentication) {

        System.out.println("User: " + authentication.getName());

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            System.out.println(
                    "Authority: " + authority.getAuthority()
            );
        }

        return ResponseEntity.ok(
                "Authenticated customer: " + authentication.getName()
        );
    }
}