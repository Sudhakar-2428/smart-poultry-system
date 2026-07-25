import { Api } from './api.js';
import { Storage } from './storage.js';

/**
 * WorkerService
 * Handles frontend worker management logic.
 * Connects to backend API endpoints when available, with fallback to local session state.
 */
export const WorkerService = {
  /**
   * Retrieve current active Farm ID automatically.
   * Owner never needs to manually enter Farm ID.
   */
  async getFarmId() {
    const user = Storage.getUser();
    if (user && (user.farmUniqueId || user.farmId)) {
      return user.farmUniqueId || user.farmId;
    }

    try {
      // TODO: Connect to backend API endpoint GET api/v2/farms/my-farm
      const res = await Api.get('api/v2/farms/my-farm');
      if (res && res.data && (res.data.farmUniqueId || res.data.id)) {
        return res.data.farmUniqueId || `FARM-${res.data.id}`;
      }
    } catch (e) {
      console.warn('[WorkerService] Unable to fetch farm details from API, using fallback ID');
    }

    // Fallback generated or cached Farm ID
    const cachedFarmId = localStorage.getItem('poultry_active_farm_id');
    if (cachedFarmId) return cachedFarmId;

    const newFarmId = `FARM-${Math.floor(10000 + Math.random() * 90000)}`;
    localStorage.setItem('poultry_active_farm_id', newFarmId);
    return newFarmId;
  },

  /**
   * Fetch workers list for given farm.
   * TODO: Connect to backend API endpoint GET api/v2/farms/{farmId}/workers
   */
  async getWorkers(farmId) {
    if (!farmId) farmId = await this.getFarmId();

    try {
      // TODO: Connect to backend endpoint GET api/v2/farms/{farmId}/workers
      const response = await Api.get(`api/v2/farms/${farmId}/workers`);
      if (response && Array.isArray(response.data)) {
        return response.data;
      }
    } catch (e) {
      console.info('[WorkerService] Backend worker endpoint offline, falling back to local storage state');
    }

    // Fallback: LocalStorage state
    const stored = localStorage.getItem(`poultry_workers_${farmId}`);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch (e) {
        console.error('[WorkerService] Failed to parse local workers', e);
      }
    }

    return [];
  },

  /**
   * Add a new worker.
   * TODO: Connect to backend API endpoint POST api/v2/farms/{farmId}/workers
   */
  async addWorker(farmId, workerData) {
    if (!farmId) farmId = await this.getFarmId();

    const newWorker = {
      id: `WRK-${Date.now().toString().slice(-5)}`,
      farmId: farmId,
      name: workerData.name,
      email: workerData.email,
      phone: workerData.phone,
      role: workerData.role,
      status: workerData.status || 'Active',
      createdAt: new Date().toISOString()
    };

    try {
      // TODO: Connect to backend endpoint POST api/v2/farms/{farmId}/workers
      const response = await Api.post(`api/v2/farms/${farmId}/workers`, newWorker);
      if (response && response.success && response.data) {
        return response.data;
      }
    } catch (e) {
      console.info('[WorkerService] Backend add worker API offline, saving to local state');
    }

    // Fallback local save
    const workers = await this.getWorkers(farmId);
    workers.unshift(newWorker);
    localStorage.setItem(`poultry_workers_${farmId}`, JSON.stringify(workers));
    return newWorker;
  },

  /**
   * Update existing worker details.
   * TODO: Connect to backend API endpoint PUT api/v2/farms/{farmId}/workers/{workerId}
   */
  async updateWorker(farmId, workerId, workerData) {
    if (!farmId) farmId = await this.getFarmId();

    try {
      // TODO: Connect to backend endpoint PUT api/v2/farms/{farmId}/workers/{workerId}
      const response = await Api.put(`api/v2/farms/${farmId}/workers/${workerId}`, workerData);
      if (response && response.success && response.data) {
        return response.data;
      }
    } catch (e) {
      console.info('[WorkerService] Backend update worker API offline, updating local state');
    }

    // Fallback local update
    let workers = await this.getWorkers(farmId);
    workers = workers.map(w => {
      if (w.id === workerId) {
        return { ...w, ...workerData };
      }
      return w;
    });
    localStorage.setItem(`poultry_workers_${farmId}`, JSON.stringify(workers));
    return workers.find(w => w.id === workerId);
  },

  /**
   * Remove a worker.
   * TODO: Connect to backend API endpoint DELETE api/v2/farms/{farmId}/workers/{workerId}
   */
  async deleteWorker(farmId, workerId) {
    if (!farmId) farmId = await this.getFarmId();

    try {
      // TODO: Connect to backend endpoint DELETE api/v2/farms/{farmId}/workers/{workerId}
      await Api.delete(`api/v2/farms/${farmId}/workers/${workerId}`);
    } catch (e) {
      console.info('[WorkerService] Backend delete worker API offline, removing from local state');
    }

    // Fallback local delete
    let workers = await this.getWorkers(farmId);
    workers = workers.filter(w => w.id !== workerId);
    localStorage.setItem(`poultry_workers_${farmId}`, JSON.stringify(workers));
    return true;
  }
};
