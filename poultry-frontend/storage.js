export const Storage = {
  getToken() {
    return localStorage.getItem('poultry_jwt_token');
  },
  
  setToken(token) {
    if (token) {
      localStorage.setItem('poultry_jwt_token', token);
    } else {
      localStorage.removeItem('poultry_jwt_token');
    }
  },
  
  getUser() {
    const user = localStorage.getItem('poultry_user_info');
    if (!user) return null;
    try {
      return JSON.parse(user);
    } catch (e) {
      console.error('Error parsing user info', e);
      return null;
    }
  },
  
  setUser(user) {
    if (user) {
      localStorage.setItem('poultry_user_info', JSON.stringify(user));
    } else {
      localStorage.removeItem('poultry_user_info');
    }
  },
  
  clearSession() {
    localStorage.removeItem('poultry_jwt_token');
    localStorage.removeItem('poultry_user_info');
    localStorage.removeItem('poultry_active_farm');
    localStorage.removeItem('poultry_registered_location');
    localStorage.removeItem('poultry_farm_location');
    localStorage.removeItem('poultry_initial_logs');
  }
};
