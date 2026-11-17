# Premium Quotes & Rating Engine

Status: Draft
Package: `com.example.miniinsurance.quote` (plus changes in `policy/`)

The keywords MUST, MUST NOT, SHOULD and SHOULD NOT are used as defined in RFC 2119.

## 1. Summary

Customers submit type-specific risk details and receive a quote containing an annual premium, an itemised factor breakdown, a status and an expiry date. A policy is then purchased from a quote. The server alone computes premiums. The feature covers MOTOR, HEALTH, HOME, TRAVEL and LIFE policy types.

## 2. Decisions

- Flow: quote, then purchase. Quotes are stored, owner-scoped, single-use and valid for 30 days.
- Pricing: `premium = baseRate(type, coverage) × Π multipliers`, expressed as an annual amount.
- Rates, bands and multipliers live in Java constants and enums in code. They are not stored in the database and not read from properties. Each quote records a `ratingVersion` string identifying the rule set used.
- Request bodies are polymorphic JSON, discriminated by `policyType`.
- Money uses `BigDecimal` at scale 2 with `RoundingMode.HALF_UP`. There is one currency, set as a configured constant (for example `USD`).

## 3. Endpoints

| Method & path | Role | Purpose |
|---|---|---|
| `POST /api/customer/quotes` | CUSTOMER | Create a quote (201) |
| `GET /api/customer/quotes/{id}` | CUSTOMER (owner) | Read own quote |
| `GET /api/customer/quotes` | CUSTOMER | Paginated list of own quotes |
| `GET /api/admin/quotes` and `/api/admin/quotes/{id}` | ADMIN | List (filter by `status`, `policyType`; paginated) or read any quote |
| `POST /api/customer/policies` | CUSTOMER | Purchase; body is `{quoteId, startDate}` only |

## 4. Rating inputs per type

All fields are required unless marked otherwise.

- **HEALTH**: `dateOfBirth`, `heightCm`, `weightKg`, `smoker`, `conditions` (a set drawn from `DIABETES`, `HYPERTENSION`, `ASTHMA`, `HEART_DISEASE`, `CANCER_HISTORY`; may be empty), `coverageAmount`.
- **LIFE**: `dateOfBirth`, `termYears`, `smoker`, `conditions` (same enum as HEALTH), `occupationRisk` (`LOW` | `MEDIUM` | `HIGH`), `sumAssured` (equal to coverage).
- **MOTOR**: `vehicleValue` (equal to coverage), `manufactureYear`, `driverDateOfBirth`, `yearsLicensed`, `usage` (`PRIVATE` | `COMMERCIAL`), `atFaultClaimsLast3Years`.
- **HOME**: `propertyValue` (equal to coverage), `constructionType` (`BRICK` | `CONCRETE` | `WOOD` | `OTHER`), `propertyAgeYears`, `locationRisk` (`LOW` | `MEDIUM` | `HIGH`), `hasSecuritySystem`, `priorClaims`.
- **TRAVEL**: `region` (`DOMESTIC` | `REGIONAL` | `WORLDWIDE` | `HIGH_RISK`), `tripStart`, `tripEnd`, `travellerDatesOfBirth` (a list of 1 to 10 dates; its size is the number of travellers), `preExistingConditions`, `adventureActivities`, `coverageAmount`.

Coverage limits (default constants):

- HEALTH: 10,000 to 1,000,000
- TRAVEL: 5,000 to 500,000
- LIFE: 25,000 to 5,000,000
- MOTOR and HOME: insured value greater than 0 and at most 10,000,000

## 5. Rating model

Default values, held in one constants class per type. They are tunable.

- **Base rate**: annual rate per 1,000 of coverage, per type. Defaults: HEALTH 12, LIFE 4, MOTOR 25, HOME 3. TRAVEL 8 per 1,000 of coverage per 7-day block of trip duration (rounded up).
- **Age band** (HEALTH, LIFE, and the oldest traveller for TRAVEL): 18–29 ×0.9, 30–39 ×1.0, 40–49 ×1.3, 50–59 ×1.8, 60–75 ×2.6.
- **Conditions**: ×1.15 per condition, cumulative, capped at ×2.5.
- **Smoker**: ×1.4.
- **BMI band**: below 18.5 ×1.1; 18.5–24.9 ×1.0; 25–29.9 ×1.15; 30 and above ×1.4.
- **MOTOR**: vehicle-age bands; driver age below 25 ×1.5; years licensed below 2 ×1.3; `COMMERCIAL` usage ×1.25; each at-fault claim ×1.2.
- **HOME**: construction-type factor; property-age bands; location `LOW` / `MEDIUM` / `HIGH` ×0.9 / ×1.0 / ×1.4; security system ×0.9; each prior claim ×1.15.
- **TRAVEL**: region factor; trip-duration factor; multiplied by traveller count; adventure activities ×1.5; pre-existing conditions ×1.4.
- **LIFE**: term factor; occupation `LOW` / `MEDIUM` / `HIGH` ×1.0 / ×1.2 / ×1.6.
- **Floor and cap**: the premium has a minimum of 10.00. The total multiplier is capped at ×6.
- Age is computed server-side at quote creation from the date of birth.

## 6. Decline rules

A quote that hits a decline rule gets status `DECLINED`, no premium, and a reason code. Declined quotes are not purchasable.

- **HEALTH**: age below 18 or above 75; `CANCER_HISTORY` together with `HEART_DISEASE`.
- **LIFE**: age below 18 or above 70; `termYears` above 40.
- **MOTOR**: driver below 18; 3 or more at-fault claims in the last 3 years; vehicle older than 25 years.
- **HOME**: 3 or more prior claims with `locationRisk = HIGH`.
- **TRAVEL**: any traveller above 85; `HIGH_RISK` region combined with adventure activities.

## 7. Requirements

### 7.1 Quote creation

- The system MUST compute the premium only on the server. Clients MUST NOT supply a premium, multipliers or a status.
- The system MUST validate input with Bean Validation (types, ranges, non-null values, date rules, collection sizes). Invalid input MUST return 400.
- The system MUST reject an unknown `policyType` and unknown JSON fields with 400.
- The system MUST persist, for each quote: owner, type, inputs, coverage, premium, currency, breakdown, status, `ratingVersion`, `createdAt`, and `expiresAt = createdAt + 30 days`.
- The response MUST include `id`, `policyType`, `status` (`QUOTED` | `DECLINED` | `PURCHASED` | `EXPIRED`), `annualPremium`, `currency`, `coverageAmount`, `breakdown[]` (label and factor or amount per line), `expiresAt`, and `declineReason` when declined.
- DECLINED quotes MUST be persisted for audit, with no premium.
- Results MUST be deterministic: identical inputs and `ratingVersion` MUST produce the identical premium.
- Each type SHOULD have a pure, framework-free calculator behind a common interface (`PremiumCalculator`, selected by `PolicyType`), so adding a type does not change existing calculators.
- The system MUST NOT use `double` or `float` for money or factors.
- ADMIN MUST NOT be able to create quotes; the creation endpoint is customer-only.

### 7.2 Quote reads

- A customer MUST be able to read only their own quotes. Requesting another customer's quote id MUST return 404, not 403, to avoid id probing.
- ADMIN MUST be able to read any quote and filter lists by `status` and `policyType`. Lists MUST be paginated (default page size 20, maximum 100) and ordered newest first.
- `EXPIRED` SHOULD be computed lazily on read (`expiresAt` has passed and the quote was not purchased), so that correctness does not depend on a scheduler.

### 7.3 Purchase

- `POST /api/customer/policies` MUST accept only `{quoteId, startDate}`. The fields `premium`, `coverageAmount`, `policyType` and `endDate` MUST be removed from `CreatePolicyRequest`.
- Type, coverage and premium MUST come from the stored quote.
- Purchase MUST require all of: the quote is owned by the caller; status is `QUOTED`; the quote has not expired; `startDate` is today or later and at most 60 days ahead.
- `endDate` MUST be `startDate + 1 year − 1 day`. For LIFE it MUST be `startDate + termYears`. For TRAVEL, `tripStart` and `tripEnd` define the dates, and `startDate` MUST equal `tripStart`.
- Marking the quote `PURCHASED` and creating the policy MUST happen in one transaction.
- Concurrent purchases of the same quote MUST result in exactly one policy. The system MUST enforce this with an optimistic `@Version` or a pessimistic lock; the losing request MUST receive 409.
- The policy MUST be linked to its quote through a unique `quote_id`.
- A purchased, expired or declined quote MUST NOT be reusable.
- Errors: 404 for not found or not owned; 409 for already purchased; 410 for expired; 422 for declined or an invalid start date.

### 7.4 Errors

- The system MUST add a `@RestControllerAdvice` returning a consistent JSON body: `{timestamp, status, error, message, path, fieldErrors[]}`.
- Mappings: validation to 400 (with field errors); not found to 404; conflict to 409; expired to 410; business-rule violation to 422; unexpected failure to 500 with a generic message.
- Existing 401 and 403 behaviour from `SecurityConfig` MUST be preserved.
- Error bodies MUST NOT contain stack traces, SQL, or submitted input values.

### 7.5 Security and privacy

- Quote endpoints MUST fall under the existing `/api/customer/**` and `/api/admin/**` rules. Identity MUST come from `authentication.getName()`.
- Rating inputs (date of birth, conditions, smoker status, BMI, claims history) MUST NOT be logged, and MUST NOT appear in exception messages. Spring Security `DEBUG` logging MUST NOT be relied on to protect this data.
- Sensitive stored fields (date of birth, height, weight, conditions, smoker status) MUST be encrypted at rest using an application-level JPA `AttributeConverter` with AES-GCM. The key MUST come from the environment variable `DATA_ENCRYPTION_KEY` (Base64). The key MUST NOT be hardcoded or committed.
- Inputs of unpurchased quotes MUST be purged 90 days after expiry by a scheduled job (`@Scheduled`). The purge retains id, status, owner, premium and `ratingVersion`. Inputs of purchased quotes are kept.
- The quote entity SHOULD use a `@Version` column.

### 7.6 Documentation and conventions

- New code MUST follow repository conventions: thin controllers, logic in `*Service` classes, request and response types as records in `dto/` subpackages.
- Every new endpoint MUST carry springdoc annotations (`@Operation`, `@ApiResponses`, `@SecurityRequirement(name = "bearerAuth")`). The polymorphic request MUST be documented with `@Schema(oneOf = …)`.
- Secrets MUST NOT be hardcoded. `application.properties` MUST NOT change beyond env-var-backed keys.
- README endpoint documentation and the architecture section of CLAUDE.md SHOULD be updated.

## 8. Edge cases

- A date of birth in the future, or an age of 0, MUST return 400. A date of birth exactly on a band boundary falls in the older band (lower bound inclusive, upper bound exclusive).
- A birthday between quote creation and purchase MUST NOT trigger recalculation. The quoted price holds until expiry.
- Height must be 100–250 cm and weight 30–300 kg; outside these ranges MUST return 400. BMI is computed with `HALF_UP` to 1 decimal place.
- Duplicate conditions in the list MUST be de-duplicated.
- TRAVEL: `tripEnd` before `tripStart`, a trip longer than 180 days, or a trip starting in the past MUST return 400. More than 10 travellers MUST return 400.
- MOTOR: `manufactureYear` in the future MUST return 400. `yearsLicensed` greater than (driver age − 16) MUST return 400.
- When the multiplier cap or the minimum premium is applied, the breakdown MUST show an explicit adjustment line.
- A quote is treated as expired when `now >= expiresAt`.
- Existing policies (created with a client-supplied premium and no quote) MUST keep working. `quote_id` MUST be nullable in the database. `ddl-auto=update` adds the new columns and existing rows are left untouched.
- The current time MUST come from an injectable `Clock`, so tests are deterministic.

## 9. Testing

- Each calculator MUST have unit tests covering: baseline premium, each factor, band boundaries, cap and floor, each decline rule, and rounding.
- MockMvc tests MUST cover: 401 when unauthenticated; 403 for the wrong role; owner isolation (404); validation 400s; polymorphic deserialisation for each type; purchase happy path; double purchase (409); expired quote (410); declined quote (422); admin list filters and pagination.
- Concurrent purchase SHOULD be tested to confirm exactly one policy is created.
- Tests MUST assert that no rating input appears in captured log output.

## 10. Out of scope

- Payments and instalments
- Renewals
- Endorsements
- Multi-currency
- Admin-editable rates
- Geocoding
- Underwriter manual referral workflow
- Claims
