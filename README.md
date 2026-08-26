# Tender Buds School Management System — API

Backend API for the school management system (students, fees, stock, dashboard, backup/restore,
and a license/renewal gate). Node.js + Express + TypeScript, PostgreSQL, JWT auth.

The existing `tender_buds_school_software_v1.7_4.html` file is the original client-only prototype
(uses `localStorage`, no backend) — it is **not** wired to this API. This repo is the API service
that a rebuilt web UI or the Android app is meant to call.

## Running locally with Docker

```bash
cp .env.example .env   # edit values, especially JWT_SECRET and passwords
docker compose up --build
```

API is available at `http://localhost:4000`. Health check: `GET /health`.

A default admin user is seeded on first boot (`SEED_ADMIN_USERNAME` / `SEED_ADMIN_PASSWORD` in
`.env`, defaults to `admin` / `ChangeMe123!`), along with a 1-year license starting from first boot.

### Force a clean rebuild (remove old containers/images first)

```bash
docker compose down -v --remove-orphans
docker image prune -a -f
docker compose build --no-cache
docker compose up
```

## Auth

All routes except `/health`, `/api/auth/signup`, `/api/auth/login`, and `/api/auth/reset-password`
require `Authorization: Bearer <token>` (returned from signup/login).

All routes under `/api/students`, `/api/fees`, `/api/stock`, `/api/dashboard`, and `/api/backup`
also require an active (or in-grace-period) license — see **License** below.

## Endpoints

### Auth
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/signup` | – | Create an admin account |
| POST | `/api/auth/login` | – | Log in, returns JWT |
| POST | `/api/auth/logout` | ✓ | Stateless no-op (client discards token) |
| POST | `/api/auth/reset-password` | – | Reset a user's password to a caller-supplied `newPassword` (min 6 chars) |
| GET | `/api/auth/me` | ✓ | Current user profile |

### Students
| Method | Path | Description |
|---|---|---|
| GET | `/api/students?search=` | List / search students |
| GET | `/api/students/:regNo` | Get one student |
| POST | `/api/students` | Create student |
| PUT | `/api/students/:regNo` | Update student (regNo change cascades to fees) |
| DELETE | `/api/students/:regNo` | Delete student (cascades to their fee records) |

### Fees
| Method | Path | Description |
|---|---|---|
| GET | `/api/fees?search=` | List / search fee records |
| GET | `/api/fees/summary` | Totals: receivable / received / pending |
| GET | `/api/fees/:id` | Get one fee record |
| POST | `/api/fees` | Create fee record |
| PUT | `/api/fees/:id` | Update fee record |
| DELETE | `/api/fees/:id` | Delete fee record |

### Stock
| Method | Path | Description |
|---|---|---|
| GET | `/api/stock` | List all stock items |
| GET | `/api/stock/class-availability?filter=` | Class-wise stock breakdown |
| GET | `/api/stock/uniform-sizes` | Saved size suggestions per uniform piece |
| GET | `/api/stock/:id` | Get one stock item |
| POST | `/api/stock/uniform-sizes` | Save a new custom size suggestion |
| POST | `/api/stock` | Add stock (merges into an existing matching item as a restock) |
| PUT | `/api/stock/:id` | Update a stock item |
| DELETE | `/api/stock/:id` | Delete a stock item |

### Dashboard
| Method | Path | Description |
|---|---|---|
| GET | `/api/dashboard/summary` | Totals for the dashboard cards |
| GET | `/api/dashboard/student-fee-status` | Per-student fee totals + Paid/Pending status |

### Backup / Restore
| Method | Path | Description |
|---|---|---|
| GET | `/api/backup/export` | Download full JSON backup |
| POST | `/api/backup/import` | Merge-only restore from a backup file (never deletes/overwrites) |

### License
| Method | Path | Description |
|---|---|---|
| GET | `/api/license/status` | `active` / `grace_period` / `expired` + days remaining |
| POST | `/api/license/renew` | Extend the license (default: +365 days) |
| GET | `/api/license/history` | Past renewals |

License is enforced server-side (not just in the app) via middleware on every business route.
After `expiry_date`, the app still works for `LICENSE_GRACE_DAYS` (default 14) before requests
start failing with `402 LICENSE_EXPIRED`, so a payment delay doesn't cut a school off instantly.

## Error format

All errors return:
```json
{ "error": { "code": "SOME_CODE", "message": "Human readable message", "details": "..." } }
```

Handled cases: validation errors (400), duplicate records (409), not found (404), invalid/expired
auth token (401), expired license (402), invalid JSON body (400), DB constraint violations mapped
from Postgres error codes (400/409), DB unavailable (503), and a generic 500 for anything
unexpected (logged server-side, no internals leaked to the client).
