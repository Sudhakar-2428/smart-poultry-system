# Smart Poultry Management System – Frontend Application

A state-of-the-art, responsive web application for commercial poultry farm operations, flock registry tracking, egg production logs, health diagnostics, feed inventory management, financial ledgers, and real-time biosecurity analytics.

---

## 🌟 Key Features

- **Owner & Worker Multi-Role Authentication**: Dedicated registration workflows for Primary Farm Owners (`/api/v1/auth/register/owner`) and Farm Workers.
- **Flock & Chicken Management**: Interactive card and table views, tag filtering, breed statistics, laying status tracking, and registry modal workflows.
- **Egg Production Analytics**: Daily collection tracking, quality gradings, damaged egg logs, and historical performance charts.
- **Feed & Inventory Tracking**: Stock level monitoring, automatic low-feed threshold alerts, and feed conversion ratio (FCR) analytics.
- **Health & Brooding Lifecycles**: Vaccination schedules, disease logs, mortality metrics, incubation timers, and health diagnostics.
- **Finance & Expense Ledgers**: Revenue tracking, feed expenditure calculations, bird sales logs, and financial performance summaries.
- **GPS Location Integration**: HTML5 Geolocation API integration with reverse-geocoding for farm address discovery and coordinates mapping.
- **Mobile-First Progressive Web App (PWA)**: Mobile bottom navigation bar, Material Design 3 Floating Action Button (FAB) quick action menu, and standalone mobile PWA display mode (`manifest.json`).
- **Resilient Global Error Management**: Automatic 400ms error debouncing, batch request consolidation, toast deduplication, 5-second auto-dismissal, and a maximum visible toast stack limit.

---

## 🛠️ Technology Stack

- **Core**: HTML5, Vanilla JavaScript (ES6+ Modules), CSS3 Design Tokens
- **Build System**: Vite v4.5
- **Icons**: FontAwesome 6 Pro / Solid
- **Typography**: Google Fonts (Inter, Outfit)
- **Design System**: Custom CSS Design Tokens with modern Glassmorphism, Dark/Light palettes, and Material Design 3 guidelines
- **Mobile & PWA**: Web App Manifest (`manifest.json`), Touch Targets, Viewport Breakpoints

---

## 📁 Project Structure

```text
poultry-frontend/
├── index.html                # Application Landing / Overview Page
├── login.html                # User Login Page
├── signup.html               # Owner & Worker Sign-Up Options Page
├── create-farm.html          # Primary Farm Registration & GPS Setup Page
├── dashboard.html            # Main Farm Operations Dashboard
├── flock.html                # Chicken & Flock Management Module
├── egg-tracking.html         # Egg Production & Quality Tracking Module
├── feed-management.html      # Feed Stock & Inventory Module
├── health-records.html       # Vaccination & Health Diagnostic Module
├── hatching.html             # Egg Incubation & Hatching Module
├── pairing.html              # Breeding & Pairing Management Module
├── chick-growth.html         # Chick Development Tracking Module
├── finance.html              # Financial Ledger & Revenue Tracking Module
├── sales.html                # Bird & Egg Sales Operations Module
├── reports.html              # Analytical Reports & Exporting Module
├── notifications.html        # System Notifications & Alerts Panel
├── settings.html             # Farm Identification & Owner Profile Settings
├── invite-member.html        # Farm Member Invitation & Access Control
├── manifest.json             # Progressive Web App Manifest Configuration
├── config.js                 # Global Application Environment Configuration
├── api.js                    # HTTP Client Interceptor & Global Error Manager
├── app.js                    # Core Application Logic, FAB & Navigation Systems
├── flock.js                  # Flock Management Module Interactions
├── notifications-manager.js  # Notifications State & Storage Manager
├── storage.js                # JWT & Local Session Storage Abstractions
├── authService.js            # Authentication Service Wrappers
├── style.css                 # Primary Design System & Global Stylesheet
├── package.json              # NPM Project Metadata & Dependencies
└── vite.config.js            # Vite Multi-Page Build Configuration
```

---

## 🚀 Getting Started

### Prerequisites

Ensure you have **Node.js** (v16.0.0 or higher) and **npm** (v8.0.0 or higher) installed on your system.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/smart-poultry-management.git
   cd smart-poultry-management/poultry-frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Configure Environment Variables:
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

---

## ⚙️ Development & Build

### Running Local Development Server

Start the local Vite development server with hot module replacement (HMR):
```bash
npm run dev
```
The application will be accessible at `http://localhost:3000`.

### Compiling Production Build

To build the project for production deployment:
```bash
npm run build
```
Compiled production-ready assets will be generated in the `dist/` folder.

### Previewing Production Build

To preview the built production bundle locally:
```bash
npm run preview
```

---

## 📄 Deployment Guidelines

1. Run `npm run build` to generate compiled static assets in `dist/`.
2. Deploy the static contents of `dist/` to any modern web hosting service (Vercel, Netlify, AWS S3 / CloudFront, Nginx, or GitHub Pages).
3. Ensure server routing rewrites or falls back to `index.html` for clean URL resolution.

---

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.
