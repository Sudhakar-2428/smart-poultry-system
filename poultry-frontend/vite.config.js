import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        login: resolve(__dirname, 'login.html'),
        signup: resolve(__dirname, 'signup.html'),
        createFarm: resolve(__dirname, 'create-farm.html'),
        dashboard: resolve(__dirname, 'dashboard.html'),
        flock: resolve(__dirname, 'flock.html'),
        eggTracking: resolve(__dirname, 'egg-tracking.html'),
        hatching: resolve(__dirname, 'hatching.html'),
        chickGrowth: resolve(__dirname, 'chick-growth.html'),
        pairing: resolve(__dirname, 'pairing.html'),
        healthRecords: resolve(__dirname, 'health-records.html'),
        feedManagement: resolve(__dirname, 'feed-management.html'),
        finance: resolve(__dirname, 'finance.html'),
        reports: resolve(__dirname, 'reports.html'),
        notifications: resolve(__dirname, 'notifications.html'),
        settings: resolve(__dirname, 'settings.html'),
        inviteMember: resolve(__dirname, 'invite-member.html'),
        sales: resolve(__dirname, 'sales.html'),
      },
    },
  },
});
