# API Summary & Frontend Integration Guide

This API manages beverage distribution for industrial, commercial, and vendor agents. It supports stock management, bottle/box creation, packaging (emballage), and sales operations.

IMPORTANT for automation/agents: Before making changes or generating a frontend, read `TASKS_COMPLETED.md` to understand what's already implemented and avoid regressions. Re-check it on each run.

## Main Entities
- **User**: Admin, Agent_industrielle, Agent_commercial, Vendeur (email = username)
- **StockBrute**: Raw stock containers (3 types, initialized)
- **Bouteille**: Bottles, created by consuming StockBrute
- **Boite**: Boxes, made of bottles (ratios by litrage)
- **Emballage (Chariots)**: Packaged boxes stored in chariots
- **Vente (unified)**: Sales AU_VENDEUR (boite/bouteille) or DU_VENDEUR (bouteille only)
- **WebhookSubscription**: Event delivery targets

## Business Logic
- Bouteille creation consumes StockBrute of the requested Type.
- Boite must respect ratios:
  - Carton 1L: 15 bottles
  - Carton 1/2L: 30 bottles
  - Carton 2L: 8 bottles
  - Carton 5L: 6 bottles
- Ventes unified with `Vente.type` in { AU_VENDEUR, DU_VENDEUR }.
- Emballage endpoints manage chariots and boites; every change emits a STOCK_CHANGED webhook with `category=EMBALLAGE`.

## Authentication (JWT)
- Email is the username.
- Endpoints:
  - POST /api/auth/signup (bootstrap: first user can be Admin)
  - POST /api/auth/login
  - POST /api/auth/logout (blacklists token)

## Endpoints (high level)
- `/api/stock-brute` (augmenter, diminuer, quantite)
- `/api/industrial/bouteilles` (create bottles)
- `/api/commercial` (types, litrages, bouteilles, boites)
- `/api/emballage` (chariots listing, summary, add/remove chariot, add/remove boite)
- `/api/ventes` (create and list unified sales)
- `/api/webhooks/subscriptions` (create/list/delete)

## OpenAPI & Swagger
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI YAML (root): http://localhost:8080/openapi.yaml
- Static OpenAPI: http://localhost:8080/static/openapi.yaml (also in src/main/resources/static/openapi.yaml)

## Webhooks
- Event types: STOCK_CHANGED, VENTE_AU_VENDEUR_CREATED, VENTE_DU_VENDEUR_CREATED.
- Subscribe: POST /api/webhooks/subscriptions with `{ "targetUrl": "https://example.com/hook", "eventTypes": ["STOCK_CHANGED"] }`.
- If `eventTypes` omitted or empty, all events are delivered.
- Example event payload:
  ```json
  {
    "eventType": "STOCK_CHANGED",
    "timestamp": "2025-10-03T12:34:56Z",
    "payload": {
      "category": "EMBALLAGE",
      "action": "ADD_BOITE",
      "chariots": 3,
      "boitesEmballees": 27
    }
  }
  ```

## Angular Integration Notes
- Generate Angular services/models from OpenAPI.
- Add JWT interceptor for Authorization header.
- On webhook reception in your backend proxy or polling logic, trigger UI refresh of stocks and ventes.
- Components to build:
  - Auth (login/signup/logout)
  - StockBrut (display and mutate quantities)
  - Industrial (create bottles)
  - Commercial (create bottles/boites with ratio validation)
  - Emballage (list chariots, add/remove chariot, add/remove boite, summary)
  - Ventes (create/list with type AU_VENDEUR or DU_VENDEUR)

## Postman / Bootstrap
- Collection: `postman/agentInterface-bootstrap-users.postman_collection.json`
- Environment: `postman/agentInterface-env.postman_environment.json`
- It logs in as Admin and creates Agent commercial + Vendeur users.

---

# Concrete Angular Quickstart (recommended)

Prereqs
- Node.js LTS, Angular CLI
- Bootstrap (via CDN or npm)

1) Create an Angular app
- Create a new Angular project (or reuse an existing one). Add Bootstrap CSS in `angular.json` or via CDN.

2) Generate API client from OpenAPI (pick one)
- Option A: `ng-openapi-gen`
  - Install ng-openapi-gen and configure `ng-openapi-gen.json` to point to `http://localhost:8080/openapi.yaml` and output to `src/app/api`.
  - Run the generator to create typed services and models.
- Option B: `openapi-generator` (typescript-angular)
  - Install OpenAPI Generator and generate a `typescript-angular` client into `src/app/api`.
- Option C: `openapi-typescript-codegen`
  - Generate a lightweight TS client with fetch or axios.

3) Configure environment
- Set `environment.apiBaseUrl = 'http://localhost:8080'`.

4) Add a JWT interceptor
- Create an HTTP interceptor that:
  - Reads a token from a secure store (e.g., memory or sessionStorage)
  - Skips `/api/auth/*`
  - Adds `Authorization: Bearer <token>` to other requests
- Handle 401 by redirecting to login.

5) Implement AuthService
- Methods: `signup`, `login`, `logout`, `getRoles`, `isAuthenticated`.
- Persist token minimally (sessionStorage) and expose an observable auth state.

6) Route guards
- Create role-based `CanActivate` guards. Configure routes for Admin, Agent_industrielle, Agent_commercial, Vendeur.

7) Webhook refresh strategy
- Browser clients cannot receive server-to-client webhooks directly.
- Options:
  - Use a tiny public endpoint (Node/Cloudflare Worker) to receive webhooks, then notify your SPA (e.g., via WebSocket) and refetch.
  - Or implement periodic polling (e.g., refetch stock/ventes every 15–30s, and on page focus or after mutations).

8) First screens to build
- Login/Signup forms
- Dashboard (show stock brut quantities, emballage summary, and recent ventes)
- Industrial: Create bottles
- Commercial: Create bottles/boites and validate ratios
- Emballage: List chariots, add/remove chariot, add/remove boite
- Ventes: Create unified vente with `type` (AU_VENDEUR or DU_VENDEUR); list and filter

## Example: Unified vente creation payload
```json
{
  "type": "AU_VENDEUR",
  "vendeurId": "123",
  "lignes": [
    { "typeLigne": "BOITE", "produitId": "10", "quantite": 3, "prixUnitaire": 50.0 },
    { "typeLigne": "BOUTEILLE", "produitId": "42", "quantite": 10, "prixUnitaire": 5.0 }
  ]
}
```

## Example: Interceptor skeleton (TypeScript)
```ts
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = sessionStorage.getItem('token');
    const skip = req.url.includes('/api/auth/');
    if (!token || skip) return next.handle(req);
    const authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    return next.handle(authReq).pipe(
      catchError(err => {
        if (err.status === 401) { /* redirect to login */ }
        return throwError(() => err);
      })
    );
  }
}
```

## Docker notes (serving Angular build)
- This repository includes `frontend/Dockerfile` that serves static files with Nginx on port 4000 (exposed as 4200 via compose). To deploy your Angular app:
  - Build your Angular project and copy the `dist/<app>/` output into `frontend/public/` before running `docker compose up`.
- Backend and frontend are linked on a bridge network in `docker-compose.yaml`.

## Troubleshooting
- CORS: Backend allows all origins/methods/headers; if using credentials, ensure frontend uses same origin patterns.
- Auth: Ensure email is treated as the username everywhere.
- OpenAPI drift: If API changes, regenerate the client and update types.
- Webhooks: If no public endpoint available, prefer polling as a pragmatic fallback.

## Automation reminder
- Always check `TASKS_COMPLETED.md` before generating or updating the frontend. It summarizes what’s done and highlights constraints your app must respect.
