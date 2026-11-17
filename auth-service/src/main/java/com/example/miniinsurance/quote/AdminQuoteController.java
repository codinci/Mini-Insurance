package com.example.miniinsurance.quote;

import com.example.miniinsurance.common.PageResponse;
import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.QuoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin read-only access. Returns quote summaries, never raw rating inputs. */
@RestController
@RequestMapping("/api/admin/quotes")
@SecurityRequirement(name = "bearerAuth")
public class AdminQuoteController {

    private final QuoteService quoteService;

    public AdminQuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Operation(
            summary = "List all quotes",
            description = "Paginated, newest first, optionally filtered by status and policy type. Page size is capped at 100."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quotes retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, page or size"),
            @ApiResponse(responseCode = "401", description = "Authentication required or JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "User does not have administrator privileges")
    })
    @GetMapping
    public ResponseEntity<PageResponse<QuoteResponse>> listQuotes(
            @Parameter(description = "Filter by effective status") @RequestParam(required = false) QuoteStatus status,
            @Parameter(description = "Filter by policy type") @RequestParam(required = false) PolicyType policyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(quoteService.listAllQuotes(status, policyType, page, size));
    }

    @Operation(summary = "Get any quote", description = "Returns a quote summary by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quote retrieved"),
            @ApiResponse(responseCode = "404", description = "Quote not found"),
            @ApiResponse(responseCode = "401", description = "Authentication required or JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "User does not have administrator privileges")
    })
    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> getQuote(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.getAnyQuote(id));
    }
}
