import { Api } from './api.js';
import { Storage } from './storage.js';

export const AuthService = {
  async login(email, password) {
    const response = await Api.post('auth/login', { email, password });
    if (response.success && response.data) {
      Storage.setToken(response.data.token);
      Storage.setUser(response.data.user);
      
      // Auto cache the user full name as active farm name for consistency
      localStorage.setItem('poultry_active_farm', response.data.user.fullName + "'s Farm");
      return response.data;
    }
    throw new Error(response.message || 'Login failed');
  },

  logout() {
    try {
      Api.post('auth/logout').catch(() => {});
    } catch (e) {}
    Storage.clearSession();
    window.location.href = 'login.html';
  },

  getCurrentUser() {
    return Storage.getUser();
  },

  isAuthenticated() {
    const token = Storage.getToken();
    if (!token) return false;
    
    // Check if token payload is unexpired
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        Storage.clearSession();
        return false;
      }
      
      const payload = JSON.parse(atob(parts[1]));
      const exp = payload.exp;
      if (exp && Date.now() >= exp * 1000) {
        Storage.clearSession();
        return false;
      }
    } catch (e) {
      Storage.clearSession();
      return false;
    }
    return true;
  },

  hasRole(requiredRoles = []) {
    const user = Storage.getUser();
    if (!user) return false;
    const activeRole = user.currentFarmRole || user.role;
    return requiredRoles.includes(activeRole);
  }
};
