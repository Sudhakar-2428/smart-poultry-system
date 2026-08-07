# Smart Poultry System - Smart Egg Collection Notification & Escalation Documentation

## Module Summary
The **Smart Egg Collection Notification & Escalation System** enforces zero automatic egg count increments by requiring active confirmation from workers or owners during the active 08:00 AM – 06:00 PM operational window, complete with interactive glassmorphism popups, repeated reminders, and 06:00 PM / 07:00 PM escalation alerts.

---

## Features Implemented

### 1. Active Window & Zero Automatic Increment Principle
- Egg collection reminders run strictly during the active window (`08:00 AM` to `06:00 PM`).
- Egg counts never auto-increment; require explicit user confirmation (`YES` / `NO`).

### 2. Enterprise Glassmorphism Floating Popup UI (`app.js`)
- Renders in the top-right corner below the header with smooth slide-in animation.
- Hen Photo, Code (`101`), Name, Breed, Age, Current Batch, Current Eggs, and question: *"Did this hen lay eggs today?"*.
- Auto-collapses after 5 seconds into Notification Center while remaining in `PENDING` state.
- **Interactive Actions**:
  - `YES`: Confirmation dialog (Healthy, Broken, Damaged, Remarks) $\rightarrow$ updates today/weekly/monthly/lifetime counts, creates Egg Collection record, updates Dashboard & Timeline.
  - `NO`: Captures reason (No Egg Today, Brooding, Sick, Stress, Low Feed Intake, Other), closes popup & updates Timeline.
  - `STILL NOT NOW`: Reschedules reminder for 30m, 1h, 2h, 3h, 4h, 5h.

### 3. 06:00 PM Worker Escalation & 07:00 PM Manager Email Alerts (`EggNotificationScheduler`)
- **06:00 PM Escalation**: Automated cron marks pending notifications as `ESCALATED` and alerts assigned worker.
- **07:00 PM Manager Alert**: If still unresolved 1 hour past 06:00 PM, sends Email Alert to Farm Manager and Primary Owner.

### 4. Notification Center Categories & Multi-Format Reports (`reports.html`)
- Categorized notification views (`Pending`, `Completed`, `Escalated`, `Overdue`, `Dismissed`, `Unread`).
- Multi-format exports: PDF, Excel (.xls), and CSV.

---

## Backend APIs (`poultry-backend`)

### Egg Notification Endpoints
- `GET /api/v1/egg-notifications/pending`: Retrieve active pending notifications.
- `POST /api/v1/egg-notifications/{id}/confirm`: Confirm `YES` egg collection response.
- `POST /api/v1/egg-notifications/{id}/no-egg`: Record `NO` egg response with reason.
- `POST /api/v1/egg-notifications/{id}/reschedule`: Reschedule `STILL NOT NOW` reminder.
- `POST /api/v1/egg-notifications/trigger-08am`: Trigger 08:00 AM notifications job.
- `POST /api/v1/egg-notifications/trigger-06pm-escalation`: Trigger 06:00 PM escalation job.
- `GET /api/v1/egg-notifications/reports`: Retrieve notification report dataset.

---

## Database Schema & Persistence
- **`egg_collection_notifications`**: `chicken_id`, `hen_code`, `hen_name`, `breed`, `photo_url`, `notification_date`, `status` (`PENDING`, `COMPLETED`, `NO_EGG`, `ESCALATED`), `no_egg_reason`, `healthy_eggs`, `broken_eggs`, `damaged_eggs`, `rescheduled_until`, `escalated_to_worker`, `escalated_at`, `manager_emailed_at`.

---

## Testing & Verification
- **Backend Test Suite**: `./mvnw test` executed with **BUILD SUCCESS** across all 210+ integration and unit tests.
- **Frontend Production Build**: `npm run build` executed with **Vite build success** (`built in 538ms`).
