# Smart Poultry System - Hatching Management & Hatching Report Documentation

## Module Summary
The **Hatching Management Module** and **Hatching Report Module** provide complete, production-ready, automated tracking between egg collection, incubation monitoring, candling checks, hatch completion, and automated report generation:
`Pairing` $\rightarrow$ `Egg Laying` $\rightarrow$ `Egg Collection` $\rightarrow$ `Hatching Management` $\rightarrow$ `Hatching Report` $\rightarrow$ `Chick Registration`.

---

## Features Implemented

### 1. Hatching Management (`hatching.html`)
- **Automated Cohort Creation**: Transferring eggs marked for `Hatching` in Egg Collection automatically generates a Hatch Batch with a unique code (e.g. `HB-2026-001`), transferring Mother Hen, Father Rooster, Breeding Pair, and Egg Batch.
- **Incubation Method Selection**: Supports Machine Incubator (Incubator #, Tray #, Temp, Humidity, Turning Schedule) and Natural Brooding (Broody Hen, Nest Location).
- **Milestone Candling Logs**: Persistent candling checks (Day 7, Day 14, Day 18) tracking Fertile, Infertile, and Dead Embryos with complete historical audit logs per batch.
- **Dynamic Progress Monitoring**: Real-time progress bar tracking Day 1 to Day 21 incubation milestones.
- **Hatch Completion Outcome**: Captures total eggs, fertile eggs, hatched chicks, healthy chicks, weak chicks, dead chicks, dead embryos, and unhatched eggs with calculated hatch success %.

### 2. Automated Hatching Report Module (`HatchingReport`)
- **Automated Report Generation**: When a hatch batch status changes to `COMPLETED`, a `HatchingReport` entity is automatically created and persisted in MySQL.
- **Header & Parent Information**: Includes Hatch Batch Code, Egg Batch Code, Pairing Code, Report Date, Farm Name ("Greenfield Hatchery"), Mother Hen metadata (Reg #, Name, Breed, Age, Origin), and Father Rooster metadata.
- **Breeding & Collection Summary**: Includes Pairing Date, Egg Laying Start Date, Collection Period (days), Incubation Method, and Egg Summary (Collected, Selected, Healthy, Broken, Rejected).
- **Performance Calculations**: Automatically computes Fertility Rate %, Hatch Success %, Healthy Chick %, and Loss %.
- **Multi-Timeline Event Attachment**: Automatically posts timeline event (`HATCHING_REPORT_GENERATED`) to Mother Hen Timeline, Father Rooster Timeline, and Hatch Batch Timeline.
- **Multi-Format Export**: Export options for PDF, Excel, and CSV with printable document layout.

---

## Backend APIs (`poultry-backend`)

### Hatching & Incubator Endpoints
- `GET /api/v1/incubators/stats`: Retrieves hatching dashboard KPI metrics.
- `GET /api/v1/incubators`: Search incubator batches with page, code, and date filters.
- `POST /api/v1/incubators`: Initialize or schedule a new incubation cohort.
- `POST /api/v1/incubators/{id}/candling`: Record milestone candling check (Day 7/14/18).
- `GET /api/v1/incubators/{id}/candling`: Get candling history logs.
- `POST /api/v1/hatch-results`: Record final hatch completion results.

### Hatching Report Endpoints
- `GET /api/v1/hatching-reports/batch/{incubatorBatchId}`: Retrieve automated hatching report for a batch.
- `POST /api/v1/hatching-reports/batch/{incubatorBatchId}/generate`: Generate or refresh hatching report.

---

## Database Schema & Persistence
- **`incubator_batches`**: `batch_code`, `egg_batch_id`, `source_hen_id`, `male_chicken_id`, `breeding_pair_id`, `incubation_method`, `incubator_number`, `tray_number`, `nest_location`, `start_date`, `expected_hatch_date`, `actual_hatch_date`, `status`.
- **`candling_records`**: `incubator_batch_id`, `candling_day`, `candling_date`, `fertile_eggs`, `infertile_eggs`, `dead_embryos`, `remarks`.
- **`hatch_results`**: `incubator_batch_id`, `total_eggs`, `fertile_eggs`, `hatched_chicks`, `healthy_chicks`, `weak_chicks`, `dead_chicks`, `dead_embryos`, `unhatched_eggs`, `hatch_percentage`, `recorded_date`.
- **`hatching_reports`**: `report_code`, `incubator_batch_id`, `mother_hen_code`, `father_rooster_code`, `pairing_code`, `fertility_rate`, `hatch_success_rate`, `healthy_chick_rate`, `loss_percentage`.

---

## Testing Performed
- **Backend Test Suite**: Executed `./mvnw test` with **BUILD SUCCESS** and 0 failing tests across all packages.
- **Frontend Production Build**: Executed `npm run build` using Vite (**built in 535ms** with 0 errors).
