# 🐔 Smart Poultry Management System

A modern full-stack Smart Poultry Management System designed to simplify poultry farm operations using modern web technologies.

---

## 📖 Project Overview

The **Smart Poultry Management System** is an end-to-end digital management platform engineered to assist poultry farmers in tracking, analyzing, and optimizing their daily farm workflows. Through a single, unified interactive dashboard, farmers and farm administrators can efficiently manage:

* **Farms**: Multi-farm creation, location tracking, and team member management.
* **Chickens**: Individual and flock-level tracking, breeding pair management, and growth logs.
* **Egg Production**: Daily egg collection logs, grading, and incubation tracking.
* **Feed**: Inventory management, feed consumption tracking, and automated restock alerts.
* **Health Records**: Vaccination schedules, disease logs, mortality tracking, and veterinary records.
* **Financial Management**: Revenue, expense logging, transaction categorization, and financial reporting.
* **Weather Monitoring**: Live weather integration and localized environmental forecasts.
* **User Management**: Secure user onboarding, authentication, and role-based access control.

---

## ✨ Features

- ✅ **Multi Farm Management**: Create and manage multiple farm units with custom settings and member permissions.
- ✅ **Role Based Authentication**: Granular access control for Farm Owners, Managers, and Workers.
- ✅ **JWT Security**: Stateless authentication utilizing JSON Web Tokens with encrypted password hashing.
- ✅ **GPS Farm Location**: Interactive farm coordinate pinpointing and geographical mapping.
- ✅ **Weather Monitoring**: Live localized weather tracking and environmental conditions.
- ✅ **Chicken Registration**: Comprehensive flock and individual bird profiles with pedigree lineage.
- ✅ **Breed Management**: Track chicken breeds, genetic traits, and breeding pairs.
- ✅ **Egg Tracking**: Egg production analytics, hatchability rates, and incubation cycles.
- ✅ **Feed Management**: Real-time feed stock levels, batch consumption rates, and cost analysis.
- ✅ **Health Records**: Scheduled vaccinations, health condition logs, treatment tracking, and alerts.
- ✅ **Financial Reports**: Income statement summaries, expense breakdowns, and profitability metrics.
- ✅ **Dashboard Analytics**: Real-time operational metrics and visual data charts.
- ✅ **Responsive Design**: Mobile-friendly, dynamic UI tailored for desktops, tablets, and smartphones.

---

## 🏗 System Architecture

### Frontend
- **HTML5**: Semantic markup for accessible and UI structure.
- **CSS3**: Modern, custom CSS design system with dynamic themes and glassmorphism styling.
- **JavaScript (ES6+)**: Modular application logic, API integration, and dynamic DOM management.
- **Vite**: Ultra-fast build tool and local development server.

### Backend
- **Spring Boot 3**: Enterprise Java framework for microservice-ready backend APIs.
- **Java 21**: Modern LTS Java features for high performance and clean code.
- **MySQL**: Relational database for robust, ACID-compliant data persistence.
- **JWT Authentication**: Spring Security implementation with JWT token validation filters.
- **Flyway Migration**: Automated database schema migration and version control.

---

## 📁 Project Structure

```text
smart-poultry-system/
│
├── poultry-frontend/
├── poultry-backend/
├── README.md
└── .gitignore
```

---

## 🚀 Installation

### Prerequisites
- **Node.js**: v18+ and **npm**
- **Java JDK**: 21+
- **MySQL Server**: 8.0+

### Frontend

```bash
npm install
npm run dev
```

### Backend

```bash
./mvnw spring-boot:run
```

### Database

**MySQL** (schema auto-migrated via Flyway upon application start)

---

## 🔐 Authentication

- **JWT Authentication**: Secure login flow returning signed JSON Web Tokens for subsequent request headers.
- **Role Based Access**: Enforced authorization constraints across endpoint routes based on user roles (`ADMIN`, `OWNER`, `MANAGER`, `WORKER`).
- **Secure REST APIs**: Password hashing via BCrypt and CORS-protected API endpoints.

---

## 📌 Current Status

The project is currently under **active development**.

### Completed Modules
- [x] User Authentication & Authorization (JWT & BCrypt)
- [x] Multi-Farm Management & Member Invitations
- [x] Flock & Chicken Profile Management
- [x] Breeding Pair & Hatching Management
- [x] Egg Collection & Incubator Tracking
- [x] Feed Inventory & Usage Logging
- [x] Health & Vaccination Records
- [x] Financial Transactions & Sales Management
- [x] Weather Service Integration
- [x] Interactive Frontend Dashboards & Visualizations

---

## 👨‍💻 Developer

**Sudhakar**  
*Computer and Communication Engineering*

---

## 📄 License

MIT License