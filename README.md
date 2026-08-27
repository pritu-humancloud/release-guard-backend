# ReleaseGuard Platform

ReleaseGuard is an automated release governance and risk evaluation platform built using Spring Boot microservices, Spring Cloud API Gateway, Apache Kafka, PostgreSQL, and Redis.

---

## 🏗️ Microservices Architecture

```
                                  +------------------------------+
                                  |   Spring Cloud API Gateway   |
                                  |        (Port: 8080)          |
                                  |     Unified Swagger UI       |
                                  +---------------+--------------+
                                                  |
           +--------------------------------------+--------------------------------------+
           |                                      |                                      |
           v                                      v                                      v
+----------------------+              +----------------------+              +-------------------------+
|   release-service    |              |     risk-service     |              |  notification-service   |
|     (Port: 8082)     |              |     (Port: 8083)     |              |       (Port: 8085)      |
| PostgreSQL: releases |              | PostgreSQL: risk_db  |              | PostgreSQL: notif_db    |
+----------+-----------+              +----------+-----------+              +------------+------------+
           |                                     ^                                       ^
           | release.created                     |                                       |
           +-------------------------------------+                                       |
           |                                                                             |
           | release.status.changed                                                      |
           +-----------------------------------------------------------------------------+
           |                                                                             |
           |                              risk.calculated (score >= 70)                  |
           |                              +----------------------------------------------+
           |                              |
           | health.incident.created      |
           +------------------------------+
```

---

## 🔄 End-to-End Release Workflow

1. **Developer finishes a change** $\rightarrow$ Creates a release (`POST /api/releases`). Status is initialized to `DRAFT`.
2. **Kafka event published** $\rightarrow$ `release.created` event is published to Kafka.
3. **Connect Release to GitHub PR** $\rightarrow$ Pull request diff is ingested (`POST /api/releases/{id}/github-sync`), storing changed files, line additions/deletions, and PR metadata.
4. **Risk Service analyzes changes** $\rightarrow$ Evaluates changes against risk rules (e.g., Database migrations, Auth/Security changes, Configuration changes, Deleted files, Large diffs, High file count) via `POST /api/risk/assess`.
5. **High Risk Notification** $\rightarrow$ When risk score $\ge 70$, `risk.calculated` triggers an automated `RISK_HIGH` notification in `notification-service`.
6. **Release Review** $\rightarrow$ Reviewers update release status (`PUT /api/releases/{id}/status`).
   - If marked `BLOCKED`: publishes `release.status.changed` $\rightarrow$ `notification-service` consumes and generates a `RELEASE_BLOCKED` alert.
   - If marked `APPROVED`: proceeds to deployment.
7. **Deployment** $\rightarrow$ Status updated to `DEPLOYED` (sets `deployedAt` timestamp).

---

## 🌐 Unified Swagger UI (Single Entry Point)

Spring Cloud API Gateway aggregates all OpenAPI specs into a single dashboard:

- **Unified Swagger UI URL**: `http://localhost:8080/swagger-ui.html`
- **Dropdown Selector**: Switch seamlessly between:
  - `Release Service` (`/v3/api-docs/release-service`)
  - `Risk Service` (`/v3/api-docs/risk-service`)
  - `Notification Service` (`/v3/api-docs/notification-service`)

### API Gateway Routes (Port 8080)

| Route Pattern | Target Service | Description |
|---|---|---|
| `/api/releases/**` | `http://localhost:8082` | Release management & GitHub sync |
| `/api/risk/**` | `http://localhost:8083` | Risk calculations & rule management |
| `/api/notifications/**` | `http://localhost:8085` | Notification listings & SSE streaming |
| `/ws/notifications/**` | `ws://localhost:8085` | STOMP over WebSocket push |

---

## 🔔 Notification Service (Port 8085)

### Entities
- **Notification**:
  - `id`: UUID (Primary Key)
  - `type`: Enum (`RISK_HIGH`, `INCIDENT`, `RELEASE_BLOCKED`)
  - `recipient`: String
  - `message`: String
  - `sentAt`: Instant
  - `status`: Enum (`PENDING`, `SENT`, `FAILED`)

### Endpoints
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/notifications` | List notifications (filter by `recipient`, `type`, `status`) |
| `GET` | `/api/notifications/{id}` | Get notification details |
| `POST` | `/api/notifications` | Manually dispatch a notification |
| `PUT` | `/api/notifications/{id}/status` | Update notification status |
| `GET` | `/api/notifications/stream` | Real-time push via Server-Sent Events (SSE) |
| `WS` | `/ws/notifications` | Real-time push via STOMP WebSocket (Topic: `/topic/notifications`) |

---

## 📨 Kafka Topics & Event Payloads

### Topics
| Topic | Producer | Consumer | Condition / Trigger |
|---|---|---|---|
| `release.created` | `release-service` | `risk-service` | When a new release is created |
| `release.status.changed` | `release-service` | `notification-service` | Alerts on `BLOCKED` status |
| `risk.calculated` | `risk-service` | `notification-service` | Triggers alert when score $\ge 70$ |
| `health.incident.created` | External / Health | `notification-service` | Triggers `INCIDENT` alert |

### Standardized Event Payload Schema
```json
{
  "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "eventType": "risk.calculated",
  "releaseId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "timestamp": "2026-08-27T12:00:00Z",
  "data": {
    "score": 75,
    "riskLevel": "HIGH",
    "assessmentId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
  }
}
```

---

## 🚀 Running the Services Locally

1. **Start Release Service** (Port 8082):
   ```powershell
   cd release-service
   .\mvnw.cmd spring-boot:run
   ```

2. **Start Risk Service** (Port 8083):
   ```powershell
   cd risk-service
   ..\release-service\mvnw.cmd spring-boot:run
   ```

3. **Start Notification Service** (Port 8085):
   ```powershell
   cd notification-service
   ..\release-service\mvnw.cmd spring-boot:run
   ```

4. **Start API Gateway** (Port 8080):
   ```powershell
   cd api-gateway
   ..\release-service\mvnw.cmd spring-boot:run
   ```
