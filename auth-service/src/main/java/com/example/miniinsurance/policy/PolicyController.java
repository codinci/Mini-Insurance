package com.example.miniinsurance.policy;

import com.example.miniinsurance.policy.dto.PolicyResponse;
import com.example.miniinsurance.policy.dto.PurchasePolicyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/policies")
@SecurityRequirement(name = "bearerAuth")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Operation(
            summary = "Purchase policy from a quote",
            description = "Converts an owned, unexpired quote into an active policy. "
                    + "Type, coverage and premium come from the quote; only quoteId and startDate are accepted."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Policy created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or unknown input"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required or JWT token is invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have customer privileges"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Quote not found for this customer"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Quote has already been purchased"
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Quote has expired"
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Quote was declined or start date is not allowed"
            )
    })
    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(
            @Valid @RequestBody PurchasePolicyRequest request,
            Authentication authentication
    ) {

        PolicyResponse response =
                policyService.purchasePolicy(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}