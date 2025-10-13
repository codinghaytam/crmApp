# Copilot Implementation Instructions (Stock Management App)

Goal:
Augment the existing Java 17 / Spring Boot 3 / PostgreSQL MVC application for stock management with:
1. Required business methods on existing classes (no new domain/entity/model classes **EXCEPT the authorized Vente refactor below**).
2. REST controllers exposing necessary operations.
3. JWT-based security configuration honoring four roles: ADMIN, INDUSTRIAL_AGENT, COMMERCIAL_AGENT, SELLER.
4. Uniform global error handling with consistent JSON error payloads.
5. **All persistence must use Spring Data JPA repositories.**
6. **All REST APIs must be documented and compatible with OpenAPI (Swagger).**
7. use lombok where appropriate.
**Authorized Domain Refactor (Vente Split)**
The legacy `Vente` entity MUST be refactored into TWO distinct sale entities:
- `VenteAuVendeur` (transaction entre agent commercial et vendeur) – peut contenir des `Boite` ET/OU des `Bouteille`.
- `VenteDuVendeur` (transaction entre vendeur et client final) – ne peut contenir que des `Bouteille`.

Rules:
- You MAY create ONLY these two new JPA entity classes plus their Spring Data repository interfaces (e.g., `VenteAuVendeurRepository`, `VenteDuVendeurRepository`).
- The old `Vente` class may be deprecated (annotate with `@Deprecated`) and must NOT be used by new logic. Optionally remove its controller usage while keeping the class temporarily for backward compatibility if needed.
- Each new entity keeps a `String id` (UUID generated on persist) and a calculated `montantTotal` set in `@PrePersist` / `@PreUpdate`.
- Persist sale line items using an explicit structure:
  - Minimal approach: serialized JSON list field (TEXT) of line DTOs (if avoiding new entity classes for lines), OR
  - If line entity creation would violate constraints, keep a transient list for request handling and store aggregated numeric fields (quantités, total) plus a compact textual representation.
- Business invariant enforcement happens in dedicated service methods (see Service-Oriented section). Controllers remain thin.
- Endpoints (see below) MUST enforce content rules (Boite + Bouteille for VenteAuVendeur; only Bouteille for VenteDuVendeur) via service-level validation.

**Stock Interaction Rules (Production & Assemblage)**
- Une `Bouteille` ne peut être créée (ou "remplie") qu'en consommant du stock brut (matière première) à partir d'un des trois conteneurs internes prédéfinis de `StockBrute` (initialisés dans son constructeur). Le type de bouteille détermine le conteneur source.
- La création de `Boite` consomme des `Bouteille` disponibles selon ratios:
  - Carton 1l : 15 bouteilles
  - Carton 1/2 l : 30 bouteilles
  - Carton 2l : 8 bouteilles
  - Carton 5l : 6 bouteilles
- Toute tentative de production (bouteille ou boite) échoue si le stock nécessaire est insuffisant (lever IllegalArgumentException ou exception métier existante).
- Suppression de la classe `VendeurOperations` (ne plus l'utiliser; retirer usages dans services / contrôleurs). Ne pas créer de remplaçant direct : les opérations passent par services dédiés.

**Service-Oriented Action Separation Requirement:**
- All business actions (such as augmenterQuantite, diminuerQuantite, enregistrerVenteAuVendeur, enregistrerVenteDuVendeur, produireBouteille, assemblerBoite, calculerDisponible, etc.) must be implemented as separate service methods.
- If a service class already exists for a domain, add a new method for each distinct action. If not, enrich the existing service class with these methods.
- Controllers must not contain business logic; they should only delegate to the appropriate service method for each action.
- Each REST endpoint should correspond to a single, well-defined service method representing a business action.
- If a single controller needs to expose multiple actions, each action must be mapped to a distinct service method.
- **Do not combine multiple business actions into a single service method.** Each action must be clearly separated as its own method in the service layer.
- Enforce service isolation: do not access repositories belonging to unrelated services and do not spread business logic across services or controllers.

Constraints (Updated):
- Creating new entity/model/repository/service classes remains PROHIBITED EXCEPT for:
  1. `VenteAuVendeur` (+ repository)
  2. `VenteDuVendeur` (+ repository)
  3. Support classes strictly required for JWT/security (if absent) and controller-layer DTO inner records.
- No additional domain entity splits/refactors beyond the above without explicit future authorization.
- You MAY create new controller classes (if not present), security-related classes (filter, config), and ONE global exception handler (@RestControllerAdvice) if absent.
- Preserve existing package structure.
- Use Spring Boot 3 conventions (SecurityFilterChain bean, no WebSecurityConfigurerAdapter).
- **All data access must use Spring Data JPA repositories (@Repository interfaces). Do not use EntityManager or JDBC directly.**
- **Annotate all REST controller classes and endpoints with OpenAPI annotations (`@Tag`, `@Operation`, `@Parameter`, etc.).**
- Keep code minimal; no unused abstractions.
- PostgreSQL already assumed; do not alter persistence configuration unless strictly required.
- Roles are defined in the existing `Role` enum. DO NOT hard-code role strings; always reference the enum.
- IMPORTANT: Existing domain classes / field names are French. DO NOT translate or anglicize.

**Routes for New Vente Entities**
- `VenteAuVendeur` endpoints base: `/api/ventes-au-vendeur`
  - POST /api/ventes-au-vendeur          -> enregistrerVenteAuVendeur
  - GET  /api/ventes-au-vendeur/{id}     -> obtenirVenteAuVendeur
  - GET  /api/ventes-au-vendeur          -> listerVentesAuVendeur (filtrage date optional)
- `VenteDuVendeur` endpoints base: `/api/ventes-du-vendeur`
  - POST /api/ventes-du-vendeur          -> enregistrerVenteDuVendeur
  - GET  /api/ventes-du-vendeur/{id}     -> obtenirVenteDuVendeur
  - GET  /api/ventes-du-vendeur          -> listerVentesDuVendeur (peut filtrer par vendeur courant)

Content Validation Summary:
- VenteAuVendeur: lignes peuvent référencer Boite ou Bouteille (au moins 1). Rejet si mélange illégal de types non reconnus.
- VenteDuVendeur: lignes Bouteille uniquement; rejet si une Boite est fournie.

Montant Total:
- Calcul = somme(prixUnitaire * quantite) (si quantités gérées) ou somme(prixUnitaire) si chaque produit instance représente déjà l'unité vendue (adapter à l'existant). Recalculer sur persist/update.

Stock Adjustments on Sales:
- VenteAuVendeur: diminue le stock commercial disponible (boîtes/bouteilles) et augmente (implicite) la possession du vendeur si ce tracking existe; si non, juste diminuer le stock global.
- VenteDuVendeur: diminue le stock détenu par le vendeur (bouteilles) – rejeter si insuffisant.

Industrial / Production Additions:
- produireBouteille(type, quantite) : consomme le conteneur brut approprié -> crée quantite Bouteille.
- assemblerBoite(litrage, quantite) : vérifie nombre nécessaire de Bouteille selon ratio -> consomme -> crée quantite Boite.
- deconstruireBoite(idBoite) (optionnel si besoin) : inverse l'opération (bonus, pas obligatoire si hors scope actuel).

Error Conditions (throw IllegalArgumentException or suitable custom):
- Stock insuffisant pour production ou vente.
- Type de produit non supporté dans contexte de vente spécifique.
- Identifiant entité introuvable -> NoSuchElementException.

OpenAPI Adjustments:
- Tag distincts: "Ventes au vendeur" & "Ventes du vendeur".
- Document business rules via @Operation(summary, description) including ratios and constraints.

Migration Note:
- If legacy `Vente` table exists, either: (a) leave untouched and stop reading from it, or (b) migrate rows into one of the two new tables manually (out of scope for automated code unless instructions expand). Code should not depend on old `Vente` going forward.

Security Scopes Mapping (unchanged):
- INDUSTRIAL_AGENT: production & stock endpoints
- COMMERCIAL_AGENT: ventes au vendeur
- SELLER: ventes du vendeur (création + consultation propres)
- ADMIN: accès global

High-Level Tasks (Order) (Adjusted for Vente split):
1. Introduce entities `VenteAuVendeur` & `VenteDuVendeur`; deprecate legacy `Vente` usage.
2. Create repositories for both.
3. Add service methods: enregistrerVenteAuVendeur, obtenirVenteAuVendeur, listerVentesAuVendeur, enregistrerVenteDuVendeur, obtenirVenteDuVendeur, listerVentesDuVendeur + helpers for calcul montant & validation.
4. Add/extend controllers with new endpoints and OpenAPI annotations.
5. Implement production logic (produireBouteille, assemblerBoite) consuming raw stock & enforcing ratios.
6. Remove usages of VendeurOperations.
7. Ensure security annotations align with roles.
8. Update OpenAPI file to reflect new endpoints & entities.
9. Maintain global error handling, JWT auth, and existing constraints elsewhere.

(Everything else below remains as previously defined unless overridden by the Authorized Domain Refactor section.)

---

Roles & Responsibilities Mapping (Role enum constants):
- Role.ADMIN
- Role.INDUSTRIAL_AGENT
- Role.COMMERCIAL_AGENT
- Role.SELLER
Responsibilities:
- INDUSTRIAL_AGENT: manage raw stock and sellable stock (record inbound/outbound movements + production).
- COMMERCIAL_AGENT: manage distribution to sellers (ventes au vendeur) & view aggregated seller sales.
- SELLER: report final customer sales (ventes du vendeur) & view own history.
- ADMIN: full read access plus user/account/role management.

High-Level Tasks (Order):
1. Scan existing packages: entity/model, repository, service, controller, security.
2. Detect French class names and record their kebab-case plural route forms (do not modify class names).
3. Identify domain classes likely representing: StockItem / (StockBrut, ArticleVendable), RawStockMovement / (MouvementStockBrut), SellableStockMovement / (MouvementStockVendable), Distribution / (DistributionStock), SaleReport / (RapportVente), User / (Utilisateur), Role (names may differ—adapt).
4. For each domain class:
   - Add missing business methods (e.g., increaseQuantity / augmenterQuantite, decreaseQuantity / diminuerQuantite with validation; registerSale / enregistrerVente; computeAvailable / calculerDisponible; computeTurnover / calculerChiffreAffaires; summarizeByPeriod / resumerParPeriode).
   - Keep methods cohesive; avoid leaking persistence logic.
5. **For each business action, add a separate method in the corresponding service class. Do not combine multiple actions into a single method.**
6. In services (if existing):
   - Add transactional methods wrapping repository calls and enforcing invariants (e.g., cannot reduce below zero, role-based constraints).
   - **All persistence must use Spring Data JPA repositories.**
7. Create or update controllers:
   - IndustrialStockController: endpoints for raw/sellable stock movements.
   - CommercialOperationsController: endpoints for distributing stock to sellers and viewing seller sales.
   - SellerReportingController: endpoints for submitting and listing own sales.
   - AdminController: endpoints for listing users, activating/deactivating accounts, resetting passwords, viewing global stats.
   - If similarly named controllers already exist, extend them instead of creating new ones.
   - **Annotate all controllers and endpoints with OpenAPI annotations (`@Tag`, `@Operation`, `@Parameter`, etc.) to ensure full API documentation.**
   - **Controllers must only delegate to service methods for business actions.**
8. DTOs: If none exist and sensitive fields must be hidden, create inner static records inside controllers instead of new top-level classes (to respect “no new classes” for domain). Prefer direct entity mapping only if safe.
9. Validation:
   - Use jakarta validation annotations on request bodies where appropriate.
10. Security (JWT):
   - Add SecurityConfig class (if absent) with:
     - Bean: PasswordEncoder (BCrypt).
     - Bean: AuthenticationManager (via AuthenticationConfiguration).
     - Bean: SecurityFilterChain configuring:
       - Stateless session.
       - CSRF disabled (REST).
       - Authorization (use enum references, not raw strings). Example:
         .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
         .requestMatchers("/api/industrial/**").hasAnyRole(Role.INDUSTRIAL_AGENT.name(), Role.ADMIN.name())
         .requestMatchers("/api/commercial/**").hasAnyRole(Role.COMMERCIAL_AGENT.name(), Role.ADMIN.name())
         .requestMatchers("/api/seller/**").hasAnyRole(Role.SELLER.name(), Role.ADMIN.name())
         .requestMatchers("/api/auth/**", "/api/health").permitAll()
         .anyRequest().authenticated()
     - ExceptionHandling: custom AuthenticationEntryPoint + AccessDeniedHandler (inline lambdas acceptable).
   - JwtAuthenticationFilter (OncePerRequestFilter):
     - Extract Bearer token.
     - Validate signature & expiration.
     - Load user via existing UserDetailsService.
     - Map enum roles to SimpleGrantedAuthority("ROLE_" + role.name()).
   - JwtUtil (only if no equivalent present; if disallowed to add, embed logic in filter).
11. Authentication endpoints:
   - POST /api/auth/login: accept username/password, return token + role(s).
   - (Optional) /api/auth/refresh if refresh logic already supported.
12. Method-Level Security:
    - Enable @EnableMethodSecurity in SecurityConfig.
    - Use @PreAuthorize referencing enum values:
      Example: @PreAuthorize("hasRole(T(com.example.security.Role).INDUSTRIAL_AGENT.name()) or hasRole(T(com.example.security.Role).ADMIN.name())")
      Seller self-access: @PreAuthorize("#sellerId == authentication.principal.id or hasRole(T(...Role).ADMIN.name())")
13. Business Logic Additions (Examples—adapt names to actual classes):
    - In StockItem or equivalent:
      public void increase(int amount)
      public void decrease(int amount)  // guard: amount <= available
      public int getAvailable()
    - In SaleReport or equivalent:
      public BigDecimal lineTotal()
    - In a statistics-capable class/service:
      public BigDecimal totalTurnover(LocalDate from, LocalDate to)
      public Map<String,Integer> quantitySoldBySeller(LocalDate from, LocalDate to)
14. Global Error Handling (add if absent):
    - Create GlobalExceptionHandler (@RestControllerAdvice) in a suitable package (e.g., web or api).
    - Handle:
      MethodArgumentNotValidException -> 400
      IllegalArgumentException -> 400
      AccessDeniedException -> 403
      BadCredentialsException / UsernameNotFoundException -> 401
      JwtException / signature/expired -> 401
      NoSuchElementException -> 404
      Generic Exception -> 500
    - Standard error JSON payload fields: timestamp (ISO-8601), path, status, error, message, traceId (optional).
    - Provide a private static record ErrorResponse(...) if DTO needed (allowed because it is local to handler; does not introduce new domain entity).
15. Error Handling Usage:
    - Throw existing custom exceptions where available; otherwise use IllegalArgumentException / AccessDeniedException.
16. Transactions:
    - Annotate mutating service methods with @Transactional.
17. Logging:
    - Add lightweight logging (info for login success/failure, stock mutations; debug for JWT validation steps).
18. Testing Hooks:
    - Ensure /api/health (simple controller or existing endpoint) remains permitAll.

Controller Endpoint Sketch (Adjust to actual domain names):
(If French classes exist, use their derived route bases; below are illustrative—replace with actual French-based kebab-case)
- POST /api/mouvements-stock-brut
- POST /api/mouvements-stock-vendable
- GET  /api/stock-resume (or /api/stock/summary if already established—prefer existing)
- POST /api/distributions (or /api/distributions-stock)
- GET  /api/vendeurs/{vendeurId}/ventes
- POST /api/ventes
- GET  /api/ventes (own)
- GET  /api/utilisateurs
- PATCH /api/utilisateurs/{id}/statut
- GET  /api/statistiques/chiffre-affaires
- POST /api/auth/login
- GET  /api/health

Note: If English controllers already exist and are in use, extend them instead of renaming routes—only add French-derived ones if missing and required.

JWT Payload (claims):
{
  sub: username,
  uid: userId,
  roles: [ "ADMIN", "SELLER" ],
  exp: ...
}

Implementation Notes:
- Use io.jsonwebtoken (jjwt) or Nimbus; prefer library already on classpath.
- Keep secret/key in application configuration (do not hardcode).
- Use Duration for token validity (e.g., 15m access, 7d refresh if implemented).
- Return token in JSON: { "token": "...", "expiresAt": "...", "roles": [...] }

Agent Operational Rules (Rescan, Respect Deletions, Service Isolation)
- On every agent invocation, rescan the entire repository to detect human modifications (adds, edits, renames, deletions) and adapt proposed changes accordingly.
- Do not recreate files that were deleted by other developers unless explicitly instructed to restore them.
- When adding logic, follow the isolation-of-services principle: keep business logic in the appropriate service; avoid cross-service coupling; controllers must only delegate; each action stays a separate service method.

Step-by-Step Execution Plan for Copilot:
1. Index all existing classes. On every invocation, re-index the project and reconcile with human changes (detect renames/deletions/edits) before proposing edits.
2. Build a map: ClassName -> /api/<kebab-case(-plural)>. Do not change original code identifiers.
3. Identify candidate classes for augmentation (stock, sales, user/account) using French names.
4. Propose added methods (pure domain logic) inside those classes; no side effects beyond state changes—method names remain French.
5. **For each business action, add a separate method in the corresponding service class.**
6. Update service layer with transactional wrappers and authorization boundaries (enum-based role checks).
7. Create or extend controllers using derived French route bases unless an existing stable path already serves that concern.
8. Add (or extend) SecurityConfig + JwtAuthenticationFilter + (optional) JwtUtil.
9. Implement authentication endpoint returning JWT (enum roles serialized).
10. Apply method-level security using Role enum references.
11. Add GlobalExceptionHandler (if missing) with consistent error response.
12. **Ensure all persistence is via Spring Data JPA repositories.**
13. **Annotate all REST controllers and endpoints with OpenAPI annotations for full documentation.**
14. Ensure build passes (imports, no unused symbols, enum references correct, route naming aligned).

Coding Conventions:
- Package naming: lowercase, no underscores.
- Controller names end with Controller.
- Return ResponseEntity<?> where status variants needed; otherwise return DTO/entity directly.
- Prefer records for inline DTOs (if allowed); else static inner classes.
- Avoid hard-coded role strings; always derive from Role enum.

Do Not:
- Introduce new entity/repository/service classes.
- Change existing table mappings unless required for JWT (e.g., ensuring roles eagerly fetched).
- Embed business logic inside controllers.
- Recreate files that have been deleted by other developers unless explicitly asked to do so.

Deliverables:
- Modified existing domain classes (added French-named methods).
- Added/updated controllers with French-based route paths derived from class names.
- Security configuration with JWT (enum-based roles).
- Authentication endpoint.
- Global exception handler.
- **All persistence via Spring Data JPA repositories.**
- **All REST APIs documented with OpenAPI annotations.**

If Ambiguity:
- Choose the least invasive change.
- Favor adding methods over restructuring.
- Prefer enum-driven role checks over literal strings.

End of instructions.
