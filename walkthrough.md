# Smart Poultry System - Complete Timeline Integration Documentation

## Module Summary
The **Complete Timeline Integration Module** unifies every event in the lifecycle of every chicken into a single chronological timeline:
`Pairing` $\rightarrow$ `Egg Laying` $\rightarrow$ `Egg Collection` $\rightarrow$ `Hatching` $\rightarrow$ `Chick Registration` $\rightarrow$ `Purchase Registration` $\rightarrow$ `Health Records` $\rightarrow$ `Vaccination` $\rightarrow$ `Sales` $\rightarrow$ `Death Records` $\rightarrow$ `Complete Timeline`.

---

## Features Implemented

### 1. Automated Lifecycle Event Tracking
- Automatically records lifecycle events across all farm modules:
  - `PURCHASED`, `REGISTERED`, `QR_GENERATED`, `ADDED_TO_FARM`
  - `VACCINATION`, `HEALTH_CHECK`
  - `PAIRING_STARTED`, `PAIRING_COMPLETED`
  - `EGG_LAYING_STARTED`, `DAILY_EGG_COLLECTION`
  - `EGGS_SENT_TO_HATCHING`, `INCUBATION_STARTED`, `CANDLING_DAY_7`, `CANDLING_DAY_14`, `CANDLING_DAY_18`, `HATCHING_COMPLETED`
  - `CHICK_REGISTERED`, `WEIGHT_UPDATED`, `FEED_PROGRAM_CHANGED`, `SALE`, `DEATH`, `ARCHIVE`

### 2. Chicken Profile Vertical Timeline Tab (`flock.html`, `flock.js`)
- Dedicated Timeline tab inside every Chicken Profile.
- Displays vertical UI timeline cards with:
  - Timestamp
  - Event Icon & Color Badge
  - Event Title & Description
  - Action Performed By (User / System)
  - Direct Clickable Module Link (navigates to Pairing, Egg Collection, Hatching, Health, Sales)
- Interactive filter controls (Event Type, Module, Keyword Search).
- Manual Note creation modal (`POST /api/v1/chickens/{id}/timeline/notes`).

### 3. Global Timeline Audit Reports & Exports (`reports.html`)
- Dedicated **Timeline Audit Reports** tab.
- Filter by Event Type, Module, Date range, and Search keyword.
- Export options: PDF, Excel (.xls), and CSV.

---

## Backend APIs (`poultry-backend`)

### Timeline Endpoints
- `GET /api/v1/chickens/{id}/timeline`: Retrieve filtered chronological timeline events for a chicken.
- `POST /api/v1/chickens/{id}/timeline/notes`: Save manual user notes to a chicken's timeline.
- `GET /api/v1/chickens/timeline/reports`: Retrieve aggregated global timeline report dataset.

---

## Database Schema & Persistence
- **`chicken_timeline_events`**: `chicken_id`, `event_type`, `title`, `description`, `created_by`, `module_name`, `related_entity_id`, `timestamp`.

---

## Testing & Verification
- **Backend Test Suite**: `./mvnw test` executed with **BUILD SUCCESS** across all 204+ integration and unit tests.
- **Frontend Production Build**: `npm run build` executed with **Vite build success** (`built in 535ms`).
