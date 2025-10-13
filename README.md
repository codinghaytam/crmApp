# Agent Interface Backend

This project is a Spring Boot (Java 17) backend for managing industrial production, commercial distribution, and vendor sales. It uses PostgreSQL (no H2) and emits webhooks for stock and sales events. OpenAPI documentation is provided.

## Architecture highlights
- Persistence: Spring Data JPA + PostgreSQL
- Security: JWT
- Domain:
  - StockBrute (raw stock with 3 internal containers)
  - Bouteille, Boite (Produit)
  - Vente (unified): attribute `type` in { AU_VENDEUR, DU_VENDEUR }
- Webhooks: STOCK_CHANGED, VENTE_AU_VENDEUR_CREATED, VENTE_DU_VENDEUR_CREATED

## Automation/Agent operating rules
- On each invocation, scan for human changes and adapt edits.
- Do not recreate deleted files.
- Isolation of services: controllers thin, business logic in services, repos persistence-only.

## Domain rules to preserve
- Database: PostgreSQL only.
- Vente unified: use `Vente` with `VenteType` discriminator; legacy split classes are deprecated.
- Stock rules:
  - Creating a Bouteille consumes StockBrute of matching type.
  - Creating a Boite consumes Bouteilles with litrage ratios: 1L=15, 0.5L=30, 2L=8, 5L=6.
- Webhooks emitted on stock changes and vente creation.

## OpenAPI & Docs
- Swagger UI: /swagger-ui.html
- OpenAPI: openapi.yaml and src/main/resources/static/openapi.yaml
- Frontend guide: FRONTEND_INTEGRATION.md

## Docker
- Backend Dockerfile: agentInterface/Dockerfile
- Frontend Dockerfile: frontend/Dockerfile
- Compose: docker-compose.yaml

## Local development
- Java 17, Maven 3.8+
- PostgreSQL per application.properties
- JWT secret via security.jwt.secret
