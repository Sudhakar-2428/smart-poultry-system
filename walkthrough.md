# Smart Poultry System - Automatic Chick Registration Documentation

## Module Summary
The **Automatic Chick Registration Module** provides zero-manual-entry chick creation and pedigree tracking when a Hatch Batch is completed:
`Pairing` $\rightarrow$ `Egg Laying` $\rightarrow$ `Egg Collection` $\rightarrow$ `Hatching Management` $\rightarrow$ `Hatching Report` $\rightarrow$ `Automatic Chick Registration (THIS MODULE)`.

---

## Features Implemented

### 1. Automatic Chick Creation & Intelligent Registration Numbering
- **Zero Manual Registration**: Automatically creates `Chicken` entities (`category = CHICK`, `healthStatus = HEALTHY`, `status = ACTIVE`) for every healthy chick when a hatch result is saved.
- **Intelligent Registration Code Algorithm**:
  - **Farm-Born Mother Hen** (e.g. `101`): `{MotherHenID}-{HatchBatchNum}-{ChickSeq}` $\rightarrow$ `101-3-001`, `101-3-002`, `101-3-003`.
  - **Purchased Mother Hen** (e.g. `PB01-005`): `{MotherHenID}-{HatchBatchNum}-{ChickSeq}` $\rightarrow$ `PB01-005-1-001`, `PB01-005-1-002`, `PB01-005-1-003`.
  - Automatically calculates current Hatch Batch sequence per hen and formats 3-digit chick sequence numbers with zero duplicate code collisions.

### 2. QR Code & Profile Links
- Generates QR code link pointing to each chick's profile `/flock.html?id={id}`.
- Links Mother Hen, Father Rooster, Hatch Batch, Egg Batch, Breed, Category (`CHICK`), Gender (`UNKNOWN`), Origin (`Farm Born`), DOB, and Age.

### 3. Parent Analytics & Profile Stats
- Dynamically updates Mother Hen Profile (Total Hatch Batches, Total Chicks Produced, Current Hatch count).
- Dynamically updates Father Rooster Profile (Total Chicks Produced, Total Hatch Batches, Partner Hens count).

### 4. Automated Timeline Tracking
- Automatically records 3 sequential timeline events per registered chick:
  1. `Chick Registered`: "Farm-born chick registered automatically with Intelligent Code: 101-3-001"
  2. `QR Generated`: "QR Code generated linking to chick profile: /flock.html?id=..."
  3. `Added To Farm`: "Chick added to active farm flock inventory."

### 5. Multi-Level Reports & Exports (`reports.html`)
- Dedicated **Automatic Chick Registration Reports** tab.
- Filter by All Chicks, Mother-wise, Father-wise, and Hatch Batch.
- Export options: PDF, Excel (.xls), and CSV downloads.

---

## Backend APIs (`poultry-backend`)

### Chick Registration Endpoints
- `POST /api/v1/chick-registration/hatch-batch/{batchId}`: Process automatic chick registration for a completed hatch batch.
- `GET /api/v1/chick-registration/parents/{chickenId}/stats`: Retrieve parent production statistics (Batches, Chicks, Partner Hens).
- `GET /api/v1/chick-registration/reports`: Retrieve chick registration reports grouped by mother, father, batch, or date.

---

## Database Schema & Persistence
- **`chickens`**: `chicken_code` (`101-3-001`), `mother_id`, `father_id`, `pair_id`, `egg_batch_id`, `hatch_result_id`, `category` (`CHICK`), `origin` (`FARM_BORN`), `date_of_birth`, `health_status` (`HEALTHY`), `status` (`ACTIVE`).
- **`chicken_timeline_events`**: `chicken_id`, `event_type` (`CHICK_REGISTERED`, `QR_GENERATED`, `ADDED_TO_FARM`), `title`, `description`, `created_by` ("System").

---

## Testing & Verification
- **Backend Test Suite**: `./mvnw test` executed with **BUILD SUCCESS** across all 197+ integration and unit tests.
- **Frontend Production Build**: `npm run build` executed with **Vite build success** (`built in 511ms`).
