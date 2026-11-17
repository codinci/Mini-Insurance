package com.example.miniinsurance.quote;

import com.example.miniinsurance.common.PageResponse;
import com.example.miniinsurance.quote.dto.QuoteRequest;
import com.example.miniinsurance.quote.dto.QuoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/quotes")
@SecurityRequirement(name = "bearerAuth")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Operation(
            summary = "Create premium quote",
            description = "Prices the submitted risk details and stores a quote valid for 30 days. "
                    + "Clients cannot supply a premium. Out-of-appetite risks return status DECLINED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Quote created (status QUOTED or DECLINED)"),
            @ApiResponse(responseCode = "400", description = "Invalid or unknown input"),
            @ApiResponse(responseCode = "401", description = "Authentication required or JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "User does not have customer privileges")
    })
    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(
            @Valid @RequestBody QuoteRequest request,
            Authentication authentication
    ) {
        QuoteResponse response = quoteService.createQuote(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get own quote",
            description = "Returns one of the authenticated customer's quotes. Other customers' quotes return 404."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quote retrieved"),
            @ApiResponse(responseCode = "404", description = "Quote not found"),
            @ApiResponse(responseCode = "401", description = "Authentication required or JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "User does not have customer privileges")
    })
    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> getQuote(
            @Parameter(description = "Quote id") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quoteService.getOwnQuote(id, authentication.getName()));
    }

    @Operation(
            summary = "List own quotes",
            description = "Paginated, newest first. Page size is capped at 100."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quotes retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid page or size"),
            @ApiResponse(responseCode = "401", description = "Authentication required or JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "User does not have customer privileges")
    })
    @GetMapping
    public ResponseEntity<PageResponse<QuoteResponse>> listQuotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quoteService.listOwnQuotes(authentication.getName(), page, size));
    }
}
