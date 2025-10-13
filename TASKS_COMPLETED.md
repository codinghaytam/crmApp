# Tasks Completed Tracker

This file is maintained by the automation to record backend tasks completed. Agents must read and reconcile this file on each run to detect drift or human edits.

Last updated: 2025-10-03

Completed items:
- Database: Switched to PostgreSQL only; removed H2 usage. Configured `spring.datasource.url` and `hibernate.dialect`.
- CORS: Enabled permissive CORS for all origins/methods/headers via SecurityConfig.
- Auth: Implemented login, signup, logout. Email is the username. Signup allows bootstrap Admin or Admin creating non-Vendeur roles. Token blacklist on logout.
- Domain entities: User, Bouteille, Boite, Vente (unified), StockBrute, Chariot, WebhookSubscription, etc.
- Ventes: Unified `Vente` with `VenteType` discriminator. Deprecated split classes/controllers. Added `VenteController` and `VenteService` with validation and filtering.
- Stock rules: Creating a Bouteille consumes StockBrute of matching Type; Boite enforces bottle ratios (1L=15, 0.5L=30, 2L=8, 5L=6).
- Industrial controller: Added `IndustrialController` to fabricate bottles consuming stock.
- Emballage: Implemented `EmballageService` and `EmballageController` (add/remove chariots, add/remove boites). Added GET endpoints to list chariots and summary.
- Webhooks: Implemented `WebhookSubscription` repo/service/controller and event dispatcher. Publish events on stock changes and ventes. Emballage operations also publish STOCK_CHANGED with category EMBALLAGE.
- OpenAPI: Updated root and static OpenAPI with unified ventes, auth, stock, industrial, commercial, emballage, and webhooks paths.
- Postman: Included collection and environment to bootstrap 3 users (admin, agent commercial, vendeur).
- Docker: Added backend Dockerfile. Compose builds backend and frontend services on a bridge network. Frontend Dockerfile path prepared.
- Documentation: Updated README and FRONTEND_INTEGRATION.md with integration tips, endpoints, and webhook behavior.

Action items for future runs:
- Keep static `openapi.yaml` in sync with code.
- If human edits remove deprecated legacy vente classes/controllers, ensure callers use unified endpoints.
- If a separate frontend repo exists, update docker-compose frontend build context accordingly.

