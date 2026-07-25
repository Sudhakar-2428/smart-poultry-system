import { CONFIG } from './config.js';
import { Storage } from './storage.js';

let activeRequestsCount = 0;

function showLoadingIndicator() {
  activeRequestsCount++;
  let loader = document.getElementById('api-global-loader');
  if (!loader) {
    loader = document.createElement('div');
    loader.id = 'api-global-loader';
    loader.style.position = 'fixed';
    loader.style.top = '0';
    loader.style.left = '0';
    loader.style.width = '100%';
    loader.style.height = '4px';
    loader.style.background = 'linear-gradient(90deg, #FBC02D, #FFB300, #4CAF50)';
    loader.style.zIndex = '9999';
    loader.style.animation = 'api-loader-slide 1.5s infinite linear';
    
    const style = document.createElement('style');
    style.id = 'api-loader-style';
    style.innerHTML = `
      @keyframes api-loader-slide {
        0% { background-position: 0% 50%; }
        100% { background-position: 100% 50%; }
      }
    `;
    document.head.appendChild(style);
    document.body.appendChild(loader);
  }
  loader.style.display = 'block';
  disableAllSubmitButtons(true);
}

function hideLoadingIndicator() {
  activeRequestsCount = Math.max(0, activeRequestsCount - 1);
  if (activeRequestsCount === 0) {
    const loader = document.getElementById('api-global-loader');
    if (loader) {
      loader.style.display = 'none';
    }
    disableAllSubmitButtons(false);
  }
}

function disableAllSubmitButtons(disable) {
  const buttons = document.querySelectorAll('button[type="submit"], input[type="submit"]');
  buttons.forEach(btn => {
    btn.disabled = disable;
  });
}

// Global Error Manager & Toast Deduplication Manager
class GlobalErrorManager {
  constructor() {
    this.pendingModuleErrors = new Set();
    this.debounceTimer = null;
  }

  getModuleContextMessage() {
    const path = window.location.pathname.toLowerCase();
    if (path.includes('dashboard') || path.endsWith('/') || path.endsWith('index.html')) return 'Unable to load dashboard statistics.';
    if (path.includes('reports')) return 'Unable to load reports.';
    if (path.includes('finance')) return 'Unable to load finance data.';
    if (path.includes('feed-management')) return 'Unable to load feed information.';
    if (path.includes('health-records')) return 'Unable to load health records.';
    if (path.includes('flock')) return 'Unable to load chicken records.';
    if (path.includes('hatching')) return 'Unable to load hatching data.';
    if (path.includes('pairing')) return 'Unable to load pairing data.';
    if (path.includes('egg-tracking')) return 'Unable to load egg records.';
    if (path.includes('settings')) return 'Unable to load farm settings.';
    return 'Some information could not be loaded.';
  }

  handleApiError(rawErrorMsg, endpoint = '', isGet = true) {
    // 1. Detailed error logged strictly in Developer Console
    console.error(`[Global Error Manager] API Failure [${endpoint}]:`, rawErrorMsg);

    // 2. Debounce and consolidate background GET failures within 3s window
    if (isGet) {
      this.pendingModuleErrors.add(this.getModuleContextMessage());

      if (this.debounceTimer) clearTimeout(this.debounceTimer);

      this.debounceTimer = setTimeout(() => {
        this.flushConsolidatedErrors();
      }, 400); // 400ms debounce captures near-simultaneous batch GET failures
      return;
    }

    // Direct user action / POST / PUT failure
    const cleanMsg = rawErrorMsg && !rawErrorMsg.includes('Failed to fetch') 
      ? rawErrorMsg 
      : 'Action failed. Please check network connection.';
    showToast(cleanMsg, 'error');
  }

  flushConsolidatedErrors() {
    this.pendingModuleErrors.forEach(msg => {
      showToast(msg, 'error');
    });
    this.pendingModuleErrors.clear();
    this.debounceTimer = null;
  }
}

export const errorManager = new GlobalErrorManager();

export function showToast(message, type = 'error') {
  let toastContainer = document.getElementById('api-toast-container');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'api-toast-container';
    toastContainer.style.cssText = 'position: fixed; bottom: 24px; right: 24px; z-index: 10000000; display: flex; flex-direction: column; gap: 10px; pointer-events: none;';
    document.body.appendChild(toastContainer);
  }

  // 1. Toast Deduplication: Prevent duplicate message if already active
  const existingToasts = Array.from(toastContainer.children);
  const isDuplicate = existingToasts.some(t => t.dataset.toastMessage === message);
  if (isDuplicate) return;

  // 2. Max Stack Limit: Keep max 3 toasts visible. Remove oldest if > 3
  if (existingToasts.length >= 3) {
    const oldest = existingToasts[0];
    oldest.style.opacity = '0';
    oldest.style.transform = 'translateY(-10px)';
    setTimeout(() => oldest.remove(), 200);
  }

  const toast = document.createElement('div');
  toast.dataset.toastMessage = message;
  toast.style.cssText = `
    padding: 14px 20px;
    border-radius: 12px;
    font-size: 0.9rem;
    font-weight: 600;
    color: #FFFFFF;
    display: flex;
    align-items: center;
    gap: 10px;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.25);
    backdrop-filter: blur(8px);
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    transform: translateY(20px);
    opacity: 0;
    pointer-events: auto;
    border: 1px solid rgba(255, 255, 255, 0.15);
  `;

  if (type === 'success') {
    toast.style.backgroundColor = '#16A34A';
    toast.innerHTML = `<i class="fa-solid fa-circle-check" style="font-size: 1.1rem;"></i> <span>${message}</span>`;
  } else if (type === 'warning') {
    toast.style.backgroundColor = '#D97706';
    toast.innerHTML = `<i class="fa-solid fa-triangle-exclamation" style="font-size: 1.1rem;"></i> <span>${message}</span>`;
  } else {
    toast.style.backgroundColor = '#DC2626';
    toast.innerHTML = `<i class="fa-solid fa-circle-exclamation" style="font-size: 1.1rem;"></i> <span>${message}</span>`;
  }

  toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.style.transform = 'translateY(0)';
    toast.style.opacity = '1';
  }, 20);

  // 3. Auto-dismiss after 5 seconds
  setTimeout(() => {
    if (toast.parentNode) {
      toast.style.transform = 'translateY(-10px)';
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 300);
    }
  }, 5000);
}

export async function request(endpoint, options = {}) {
  const url = `${CONFIG.API_BASE_URL}/${endpoint.replace(/^\//, '')}`;
  const isGet = (options.method || 'GET').toUpperCase() === 'GET';
  
  const headers = new Headers(options.headers || {});
  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const token = Storage.getToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const requestOptions = {
    ...options,
    headers
  };

  showLoadingIndicator();

  try {
    const response = await fetch(url, requestOptions);
    
    if (response.status === 401) {
      Storage.clearSession();
      const path = window.location.pathname;
      let dataMsg = 'Invalid username or password.';
      try {
        const errJson = await response.clone().json();
        if (errJson && errJson.message) {
          dataMsg = errJson.message;
        }
      } catch (e) {}

      if (!path.endsWith('login.html') && !path.endsWith('signup.html') && path !== '/' && !path.endsWith('index.html')) {
        showToast('Login session expired. Redirecting to login page...', 'warning');
        setTimeout(() => {
          window.location.href = 'login.html';
        }, 1500);
      } else {
        showToast(dataMsg, 'error');
      }
      throw new Error(dataMsg);
    }

    if (response.status === 403) {
      if (!isGet) {
        showToast('You do not have permission to access this resource or perform this action.');
      }
      throw new Error('Forbidden access');
    }

    if (response.status === 404) {
      errorManager.handleApiError('Resource not found', endpoint, isGet);
      throw new Error('Not found');
    }

    let data;
    try {
      data = await response.json();
    } catch (e) {
      data = { success: response.ok, message: 'Non-JSON server response' };
    }

    if (!response.ok) {
      const errorMsg = data.message || `Request failed with status ${response.status}`;
      errorManager.handleApiError(errorMsg, endpoint, isGet);
      throw new Error(errorMsg);
    }

    return data;
  } catch (error) {
    if (error.message === 'Failed to fetch') {
      errorManager.handleApiError('Network failure. Cannot reach backend server.', endpoint, isGet);
    }
    throw error;
  } finally {
    hideLoadingIndicator();
  }
}

export const Api = {
  get(endpoint, options = {}) {
    return request(endpoint, { ...options, method: 'GET' });
  },
  post(endpoint, body, options = {}) {
    return request(endpoint, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined
    });
  },
  put(endpoint, body, options = {}) {
    return request(endpoint, {
      ...options,
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined
    });
  },
  patch(endpoint, body, options = {}) {
    return request(endpoint, {
      ...options,
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined
    });
  },
  delete(endpoint, options = {}) {
    return request(endpoint, { ...options, method: 'DELETE' });
  },
  upload(endpoint, formData, options = {}) {
    return request(endpoint, {
      ...options,
      method: 'POST',
      body: formData
    });
  }
};
