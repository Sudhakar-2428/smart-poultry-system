import { Api } from './api.js';

const BREEDS = { "Cobb 500": "COBB_500", "Ross 308": "ROSS_308", "Hubbard": "HUBBARD", "White Leghorn": "LEGHORN", "Rhode Island Red": "RHODE_ISLAND_RED", "Plymouth Rock": "PLYMOUTH_ROCK", "Light Sussex": "SUSSEX", "Brama": "BRAMA", "Peruvidai": "OTHER", "Siruvidai": "OTHER", "Cross": "OTHER", "Other": "OTHER" };
const BREEDS_REV = Object.fromEntries(Object.entries(BREEDS).map(([k,v])=>[v,k]));
const CATS = { "Broiler": "BROILER", "Layer": "LAYER", "Country Chicken": "OTHER", "Other": "OTHER" };
const CATS_REV = { "BROILER": "Broiler", "LAYER": "Layer", "BREEDER": "Other", "CHICK": "Other", "ROOSTER": "Other", "OTHER": "Other" };
const STATUS = { "Laying": "ACTIVE", "Breeding": "ACTIVE", "Meat": "ACTIVE", "Molting": "ACTIVE", "Ready for Sale": "ACTIVE", "Sold": "SOLD", "Dead": "DEAD", "Removed from Farm": "INACTIVE" };
const STATUS_REV = { "ACTIVE": "Laying", "BROODER": "Young Chick", "GROWING": "Young Chick", "SOLD": "Sold", "DEAD": "Dead", "INACTIVE": "Removed from Farm" };

document.addEventListener("DOMContentLoaded", () => {
  let birdsData = [];
  let currentStartLayingBird = null;
  let searchQuery = "";
  let activeQuickFilter = "all";
  let isAdvancedOpen = false;
  let editTargetId = null;
  let wasViewingDetailsBeforeEdit = false;
  let searchPage = 0;
  const searchPageSize = 24;
  let searchTotalPages = 1;

  const listWorkspace = document.getElementById("flock-list-workspace");
  const formWorkspace = document.getElementById("flock-form-workspace");
  const detailWorkspace = document.getElementById("flock-detail-workspace");
  const gridDeck = document.getElementById("flock-grid-deck");
  const tableBody = document.getElementById("flock-table-tbody");
  const listTableDeck = document.getElementById("flock-table-deck");
  const visualViewport = document.getElementById("flock-visual-viewport");
  const emptyStateBlock = document.getElementById("flock-empty-block");

  const wildSearchInput = document.getElementById("flock-wild-search");
  const quickTagsGroup = document.getElementById("quick-filter-selector");
  const advFiltersPanel = document.getElementById("advanced-filters-drawer-content");
  const btnToggleAdv = document.getElementById("btn-toggle-adv-filters");

  const cardTotal = document.getElementById("card-total-birds");
  const cardHealthy = document.getElementById("card-healthy-val");
  const cardHens = document.getElementById("card-hens-val");
  const cardRoosters = document.getElementById("card-roosters-val");

  const advGender = document.getElementById("filter-gender");
  const advBreed = document.getElementById("filter-breed");
  const advHealth = document.getElementById("filter-health");
  const advStatus = document.getElementById("filter-status");
  const advCategory = document.getElementById("filter-category");
  const advOrigin = document.getElementById("filter-origin");
  const advAgeGroup = document.getElementById("filter-age-group");
  const activeFiltersQty = document.getElementById("active-filters-qty");

  function toggleAcquisitionFields() {
    const source = document.getElementById("fm-source").value;
    const dobInput = document.getElementById("fm-dob");
    const dobLabel = document.getElementById("fm-dob-label");
    const acqFields = document.getElementById("fm-acq-details-fields");
    if (source === "Farm Born") {
      if (acqFields) acqFields.style.display = "none";
      if (dobInput) dobInput.required = true;
      if (dobLabel) dobLabel.innerHTML = 'Date of Birth <span class="text-rose">*</span>';
    } else {
      if (acqFields) acqFields.style.display = "flex";
      if (dobInput) dobInput.required = false;
      if (dobLabel) dobLabel.textContent = 'Date of Birth (if known)';
    }
  }
  document.getElementById("fm-source").addEventListener("change", toggleAcquisitionFields);

  if (window.makePremiumSelect) {
    ["fm-gender", "fm-health", "filter-category", "filter-origin", "filter-age-group"].forEach(id => window.makePremiumSelect(id));
    window.makePremiumSelect("fm-category", (val) => {
      const breedSel = document.getElementById("fm-breed");
      const breedCont = document.getElementById("fm-breed-container");
      if (breedSel && breedCont) {
        breedCont.classList.remove("visible");
        setTimeout(() => {
          breedSel.innerHTML = '<option value="" disabled selected hidden></option>';
          if (val && window.BREED_CATEGORIES[val]) {
            window.BREED_CATEGORIES[val].forEach(b => {
              const opt = document.createElement("option");
              opt.value = b.value;
              opt.textContent = b.text;
              breedSel.appendChild(opt);
            });
            window.makePremiumSelect("fm-breed");
            breedCont.classList.add("visible");
          }
        }, val ? 200 : 0);
      }
    });
  }

  function switchView(target) {
    const backdrop = document.getElementById("drawer-backdrop");
    if (target === formWorkspace) {
      listWorkspace.style.opacity = "0.5";
      listWorkspace.style.pointerEvents = "none";
      detailWorkspace.style.display = "none";
      formWorkspace.style.display = "block";
      setTimeout(() => {
        formWorkspace.classList.add("open");
        if (backdrop) backdrop.classList.add("open");
      }, 20);
    } else {
      formWorkspace.classList.remove("open");
      if (backdrop) backdrop.classList.remove("open");
      listWorkspace.style.opacity = "1";
      listWorkspace.style.pointerEvents = "auto";
      setTimeout(() => {
        if (!formWorkspace.classList.contains("open")) formWorkspace.style.display = "none";
      }, 450);
      
      if (target === detailWorkspace) {
        listWorkspace.style.display = "none";
        detailWorkspace.style.display = "block";
        window.scrollTo({ top: 0, behavior: "smooth" });
      } else {
        listWorkspace.style.display = "block";
        detailWorkspace.style.display = "none";
      }
    }
  }

  document.getElementById("btn-trigger-add-view").addEventListener("click", () => openFormWorkspace(null));
  document.getElementById("btn-empty-add-view-trigger").addEventListener("click", () => openFormWorkspace(null));
  
  const drawerBackdrop = document.getElementById("drawer-backdrop");
  if (drawerBackdrop) {
    drawerBackdrop.addEventListener("click", () => {
      const startLayingDrawer = document.getElementById("start-laying-drawer");
      if (startLayingDrawer && startLayingDrawer.classList.contains("open")) {
        window.closeStartLayingDrawer();
        return;
      }
      switchView(wasViewingDetailsBeforeEdit ? detailWorkspace : listWorkspace);
    });
  }
  
  const btnCancelTop = document.getElementById("btn-form-cancel-top");
  if (btnCancelTop) btnCancelTop.addEventListener("click", () => switchView(wasViewingDetailsBeforeEdit ? detailWorkspace : listWorkspace));
  document.getElementById("btn-form-cancel").addEventListener("click", () => switchView(wasViewingDetailsBeforeEdit ? detailWorkspace : listWorkspace));
  document.getElementById("btn-detail-back").addEventListener("click", () => switchView(listWorkspace));

  window.openStartLayingDrawer = (bird) => {
    currentStartLayingBird = bird;
    const startLayingDrawer = document.getElementById("start-laying-drawer");
    const backdrop = document.getElementById("drawer-backdrop");

    document.getElementById("laying-hen-id").value = bird.id;
    document.getElementById("laying-hen-name").textContent = bird.name;
    document.getElementById("laying-hen-id-label").innerHTML = `ID: <strong>${bird.id}</strong>`;
    
    let emoji = bird.gender === "Rooster" ? "🐓" : (bird.category === "Chick" ? "🐥" : "🐔");
    document.getElementById("laying-hen-photo").textContent = emoji;
    document.getElementById("laying-hen-breed").textContent = bird.breed;
    document.getElementById("laying-hen-age").textContent = bird.ageText || "N/A";
    
    const healthBadge = document.getElementById("laying-hen-health");
    healthBadge.textContent = bird.health;
    healthBadge.className = `chk-status-pill ${bird.health === "Healthy" ? "healthy" : "treatment"}`;

    const localDate = new Date();
    const todayStr = new Date(localDate - localDate.getTimezoneOffset() * 60000).toISOString().split('T')[0];
    document.getElementById("laying-start-date").value = todayStr;

    listWorkspace.style.opacity = "0.5";
    listWorkspace.style.pointerEvents = "none";
    
    startLayingDrawer.style.display = "flex";
    if (backdrop) {
      backdrop.style.display = "block";
      setTimeout(() => {
        startLayingDrawer.classList.add("open");
        backdrop.classList.add("open");
      }, 20);
    }
  };

  window.closeStartLayingDrawer = () => {
    const startLayingDrawer = document.getElementById("start-laying-drawer");
    const backdrop = document.getElementById("drawer-backdrop");
    if (startLayingDrawer) startLayingDrawer.classList.remove("open");
    if (backdrop) backdrop.classList.remove("open");
    listWorkspace.style.opacity = "1";
    listWorkspace.style.pointerEvents = "auto";
    setTimeout(() => {
      if (startLayingDrawer && !startLayingDrawer.classList.contains("open")) {
        startLayingDrawer.style.display = "none";
        if (backdrop) backdrop.style.display = "none";
      }
    }, 450);
  };

  document.getElementById("btn-laying-cancel-top").addEventListener("click", window.closeStartLayingDrawer);
  document.getElementById("btn-laying-cancel").addEventListener("click", window.closeStartLayingDrawer);

  document.getElementById("form-start-laying").addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!currentStartLayingBird) return;
    const startDate = document.getElementById("laying-start-date").value;
    try {
      const res = await Api.get(`chickens/${currentStartLayingBird.dbId}`);
      if (res && res.success) {
        const chk = res.data;
        const updatePayload = {
          chickenCode: chk.chickenCode,
          name: chk.name,
          breed: chk.breed,
          category: chk.category,
          gender: chk.gender,
          dateOfBirth: chk.dateOfBirth,
          weight: chk.weight,
          color: chk.color,
          status: "ACTIVE", 
          remarks: `[Egg Laying Cycles: Started ${startDate}] ${chk.remarks || ''}`
        };
        await Api.put(`chickens/${currentStartLayingBird.dbId}`, updatePayload);
        showSuccessToast(`Egg laying cycle started for ${chk.name} (${currentStartLayingBird.id}).`);
        window.closeStartLayingDrawer();
        loadChickensList();
        if (detailWorkspace.style.display !== "none") {
          openDetailWorkspace(currentStartLayingBird);
        }
      }
    } catch (error) {
      console.error(error);
    }
  });

  function showListLoadingState() {
    const isGrid = visualViewport.classList.contains("view-mode-grid");
    if (isGrid) {
      gridDeck.style.display = "grid";
      listTableDeck.style.display = "none";
      emptyStateBlock.style.display = "none";
      gridDeck.innerHTML = `<div class="flock-skeleton-loader" style="grid-column: 1 / -1;"><div class="skele-grid">${Array(8).fill(0).map(() => `<div class="skele-card"><div class="skele-img"></div><div class="skele-strip w80"></div><div class="skele-strip w60"></div></div>`).join('')}</div></div>`;
    } else {
      gridDeck.style.display = "none";
      listTableDeck.style.display = "block";
      emptyStateBlock.style.display = "none";
      tableBody.innerHTML = `<tr><td colspan="10" style="text-align: center; padding: 40px 0;"><i class="fa-solid fa-spinner fa-spin-pulse" style="font-size: 1.5rem; color: var(--primary-green);"></i><p style="margin-top: 8px; color: var(--neutral-gray); font-size: 0.85rem;">Retrieving flock database records...</p></td></tr>`;
    }
  }

  let selectedChickenIds = new Set();
  const filterSortBy = document.getElementById("filter-sort-by");

  async function fetchDashboardStats() {
    try {
      const res = await Api.get("chickens/stats");
      if (res && res.success && res.data) {
        const stats = res.data;
        const setVal = (id, val) => {
          const el = document.getElementById(id);
          if (el) el.textContent = val !== undefined ? val : 0;
        };
        setVal("card-total-birds", stats.totalChickens);
        setVal("card-healthy-val", stats.healthy);
        setVal("card-sick-val", stats.sick);
        setVal("card-sold-val", stats.sold);
        setVal("card-dead-val", stats.dead);
        setVal("card-hens-val", stats.hens);
        setVal("card-roosters-val", stats.roosters);
        setVal("card-country-birds", stats.countryChickens);
        setVal("card-broiler-birds", stats.broilers);
        setVal("card-layer-birds", stats.layers);
        setVal("card-recent-birds", stats.recentlyRegistered);
      }
    } catch (e) {
      console.warn("Failed to fetch dashboard stats:", e);
    }
  }

  function showListLoadingState() {
    const isGrid = visualViewport.classList.contains("view-mode-grid");
    const skele = document.getElementById("flock-skeleton-loader");
    if (skele) skele.style.display = "grid";
    gridDeck.style.display = "none";
    listTableDeck.style.display = "none";
    emptyStateBlock.style.display = "none";
  }

  async function loadChickensList() {
    showListLoadingState();
    fetchDashboardStats();
    try {
      let queryParams = [`page=${searchPage}`, `size=${searchPageSize}`];
      
      const sortVal = filterSortBy ? filterSortBy.value : "newest";
      if (sortVal === "oldest") queryParams.push(`sort=id,asc`);
      else if (sortVal === "age") queryParams.push(`sort=dateOfBirth,asc`);
      else if (sortVal === "weight") queryParams.push(`sort=weight,desc`);
      else if (sortVal === "chickenId") queryParams.push(`sort=chickenCode,asc`);
      else queryParams.push(`sort=id,desc`);

      if (searchQuery) {
        queryParams.push(`search=${encodeURIComponent(searchQuery)}`);
      }
      if (activeQuickFilter !== "all") {
        if (activeQuickFilter === "Hen") queryParams.push(`gender=FEMALE`);
        else if (activeQuickFilter === "Rooster") queryParams.push(`gender=MALE`);
        else if (activeQuickFilter === "Country Chicken") queryParams.push(`category=COUNTRY_CHICKEN`);
        else if (activeQuickFilter === "Broiler") queryParams.push(`category=BROILER`);
        else if (activeQuickFilter === "Layer") queryParams.push(`category=LAYER`);
        else if (activeQuickFilter === "Healthy") queryParams.push(`healthStatus=HEALTHY`);
        else if (activeQuickFilter === "Sick") queryParams.push(`healthStatus=SICK`);
        else if (activeQuickFilter === "Sold") queryParams.push(`status=SOLD`);
        else if (activeQuickFilter === "Dead") queryParams.push(`status=DEAD`);
      }
      if (advGender && advGender.value) {
        queryParams.push(`gender=${advGender.value === "Hen" ? "FEMALE" : (advGender.value === "Rooster" ? "MALE" : "UNKNOWN")}`);
      }
      if (advBreed && advBreed.value) queryParams.push(`breed=${BREEDS[advBreed.value] || "OTHER"}`);
      if (advCategory && advCategory.value) queryParams.push(`category=${CATS[advCategory.value] || "OTHER"}`);
      if (advStatus && advStatus.value) queryParams.push(`status=${advStatus.value.toUpperCase()}`);
      if (advHealth && advHealth.value) {
        let hVal = advHealth.value.toUpperCase().replace(/\s+/g, '_');
        if (hVal === "UNDER_OBSERVATION") hVal = "OBSERVATION";
        if (hVal === "IN_TREATMENT") hVal = "UNDER_TREATMENT";
        queryParams.push(`healthStatus=${hVal}`);
      }
      if (advOrigin && advOrigin.value) queryParams.push(`origin=${advOrigin.value.toUpperCase().replace(/\s+/g, '_')}`);
      if (advAgeGroup && advAgeGroup.value) queryParams.push(`ageGroup=${advAgeGroup.value}`);

      const response = await Api.get(`chickens?${queryParams.join('&')}`);
      const skele = document.getElementById("flock-skeleton-loader");
      if (skele) skele.style.display = "none";

      if (response && response.success && response.data) {
        const pageData = response.data;
        birdsData = pageData.content.map(item => {
          let origin = item.origin ? (item.origin === "FARM_BORN" ? "Farm Born" : "Purchased") : "Farm Born";
          let notes = item.remarks || "";
          let ageText = "N/A";
          if (item.ageInDays !== null && item.ageInDays !== undefined) {
            if (item.ageInDays < 60) ageText = `${item.ageInDays} Days`;
            else if (item.ageInMonths !== null) ageText = `${item.ageInMonths} Months`;
          }
          return {
            id: item.chickenCode, dbId: item.id, name: item.name || item.chickenCode,
            gender: item.gender === "MALE" ? "Rooster" : (item.gender === "FEMALE" ? "Hen" : "Unknown"),
            category: CATS_REV[item.category] || item.category || "Other",
            breed: BREEDS_REV[item.breed] || item.breed || "Other",
            dob: item.dateOfBirth, weight: item.weight || 0.0,
            health: item.healthStatus ? item.healthStatus.replace(/_/g, ' ') : "Healthy",
            status: item.status ? item.status : "ACTIVE",
            source: origin, band: item.legBandNumber || "None", wingTag: item.wingTagNumber || "None",
            photoUrl: item.photoUrl, notes, ageText
          };
        });

        searchTotalPages = pageData.totalPages || 1;
        renderListLayoutsFromData();
        renderPaginationControls(pageData);
      }
    } catch (e) {
      console.error(e);
      const skele = document.getElementById("flock-skeleton-loader");
      if (skele) skele.style.display = "none";
      emptyStateBlock.style.display = "block";
    }
  }

  function updateBulkToolbarUI() {
    const toolbar = document.getElementById("bulk-actions-toolbar");
    const countEl = document.getElementById("bulk-selected-count");
    if (!toolbar) return;
    if (selectedChickenIds.size > 0) {
      toolbar.style.display = "flex";
      if (countEl) countEl.textContent = selectedChickenIds.size;
    } else {
      toolbar.style.display = "none";
    }

    const allChecked = birdsData.length > 0 && birdsData.every(b => selectedChickenIds.has(b.dbId));
    const chkSelectAll = document.getElementById("chk-select-all");
    const chkTableSelectAll = document.getElementById("chk-table-select-all");
    if (chkSelectAll) chkSelectAll.checked = allChecked;
    if (chkTableSelectAll) chkTableSelectAll.checked = allChecked;
  }

  function renderListLayoutsFromData() {
    if (birdsData.length === 0) {
      gridDeck.style.display = "none";
      listTableDeck.style.display = "none";
      emptyStateBlock.style.display = "block";
      updateBulkToolbarUI();
      return;
    }
    emptyStateBlock.style.display = "none";
    const isGrid = visualViewport.classList.contains("view-mode-grid");
    gridDeck.style.display = isGrid ? "grid" : "none";
    listTableDeck.style.display = isGrid ? "none" : "block";

    gridDeck.innerHTML = "";
    birdsData.forEach(b => {
      const card = document.createElement("div");
      card.className = "flock-card reveal-on-scroll revealed";
      card.style.position = "relative";
      let emoji = b.gender === "Rooster" ? "🐓" : (b.category === "Chick" ? "🐥" : "🐔");
      let healthVal = b.health.toLowerCase().includes("healthy") ? "healthy" : "treatment";
      let statusVal = b.status.toLowerCase();
      let isChecked = selectedChickenIds.has(b.dbId);

      card.innerHTML = `
        <div style="position: absolute; top: 12px; left: 12px; z-index: 2;">
          <input type="checkbox" class="chk-select-item" data-dbid="${b.dbId}" ${isChecked ? 'checked' : ''} style="width: 18px; height: 18px; accent-color: #16A34A; cursor: pointer;">
        </div>
        <div style="position: absolute; top: 12px; right: 12px; z-index: 2; display: flex; gap: 6px; align-items: center;">
          <button class="btn-qr-trigger" title="View QR Code" style="background: rgba(255,255,255,0.85); border: 1px solid #CBD5E1; border-radius: 50%; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #1E293B;">
            <i class="fa-solid fa-qrcode"></i>
          </button>
          <div class="dropdown" style="position: relative;">
            <button class="btn-three-dot" style="background: rgba(255,255,255,0.85); border: 1px solid #CBD5E1; border-radius: 50%; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #1E293B;">
              <i class="fa-solid fa-ellipsis-vertical"></i>
            </button>
            <div class="dropdown-menu-list" style="display: none; position: absolute; right: 0; top: 36px; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 8px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); width: 160px; z-index: 10; padding: 6px 0;">
              <a href="#" class="action-item btn-view-bio" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-eye" style="width:16px;"></i> View Profile</a>
              <a href="#" class="action-item btn-edit-bio" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-pen-to-square" style="width:16px;"></i> Edit Profile</a>
              <a href="health-records.html?code=${b.id}" class="action-item" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-heart-pulse" style="width:16px;"></i> Health Records</a>
              <a href="egg-tracking.html?code=${b.id}" class="action-item" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-egg" style="width:16px;"></i> Egg Records</a>
              <a href="#" class="action-item btn-print-card" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-print" style="width:16px;"></i> Print Card</a>
              <a href="#" class="action-item btn-archive-bird" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #D97706; text-decoration: none;"><i class="fa-solid fa-box-archive" style="width:16px;"></i> Archive</a>
              <a href="#" class="action-item btn-delete-bio" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #DC2626; text-decoration: none;"><i class="fa-solid fa-trash-can" style="width:16px;"></i> Delete</a>
            </div>
          </div>
        </div>

        <div class="flock-card-photo-box">${b.photoUrl ? `<img src="${b.photoUrl}" style="width:100%; height:100%; object-fit:cover; border-radius:12px;">` : emoji}</div>
        <div class="flock-card-info">
          <div class="flock-card-header-row">
            <h4>${b.name}</h4>
            <span class="chk-id-badge" style="cursor: pointer;" title="Click to view details">${b.id}</span>
          </div>
          <span class="flock-card-subspec">Category: <strong>${b.category}</strong></span>
          <span class="flock-card-subspec">Breed: ${b.breed}</span>
          <span class="flock-card-subspec">Gender: ${b.gender}</span>
          <span class="flock-card-subspec">Age: ${b.ageText} | Weight: ${b.weight} kg</span>
          <span class="flock-card-subspec">Origin: ${b.source}</span>
          <div class="flock-card-status-badges" style="margin-top: 8px;">
            <span class="chk-status-pill ${healthVal}">${b.health}</span>
            <span class="chk-status-pill ${statusVal}">${b.status}</span>
          </div>
        </div>
      `;

      // Checkbox listener
      card.querySelector(".chk-select-item").addEventListener("change", (e) => {
        if (e.target.checked) selectedChickenIds.add(b.dbId);
        else selectedChickenIds.delete(b.dbId);
        updateBulkToolbarUI();
      });

      // Three-dot dropdown toggler
      const dotBtn = card.querySelector(".btn-three-dot");
      const dropMenu = card.querySelector(".dropdown-menu-list");
      dotBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        document.querySelectorAll(".dropdown-menu-list").forEach(m => { if (m !== dropMenu) m.style.display = "none"; });
        dropMenu.style.display = dropMenu.style.display === "none" ? "block" : "none";
      });

      // Action items
      card.querySelector(".chk-id-badge").addEventListener("click", () => openDetailWorkspace(b));
      card.querySelector(".btn-view-bio").addEventListener("click", (e) => { e.preventDefault(); openDetailWorkspace(b); });
      card.querySelector(".btn-edit-bio").addEventListener("click", (e) => { e.preventDefault(); openFormWorkspace(b); });
      card.querySelector(".btn-print-card").addEventListener("click", (e) => { e.preventDefault(); if (window.triggerPrintCard) window.triggerPrintCard(b.dbId); });
      card.querySelector(".btn-archive-bird").addEventListener("click", async (e) => {
        e.preventDefault();
        if (confirm(`Archive chicken ${b.id}?`)) {
          await Api.post("chickens/bulk-archive", { ids: [b.dbId] });
          showSuccessToast(`Chicken ${b.id} archived.`);
          loadChickensList();
        }
      });
      card.querySelector(".btn-delete-bio").addEventListener("click", (e) => { e.preventDefault(); deleteBird(b.dbId, b.id); });
      card.querySelector(".btn-qr-trigger").addEventListener("click", () => {
        if (window.showChickenQrModal) window.showChickenQrModal(b);
      });

      gridDeck.appendChild(card);
    });

    // Close dropdowns on outside click
    document.addEventListener("click", () => {
      document.querySelectorAll(".dropdown-menu-list").forEach(m => m.style.display = "none");
    });

    tableBody.innerHTML = "";
    birdsData.forEach(b => {
      const tr = document.createElement("tr");
      tr.className = "flock-table-row";
      let emoji = b.gender === "Rooster" ? "🐓" : (b.category === "Chick" ? "🐥" : "🐔");
      let healthVal = b.health.toLowerCase().includes("healthy") ? "healthy" : "treatment";
      let statusVal = b.status.toLowerCase();
      let isChecked = selectedChickenIds.has(b.dbId);

      tr.innerHTML = `
        <td style="text-align: center;"><input type="checkbox" class="chk-select-item" data-dbid="${b.dbId}" ${isChecked ? 'checked' : ''} style="width: 18px; height: 18px; accent-color: #16A34A; cursor: pointer;"></td>
        <td><span class="table-emoji-avatar">${b.photoUrl ? `<img src="${b.photoUrl}" style="width:36px; height:36px; object-fit:cover; border-radius:50%;">` : emoji}</span></td>
        <td><strong class="text-green" style="cursor: pointer;" title="Click to view details">${b.id}</strong></td>
        <td>${b.category}</td>
        <td>${b.breed}</td>
        <td>${b.gender}</td>
        <td>${b.ageText}</td>
        <td>${b.weight} kg</td>
        <td><span class="chk-status-pill ${healthVal}">${b.health}</span></td>
        <td><span class="chk-status-pill ${statusVal}">${b.status}</span></td>
        <td>${b.source}</td>
        <td style="text-align: right;">
          <div style="display: flex; gap: 6px; justify-content: flex-end; align-items: center;">
            <button class="btn-qr-trigger-tb" title="View QR Code" style="background: #F1F5F9; border: 1px solid #CBD5E1; border-radius: 50%; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #1E293B;">
              <i class="fa-solid fa-qrcode" style="font-size: 0.75rem;"></i>
            </button>
            <div class="dropdown" style="position: relative;">
              <button class="btn-three-dot-tb" style="background: #F1F5F9; border: 1px solid #CBD5E1; border-radius: 50%; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #1E293B;">
                <i class="fa-solid fa-ellipsis-vertical" style="font-size: 0.75rem;"></i>
              </button>
              <div class="dropdown-menu-list-tb" style="display: none; position: absolute; right: 0; top: 34px; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 8px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); width: 160px; z-index: 10; padding: 6px 0; text-align: left;">
                <a href="#" class="action-item btn-view-bio-tb" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-eye" style="width:16px;"></i> View Profile</a>
                <a href="#" class="action-item btn-edit-bio-tb" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-pen-to-square" style="width:16px;"></i> Edit Profile</a>
                <a href="health-records.html?code=${b.id}" class="action-item" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-heart-pulse" style="width:16px;"></i> Health Records</a>
                <a href="egg-tracking.html?code=${b.id}" class="action-item" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-egg" style="width:16px;"></i> Egg Records</a>
                <a href="#" class="action-item btn-print-card-tb" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #334155; text-decoration: none;"><i class="fa-solid fa-print" style="width:16px;"></i> Print Card</a>
                <a href="#" class="action-item btn-archive-bird-tb" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #D97706; text-decoration: none;"><i class="fa-solid fa-box-archive" style="width:16px;"></i> Archive</a>
                <a href="#" class="action-item btn-delete-bio-tb" style="display: flex; align-items: center; gap: 8px; padding: 8px 14px; font-size: 0.8rem; color: #DC2626; text-decoration: none;"><i class="fa-solid fa-trash-can" style="width:16px;"></i> Delete</a>
              </div>
            </div>
          </div>
        </td>
      `;

      tr.querySelector(".chk-select-item").addEventListener("change", (e) => {
        if (e.target.checked) selectedChickenIds.add(b.dbId);
        else selectedChickenIds.delete(b.dbId);
        updateBulkToolbarUI();
      });

      const dotBtnT = tr.querySelector(".btn-three-dot-tb");
      const dropMenuT = tr.querySelector(".dropdown-menu-list-tb");
      dotBtnT.addEventListener("click", (e) => {
        e.stopPropagation();
        document.querySelectorAll(".dropdown-menu-list-tb").forEach(m => { if (m !== dropMenuT) m.style.display = "none"; });
        dropMenuT.style.display = dropMenuT.style.display === "none" ? "block" : "none";
      });

      tr.querySelector(".text-green").addEventListener("click", () => openDetailWorkspace(b));
      tr.querySelector(".btn-view-bio-tb").addEventListener("click", (e) => { e.preventDefault(); openDetailWorkspace(b); });
      tr.querySelector(".btn-edit-bio-tb").addEventListener("click", (e) => { e.preventDefault(); openFormWorkspace(b); });
      tr.querySelector(".btn-print-card-tb").addEventListener("click", (e) => { e.preventDefault(); if (window.triggerPrintCard) window.triggerPrintCard(b.dbId); });
      tr.querySelector(".btn-archive-bird-tb").addEventListener("click", async (e) => {
        e.preventDefault();
        if (confirm(`Archive chicken ${b.id}?`)) {
          await Api.post("chickens/bulk-archive", { ids: [b.dbId] });
          showSuccessToast(`Chicken ${b.id} archived.`);
          loadChickensList();
        }
      });
      tr.querySelector(".btn-delete-bio-tb").addEventListener("click", (e) => { e.preventDefault(); deleteBird(b.dbId, b.id); });
      tr.querySelector(".btn-qr-trigger-tb").addEventListener("click", () => {
        if (window.showChickenQrModal) window.showChickenQrModal(b);
      });

      tableBody.appendChild(tr);
    });

    updateBulkToolbarUI();
  }

  // Select All handlers
  const handleSelectAll = (isChecked) => {
    if (isChecked) {
      birdsData.forEach(b => selectedChickenIds.add(b.dbId));
    } else {
      birdsData.forEach(b => selectedChickenIds.delete(b.dbId));
    }
    renderListLayoutsFromData();
  };
  const chkSelectAll = document.getElementById("chk-select-all");
  const chkTableSelectAll = document.getElementById("chk-table-select-all");
  if (chkSelectAll) chkSelectAll.addEventListener("change", (e) => handleSelectAll(e.target.checked));
  if (chkTableSelectAll) chkTableSelectAll.addEventListener("change", (e) => handleSelectAll(e.target.checked));

  // Bulk Actions
  const btnBulkArchive = document.getElementById("btn-bulk-archive");
  if (btnBulkArchive) {
    btnBulkArchive.addEventListener("click", async () => {
      if (selectedChickenIds.size === 0) return;
      if (confirm(`Are you sure you want to bulk archive ${selectedChickenIds.size} selected chickens?`)) {
        try {
          await Api.post("chickens/bulk-archive", { ids: Array.from(selectedChickenIds) });
          showSuccessToast(`Successfully archived ${selectedChickenIds.size} chickens.`);
          selectedChickenIds.clear();
          loadChickensList();
        } catch (err) {
          console.error(err);
        }
      }
    });
  }

  // CSV Export helper
  const exportChickensToCSV = (chickensList) => {
    if (!chickensList || chickensList.length === 0) return;
    const headers = ["Chicken ID", "Name", "Category", "Breed", "Gender", "Age", "Weight (kg)", "Health Status", "Status", "Origin", "Leg Band", "Wing Tag"];
    const rows = chickensList.map(b => [
      `"${b.id}"`, `"${b.name}"`, `"${b.category}"`, `"${b.breed}"`, `"${b.gender}"`, `"${b.ageText}"`,
      b.weight, `"${b.health}"`, `"${b.status}"`, `"${b.source}"`, `"${b.band}"`, `"${b.wingTag}"`
    ]);
    const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map(r => r.join(","))].join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `poultry_chickens_export_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const btnBulkExport = document.getElementById("btn-bulk-export");
  if (btnBulkExport) {
    btnBulkExport.addEventListener("click", () => {
      const selectedList = birdsData.filter(b => selectedChickenIds.has(b.dbId));
      exportChickensToCSV(selectedList.length > 0 ? selectedList : birdsData);
    });
  }

  const btnDashboardExportCsv = document.getElementById("btn-dashboard-export-csv");
  if (btnDashboardExportCsv) {
    btnDashboardExportCsv.addEventListener("click", () => exportChickensToCSV(birdsData));
  }

  // Bulk Print
  const modalBulkPrint = document.getElementById("modal-bulk-print");
  const btnBulkPrint = document.getElementById("btn-bulk-print");
  const bulkCardsContainer = document.getElementById("bulk-printable-cards-container");
  if (btnBulkPrint && modalBulkPrint && bulkCardsContainer) {
    btnBulkPrint.addEventListener("click", () => {
      const selectedList = birdsData.filter(b => selectedChickenIds.has(b.dbId));
      if (selectedList.length === 0) return;
      bulkCardsContainer.innerHTML = "";
      selectedList.forEach(b => {
        const cardBox = document.createElement("div");
        cardBox.style.cssText = "background:#FFFFFF; border:2px solid #1E293B; border-radius:12px; padding:16px; page-break-inside:avoid;";
        cardBox.innerHTML = `
          <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #E2E8F0; padding-bottom:8px; margin-bottom:10px;">
            <h4 style="margin:0; font-size:1rem; color:#1E293B;">${b.name}</h4>
            <span style="background:#16A34A; color:#FFFFFF; padding:2px 8px; border-radius:4px; font-weight:700; font-size:0.8rem;">${b.id}</span>
          </div>
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:6px; font-size:0.8rem; color:#334155;">
            <div>Category: <strong>${b.category}</strong></div>
            <div>Breed: <strong>${b.breed}</strong></div>
            <div>Gender: <strong>${b.gender}</strong></div>
            <div>Weight: <strong>${b.weight} kg</strong></div>
            <div>Health: <strong>${b.health}</strong></div>
            <div>Status: <strong>${b.status}</strong></div>
          </div>
        `;
        bulkCardsContainer.appendChild(cardBox);
      });
      modalBulkPrint.style.display = "flex";
    });

    const closeBulk = () => { modalBulkPrint.style.display = "none"; };
    const btnCloseBP = document.getElementById("btn-close-bulk-print");
    const btnCancelBP = document.getElementById("btn-cancel-bulk-print");
    if (btnCloseBP) btnCloseBP.addEventListener("click", closeBulk);
    if (btnCancelBP) btnCancelBP.addEventListener("click", closeBulk);
  }

  // Sort listener
  if (filterSortBy) {
    filterSortBy.addEventListener("change", () => {
      searchPage = 0;
      loadChickensList();
    });
  }

  // Retry listener
  const btnRetry = document.getElementById("btn-flock-retry");
  if (btnRetry) {
    btnRetry.addEventListener("click", () => {
      searchPage = 0;
      loadChickensList();
    });
  }

  function deleteBird(dbId, id) {
    if (confirm(`Are you sure you want to permanently delete chicken ${id}?`)) {
      Api.delete(`chickens/${dbId}`)
        .then(() => {
          showSuccessToast(`Chicken ${id} deleted successfully.`);
          loadChickensList();
        })
        .catch(err => console.error(err));
    }
  }

  function renderPaginationControls(pageData) {
    let pagCont = document.getElementById("flock-pagination-controls");
    if (!pagCont) {
      pagCont = document.createElement("div");
      pagCont.id = "flock-pagination-controls";
      pagCont.style.cssText = "display:flex; justify-content:center; align-items:center; gap:12px; margin-top:24px; padding:12px 24px; background:rgba(255,255,255,0.4); border-radius:16px; border:1px solid rgba(255,255,255,0.3); backdrop-filter:blur(10px); width:fit-content; margin-left:auto; margin-right:auto;";
      visualViewport.appendChild(pagCont);
    }
    if (pageData.totalPages <= 1) { pagCont.style.display = "none"; return; }
    pagCont.style.display = "flex";

    const currentNum = pageData.number;
    const total = pageData.totalPages;
    let pagesHtml = '';
    let startPage = Math.max(0, currentNum - 2);
    let endPage = Math.min(total - 1, startPage + 4);
    if (endPage - startPage < 4) startPage = Math.max(0, endPage - 4);

    for (let i = startPage; i <= endPage; i++) {
      pagesHtml += `<button class="btn btn-outline btn-page-number ${i === currentNum ? 'active' : ''}" data-page="${i}" style="min-width:32px; height:32px; padding:0; display:flex; align-items:center; justify-content:center; border-radius:50%; margin:0 2px; font-weight:600; font-size:0.8rem; ${i === currentNum ? 'background:var(--primary-green) !important; color:#fff !important; border-color:var(--primary-green) !important;' : ''}">${i + 1}</button>`;
    }

    pagCont.innerHTML = `<button class="btn btn-outline" id="btn-page-prev" ${pageData.first ? 'disabled style="opacity:0.5;"' : ''}>Prev</button><div style="display:flex;">${pagesHtml}</div><button class="btn btn-outline" id="btn-page-next" ${pageData.last ? 'disabled style="opacity:0.5;"' : ''}>Next</button>`;

    const prevB = document.getElementById("btn-page-prev");
    if (prevB && !pageData.first) prevB.addEventListener("click", () => { searchPage = currentNum - 1; loadChickensList(); });
    const nextB = document.getElementById("btn-page-next");
    if (nextB && !pageData.last) nextB.addEventListener("click", () => { searchPage = currentNum + 1; loadChickensList(); });

    pagCont.querySelectorAll(".btn-page-number").forEach(btn => {
      btn.addEventListener("click", (e) => {
        searchPage = parseInt(e.target.closest(".btn-page-number").getAttribute("data-page"));
        loadChickensList();
      });
    });
  }

  if (btnToggleAdv) {
    btnToggleAdv.addEventListener("click", () => {
      isAdvancedOpen = !isAdvancedOpen;
      if (advFiltersPanel) {
        advFiltersPanel.style.maxHeight = isAdvancedOpen ? "400px" : "0px";
        advFiltersPanel.style.opacity = isAdvancedOpen ? "1" : "0";
      }
      btnToggleAdv.classList.toggle("active", isAdvancedOpen);
    });
  }

  const btnApplyAdv = document.getElementById("btn-apply-adv-filters");
  if (btnApplyAdv) {
    btnApplyAdv.addEventListener("click", () => {
      let count = 0;
      [advGender, advBreed, advHealth, advStatus, advCategory, advOrigin, advAgeGroup].forEach(i => { if (i && i.value) count++; });
      if (activeFiltersQty) {
        activeFiltersQty.style.display = count > 0 ? "inline-block" : "none";
        activeFiltersQty.textContent = count;
      }
      searchPage = 0;
      loadChickensList();
      isAdvancedOpen = false;
      if (advFiltersPanel) {
        advFiltersPanel.style.maxHeight = "0px";
        advFiltersPanel.style.opacity = "0";
      }
      if (btnToggleAdv) btnToggleAdv.classList.remove("active");
    });
  }

  document.getElementById("btn-flock-clear-filters").addEventListener("click", () => {
    [advGender, advBreed, advHealth, advStatus, advCategory, advOrigin, advAgeGroup].forEach(i => {
      i.value = "";
      if (i.refreshCustomSelect) i.refreshCustomSelect();
    });
    activeFiltersQty.style.display = "none";
    searchPage = 0;
    loadChickensList();
  });

  quickTagsGroup.addEventListener("click", (e) => {
    const pill = e.target.closest(".filter-pill");
    if (!pill) return;
    quickTagsGroup.querySelectorAll(".filter-pill").forEach(p => p.classList.remove("active"));
    pill.classList.add("active");
    activeQuickFilter = pill.getAttribute("data-filter");
    searchPage = 0;
    loadChickensList();
  });

  wildSearchInput.addEventListener("input", (e) => {
    searchQuery = e.target.value.toLowerCase().trim();
    searchPage = 0;
    loadChickensList();
  });

  document.getElementById("layout-grid-btn").addEventListener("click", () => {
    document.getElementById("layout-table-btn").classList.remove("active");
    document.getElementById("layout-grid-btn").classList.add("active");
    visualViewport.className = "view-mode-grid";
    renderListLayoutsFromData();
  });

  document.getElementById("layout-table-btn").addEventListener("click", () => {
    document.getElementById("layout-grid-btn").classList.remove("active");
    document.getElementById("layout-table-btn").classList.add("active");
    visualViewport.className = "view-mode-table";
    renderListLayoutsFromData();
  });

  function updateLiveSummary() {
    const idVal = document.getElementById("fm-bird-id")?.value || "-";
    const nameVal = document.getElementById("fm-bird-name")?.value || "-";
    const catVal = document.getElementById("fm-category")?.value || "-";
    const breedVal = document.getElementById("fm-breed")?.value || "-";
    const genderVal = document.getElementById("fm-gender")?.value || "-";
    const originVal = document.getElementById("fm-source")?.value || "-";
    const healthVal = document.getElementById("fm-health")?.value || "-";
    const dobVal = document.getElementById("fm-dob")?.value || "";
    const acqVal = document.getElementById("fm-acq-date")?.value || "";
    
    let dateSummary = originVal === "Farm Born" ? (dobVal ? `DOB: ${dobVal}` : "DOB: Not Set") : `DOB: ${dobVal || 'N/A'}, Acq: ${acqVal || 'N/A'}`;
    const preview = document.getElementById("form-photo-preview");
    if (preview && !preview.querySelector("img")) {
      let emoji = genderVal === "Rooster" ? "🐓" : (genderVal === "Chick" ? "🐥" : "🐔");
      preview.textContent = emoji;
      const sp = document.getElementById("live-summary-photo");
      if (sp) sp.textContent = emoji;
    }
    if (document.getElementById("sum-id")) document.getElementById("sum-id").textContent = idVal;
    if (document.getElementById("sum-category")) document.getElementById("sum-category").textContent = catVal;
    if (document.getElementById("sum-breed")) document.getElementById("sum-breed").textContent = breedVal;
    if (document.getElementById("sum-gender")) document.getElementById("sum-gender").textContent = genderVal;
    if (document.getElementById("sum-origin")) document.getElementById("sum-origin").textContent = originVal;
    if (document.getElementById("sum-health")) document.getElementById("sum-health").textContent = healthVal;
    if (document.getElementById("sum-dates")) document.getElementById("sum-dates").textContent = dateSummary;
  }

  window.selectOrigin = function(sourceVal) {
    const farmCard = document.getElementById("origin-card-farm");
    const purchasedCard = document.getElementById("origin-card-purchased");
    const sourceInput = document.getElementById("fm-source");
    const dobContainer = document.getElementById("fm-dob-container");
    const acqDetails = document.getElementById("fm-acq-details-fields");

    if (sourceVal === "Farm Born") {
      if (farmCard) farmCard.classList.add("selected");
      if (purchasedCard) purchasedCard.classList.remove("selected");
      if (dobContainer) dobContainer.style.display = "block";
      if (acqDetails) acqDetails.style.display = "none";
      const dobInput = document.getElementById("fm-dob");
      if (dobInput) dobInput.required = true;
    } else {
      if (purchasedCard) purchasedCard.classList.add("selected");
      if (farmCard) farmCard.classList.remove("selected");
      if (dobContainer) dobContainer.style.display = "block";
      if (acqDetails) acqDetails.style.display = "flex";
      const dobInput = document.getElementById("fm-dob");
      if (dobInput) dobInput.required = false;
    }
    if (sourceInput) {
      sourceInput.value = sourceVal;
      sourceInput.dispatchEvent(new Event("change"));
    }
    updateLiveSummary();
  }

  setTimeout(() => {
    ["fm-bird-id", "fm-bird-name", "fm-category", "fm-breed", "fm-gender", "fm-dob", "fm-acq-date", "fm-health"].forEach(id => {
      const el = document.getElementById(id);
      if (el) {
        el.addEventListener("input", updateLiveSummary);
        el.addEventListener("change", updateLiveSummary);
      }
    });
  }, 100);

  function openFormWorkspace(bird = null) {
    wasViewingDetailsBeforeEdit = (detailWorkspace && detailWorkspace.style.display !== "none");
    const titleEl = document.getElementById("form-workspace-title");
    const subTitleEl = document.getElementById("form-workspace-subtitle");
    
    document.querySelectorAll(".floating-label-group").forEach(grp => grp.classList.remove("has-error", "has-success"));
    document.querySelectorAll(".validation-message").forEach(msg => msg.style.display = "none");

    if (bird) {
      editTargetId = bird.dbId;
      titleEl.innerHTML = `<i class="fa-solid fa-pen-to-square" style="color: var(--primary-green);"></i> Edit Chicken: ${bird.id}`;
      subTitleEl.textContent = "Modify chicken credentials, origin specs, date records and status.";
      
      document.getElementById("fm-bird-id").value = bird.id;
      document.getElementById("fm-bird-id").readOnly = true;
      document.getElementById("fm-bird-name").value = bird.name;
      document.getElementById("fm-bird-name").readOnly = true;
      document.getElementById("fm-leg-band").value = bird.band === "None" ? "" : bird.band;
      document.getElementById("fm-leg-band").readOnly = true;

      document.getElementById("fm-gender").value = bird.gender;
      if (document.getElementById("fm-gender").refreshCustomSelect) document.getElementById("fm-gender").refreshCustomSelect();

      document.getElementById("fm-category").value = bird.category;
      if (document.getElementById("fm-category").refreshCustomSelect) document.getElementById("fm-category").refreshCustomSelect();

      const breedSel = document.getElementById("fm-breed");
      const breedCont = document.getElementById("fm-breed-container");
      breedSel.innerHTML = '<option value="" disabled selected hidden></option>';
      if (bird.category && window.BREED_CATEGORIES[bird.category]) {
        window.BREED_CATEGORIES[bird.category].forEach(b => {
          const opt = document.createElement("option");
          opt.value = b.value; opt.textContent = b.text;
          breedSel.appendChild(opt);
        });
        breedSel.value = bird.breed;
        window.makePremiumSelect("fm-breed");
        breedCont.classList.add("visible");
      }

      document.getElementById("fm-dob").value = bird.dob || "";
      document.getElementById("fm-dob").readOnly = true;
      
      window.selectOrigin(bird.source);
      document.getElementById("origin-card-farm").style.pointerEvents = "none";
      document.getElementById("origin-card-purchased").style.pointerEvents = "none";
      document.getElementById("origin-card-farm").style.opacity = "0.7";
      document.getElementById("origin-card-purchased").style.opacity = "0.7";

      document.getElementById("fm-weight").value = bird.weight;
      document.getElementById("fm-coop-id").value = bird.coop || "Coop A - Laying Cage";
      if (document.getElementById("fm-coop-id").refreshCustomSelect) document.getElementById("fm-coop-id").refreshCustomSelect();

      document.getElementById("fm-acq-date").value = bird.acqDate || "2026-07-16";
      document.getElementById("fm-acq-date").readOnly = true;
      document.getElementById("fm-acq-price").value = bird.acqPrice || 0;
      document.getElementById("fm-acq-price").readOnly = true;

      document.getElementById("fm-health").value = bird.health;
      if (document.getElementById("fm-health").refreshCustomSelect) document.getElementById("fm-health").refreshCustomSelect();

      document.getElementById("fm-purpose").value = bird.status;
      if (document.getElementById("fm-purpose").refreshCustomSelect) document.getElementById("fm-purpose").refreshCustomSelect();

      document.getElementById("fm-notes").value = bird.notes || "";
      
      const ageBox = document.getElementById("fm-calculated-age-display");
      if (ageBox) ageBox.textContent = bird.ageText || "N/A";
    } else {
      editTargetId = null;
      titleEl.innerHTML = `<i class="fa-solid fa-square-plus" style="color: var(--primary-green);"></i> Register Chicken`;
      subTitleEl.textContent = "Introduce a new chicken to the farm registry system.";
      
      document.getElementById("form-fullpage-bird-editor").reset();
      document.getElementById("fm-bird-id").readOnly = false;
      document.getElementById("fm-bird-name").readOnly = false;
      document.getElementById("fm-leg-band").readOnly = false;
      document.getElementById("fm-dob").readOnly = false;
      document.getElementById("fm-acq-date").readOnly = false;
      document.getElementById("fm-acq-price").readOnly = false;
      
      document.getElementById("origin-card-farm").style.pointerEvents = "auto";
      document.getElementById("origin-card-purchased").style.pointerEvents = "auto";
      document.getElementById("origin-card-farm").style.opacity = "1";
      document.getElementById("origin-card-purchased").style.opacity = "1";
      
      document.getElementById("fm-bird-id").value = `C0${Math.floor(Math.random() * 900) + 100}`;
      document.getElementById("fm-acq-date").value = new Date().toISOString().split('T')[0];
      document.getElementById("fm-dob").value = "";
      
      window.selectOrigin("Farm Born");
      ["fm-gender", "fm-category", "fm-coop-id", "fm-health", "fm-purpose"].forEach(selId => {
        const selectEl = document.getElementById(selId);
        if (selectEl && selectEl.refreshCustomSelect) selectEl.refreshCustomSelect();
      });
      document.getElementById("fm-breed").innerHTML = '<option value="" disabled selected hidden></option>';
      if (document.getElementById("fm-breed").refreshCustomSelect) document.getElementById("fm-breed").refreshCustomSelect();
      document.getElementById("fm-breed-container").classList.remove("visible");
    }
    updateLiveSummary();
    switchView(formWorkspace);
  }

  function showSuccessToast(message) {
    let toast = document.getElementById("success-toast-container");
    if (!toast) {
      toast = document.createElement("div");
      toast.id = "success-toast-container";
      toast.style.cssText = "position:fixed; bottom:24px; right:24px; background:#1E293B; color:#FFFFFF; padding:12px 24px; border-radius:8px; box-shadow:0 10px 15px -3px rgba(0,0,0,0.1); z-index:99999; transform:translateY(100px); opacity:0; transition:all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display:flex; align-items:center; gap:8px; font-weight:500; font-size:0.9rem; border:1px solid rgba(255,255,255,0.1);";
      document.body.appendChild(toast);
    }
    toast.innerHTML = `<i class="fa-solid fa-circle-check" style="color:#10B981; margin-right:4px;"></i> ${message}`;
    setTimeout(() => { toast.style.transform = "translateY(0)"; toast.style.opacity = "1"; }, 50);
    setTimeout(() => { toast.style.transform = "translateY(100px)"; toast.style.opacity = "0"; }, 3000);
  }

  document.getElementById("form-fullpage-bird-editor").addEventListener("submit", (e) => {
    e.preventDefault();
    const bid = document.getElementById("fm-bird-id").value.trim() || `C0${Math.floor(Math.random() * 900) + 100}`;
    const bname = document.getElementById("fm-bird-name").value.trim();
    let bband = document.getElementById("fm-leg-band").value.trim();
    if (!bband) bband = "TAG-" + bid;

    let bgender = document.getElementById("fm-gender").value;
    if (!bgender || bgender === "Unknown") bgender = "Hen";

    let bcategory = document.getElementById("fm-category").value;
    if (!bcategory) bcategory = "Country Chicken";

    let bbreed = document.getElementById("fm-breed").value;
    if (!bbreed) bbreed = "Other";

    const bdob = document.getElementById("fm-dob").value;
    const rawWeight = parseFloat(document.getElementById("fm-weight").value);
    const bweight = (!isNaN(rawWeight) && rawWeight > 0) ? rawWeight : 1.5;

    const bsource = document.getElementById("fm-source").value || "Farm Born";
    const bstatus = document.getElementById("fm-purpose").value || "Laying";
    const bnotes = document.getElementById("fm-notes").value.trim();

    let isFormValid = true;
    document.querySelectorAll(".floating-label-group").forEach(grp => grp.classList.remove("has-error"));
    document.querySelectorAll(".validation-message").forEach(msg => msg.style.display = "none");

    if (!bname) {
      document.getElementById("fm-bird-name").closest(".floating-label-group").classList.add("has-error");
      const errName = document.getElementById("err-bird-name");
      if (errName) errName.style.display = "block";
      isFormValid = false;
    }

    if (bsource === "Farm Born" && !bdob) {
      document.getElementById("fm-dob").closest(".floating-label-group").classList.add("has-error");
      const errDob = document.getElementById("err-bird-dob");
      if (errDob) errDob.style.display = "block";
      isFormValid = false;
    }

    if (!isFormValid) {
      const fe = document.querySelector(".floating-label-group.has-error");
      if (fe) {
        fe.scrollIntoView({ behavior: "smooth", block: "center" });
        const inp = fe.querySelector("input, select");
        if (inp) inp.focus();
      }
      return;
    }

    const payload = {
      chickenCode: bid,
      name: bname,
      breed: BREEDS[bbreed] || "OTHER",
      category: CATS[bcategory] || "OTHER",
      gender: bgender === "Rooster" ? "MALE" : "FEMALE",
      dateOfBirth: bdob || new Date().toISOString().split('T')[0],
      weight: bweight,
      color: bband,
      status: STATUS[bstatus] || "ACTIVE",
      remarks: `[Origin: ${bsource}] ${bnotes}`
    };

    let saveUrl = "chickens", method = "post";
    if (editTargetId) {
      saveUrl = `chickens/${editTargetId}`;
      method = "put";
    }

    Api[method](saveUrl, payload)
      .then((res) => {
        showSuccessToast(editTargetId ? "Chicken updated successfully." : "Chicken registered successfully.");
        switchView(listWorkspace);
        loadChickensList();
      })
      .catch(err => {
        console.error("Save failed:", err);
        const errMsg = err.message || "Failed to register chicken. Please check network connection.";
        let errToast = document.getElementById("error-toast-container");
        if (!errToast) {
          errToast = document.createElement("div");
          errToast.id = "error-toast-container";
          errToast.style.cssText = "position:fixed; bottom:24px; right:24px; background:#EF4444; color:#FFFFFF; padding:12px 24px; border-radius:8px; box-shadow:0 10px 15px -3px rgba(0,0,0,0.1); z-index:99999; transform:translateY(100px); opacity:0; transition:all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display:flex; align-items:center; gap:8px; font-weight:500; font-size:0.9rem;";
          document.body.appendChild(errToast);
        }
        errToast.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> ${errMsg}`;
        setTimeout(() => { errToast.style.transform = "translateY(0)"; errToast.style.opacity = "1"; }, 50);
        setTimeout(() => { errToast.style.transform = "translateY(100px)"; errToast.style.opacity = "0"; }, 4000);
      });
  });

  let currentProfileData = null;
  let activeProfileTab = "overview";

  async function openDetailWorkspace(bird) {
    showListLoadingState();
    try {
      const res = await Api.get(`chickens/${bird.dbId}`);
      if (res && res.success && res.data) {
        currentProfileData = res.data;
        renderDetailedProfile(res.data);
      }
    } catch (e) {
      console.error(e);
    }
  }

  // Profile Tab Switcher
  document.querySelectorAll(".profile-tab-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {
      document.querySelectorAll(".profile-tab-btn").forEach(b => {
        b.classList.remove("active");
        b.style.color = "#64748B";
        b.style.borderBottom = "none";
      });
      const t = e.target.closest(".profile-tab-btn");
      t.classList.add("active");
      t.style.color = "#16A34A";
      t.style.borderBottom = "3px solid #16A34A";
      activeProfileTab = t.getAttribute("data-tab");
      if (currentProfileData) renderProfileTabContent(currentProfileData);
    });
  });

  function renderProfileTabContent(data) {
    const layoutOutput = document.getElementById("profile-detailed-container");
    if (!layoutOutput) return;

    let origin = data.origin ? (data.origin === "FARM_BORN" ? "Farm Born" : "Purchased") : "Farm Born";
    let ageText = "N/A";
    if (data.ageInDays !== null && data.ageInDays !== undefined) {
      if (data.ageInDays < 60) ageText = `${data.ageInDays} Days`;
      else if (data.ageInMonths !== null) ageText = `${data.ageInMonths} Months`;
    }
    let emoji = data.gender === "MALE" ? "🐓" : (data.category === "CHICK" ? "🐥" : "🐔");
    let breedUI = BREEDS_REV[data.breed] || data.breed || "Other";
    let categoryUI = CATS_REV[data.category] || data.category || "Other";
    let genderUI = data.gender === "MALE" ? "Rooster" : (data.gender === "FEMALE" ? "Hen" : "Unknown");
    let statusUI = data.status || "ACTIVE";
    let healthUI = data.healthStatus ? data.healthStatus.replace(/_/g, ' ') : "Healthy";
    let healthClass = healthUI.toLowerCase().includes("healthy") ? "healthy" : "treatment";

    // Hero Section Header Card
    const heroHeader = `
      <div style="background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; padding: 24px; display: flex; flex-wrap: wrap; gap: 24px; align-items: center; justify-content: space-between; margin-bottom: 24px;">
        <div style="display: flex; gap: 20px; align-items: center; flex-wrap: wrap;">
          <div id="profile-photo-wrapper" style="position: relative; width: 90px; height: 90px; border-radius: 50%; overflow: hidden; border: 2px solid #E2E8F0; background: #F8FAFC; display: flex; align-items: center; justify-content: center; font-size: 3rem;">
            ${data.photoUrl ? `<img src="${data.photoUrl}" id="img-profile-photo" style="width:100%; height:100%; object-fit:cover;">` : `<span id="span-profile-emoji">${emoji}</span>`}
          </div>
          <div>
            <div style="display: flex; align-items: center; gap: 10px;">
              <h2 style="font-size: 1.4rem; font-weight: 700; margin: 0; color: #1E293B;">${data.name || data.chickenCode}</h2>
              <span style="background: #E2E8F0; color: #334155; padding: 2px 10px; border-radius: 12px; font-weight: 700; font-size: 0.85rem;">${data.chickenCode}</span>
            </div>
            <div style="display: flex; gap: 12px; margin-top: 8px; font-size: 0.85rem; color: #64748B; flex-wrap: wrap;">
              <span>Category: <strong style="color:#1E293B;">${categoryUI}</strong></span>
              <span>•</span>
              <span>Breed: <strong style="color:#1E293B;">${breedUI}</strong></span>
              <span>•</span>
              <span>Age: <strong style="color:#1E293B;">${ageText}</strong></span>
            </div>
            <div style="display: flex; gap: 8px; margin-top: 10px;">
              <span class="chk-status-pill ${healthClass}">${healthUI}</span>
              <span class="chk-status-pill ${statusUI.toLowerCase()}">${statusUI}</span>
            </div>
          </div>
        </div>

        <!-- Photo controls -->
        <div style="display: flex; flex-direction: column; gap: 8px; align-items: flex-end;">
          <input type="file" id="input-profile-photo-upload" accept="image/jpeg,image/jpg,image/png,image/webp" style="display: none;">
          <button type="button" class="btn btn-outline" id="btn-upload-photo" style="font-size: 0.78rem; padding: 5px 12px;"><i class="fa-solid fa-camera"></i> Upload / Replace Photo</button>
          ${data.photoUrl ? `<button type="button" class="btn btn-text" id="btn-remove-photo" style="font-size: 0.75rem; color: #DC2626;"><i class="fa-solid fa-trash"></i> Remove Photo</button>` : ''}
        </div>
      </div>
    `;

    let tabBody = "";

    if (activeProfileTab === "overview") {
      tabBody = `
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px;">
          <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:20px;">
            <h3 style="font-size:1rem; font-weight:700; color:#1E293B; margin-bottom:14px; border-bottom:1px solid #F1F5F9; padding-bottom:8px;"><i class="fa-solid fa-feather-pointed" style="color:#16A34A; margin-right:6px;"></i> Physical Characteristics</h3>
            <table class="drawer-data-table">
              <tr><td><strong>Chicken ID</strong></td><td><strong>${data.chickenCode}</strong></td></tr>
              <tr><td><strong>Name</strong></td><td>${data.name || 'Unnamed'}</td></tr>
              <tr><td><strong>Breed</strong></td><td>${breedUI}</td></tr>
              <tr><td><strong>Category</strong></td><td>${categoryUI}</td></tr>
              <tr><td><strong>Gender</strong></td><td>${genderUI}</td></tr>
              <tr><td><strong>Colour</strong></td><td>${data.color || 'N/A'}</td></tr>
              <tr><td><strong>Current Weight</strong></td><td><strong>${data.weight || 0} kg</strong></td></tr>
              <tr><td><strong>Wing Tag Number</strong></td><td>${data.wingTagNumber || 'None'}</td></tr>
              <tr><td><strong>Leg Band Number</strong></td><td>${data.legBandNumber || 'None'}</td></tr>
            </table>
          </div>

          <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:20px;">
            <h3 style="font-size:1rem; font-weight:700; color:#1E293B; margin-bottom:14px; border-bottom:1px solid #F1F5F9; padding-bottom:8px;"><i class="fa-solid fa-dna" style="color:#16A34A; margin-right:6px;"></i> Origin & Pedigree</h3>
            <table class="drawer-data-table">
              <tr><td><strong>Origin</strong></td><td>${origin}</td></tr>
              <tr><td><strong>Date of Birth</strong></td><td>${data.dateOfBirth || 'Unknown'}</td></tr>
              <tr><td><strong>Age</strong></td><td><strong>${ageText}</strong></td></tr>
              <tr><td><strong>Father Code</strong></td><td>${data.fatherCode || 'Unspecified'}</td></tr>
              <tr><td><strong>Mother Code</strong></td><td>${data.motherCode || 'Unspecified'}</td></tr>
              <tr><td><strong>Vaccination Count</strong></td><td><strong style="color:#16A34A;">${data.vaccinationCount || 0} Dose(s)</strong></td></tr>
              <tr><td><strong>Registration Date</strong></td><td>${data.createdAt ? new Date(data.createdAt).toLocaleDateString() : 'N/A'}</td></tr>
              <tr><td><strong>Last Updated</strong></td><td>${data.updatedAt ? new Date(data.updatedAt).toLocaleString() : 'N/A'}</td></tr>
            </table>
          </div>
        </div>
      `;
    } else if (activeProfileTab === "health") {
      tabBody = `
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:24px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; border-bottom:1px solid #F1F5F9; padding-bottom:10px;">
            <h3 style="font-size:1rem; font-weight:700; margin:0;"><i class="fa-solid fa-heart-pulse" style="color:#EF4444; margin-right:6px;"></i> Diagnostic Health Status</h3>
            <a href="health-records.html?code=${data.chickenCode}" class="btn btn-outline" style="font-size:0.8rem; padding:4px 12px;"><i class="fa-solid fa-up-right-from-square"></i> Open Full Health Log</a>
          </div>
          <table class="drawer-data-table" style="margin-bottom:20px;">
            <tr><td><strong>Diagnostic Health Status</strong></td><td><span class="chk-status-pill ${healthClass}">${healthUI}</span></td></tr>
            <tr><td><strong>Overall Status</strong></td><td><span class="chk-status-pill ${statusUI.toLowerCase()}">${statusUI}</span></td></tr>
            <tr><td><strong>Vaccination Status</strong></td><td>${data.vaccinated ? '<span style="color:#16A34A; font-weight:700;">Fully Vaccinated</span>' : '<span style="color:#D97706;">Pending Vaccinations</span>'}</td></tr>
            <tr><td><strong>Remarks / Notes</strong></td><td>${data.remarks || 'No remarks logged.'}</td></tr>
          </table>
        </div>
      `;
    } else if (activeProfileTab === "vaccinations") {
      const vacs = data.vaccinations || [];
      tabBody = `
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:24px;">
          <h3 style="font-size:1rem; font-weight:700; margin-bottom:16px;"><i class="fa-solid fa-syringe" style="color:#16A34A; margin-right:6px;"></i> Vaccination Log (${vacs.length} Doses)</h3>
          ${vacs.length === 0 ? `<p style="color:#64748B; font-size:0.9rem;">No vaccination records currently recorded for this chicken.</p>` : `
            <table class="flock-data-table" style="width:100%;">
              <thead>
                <tr>
                  <th>Vaccine Name</th>
                  <th>Administered Date</th>
                  <th>Next Due Date</th>
                  <th>Notes</th>
                </tr>
              </thead>
              <tbody>
                ${vacs.map(v => `
                  <tr>
                    <td><strong>${v.vaccineName}</strong></td>
                    <td>${v.vaccinationDate || 'N/A'}</td>
                    <td>${v.nextDueDate ? `<span style="color:#D97706; font-weight:600;">${v.nextDueDate}</span>` : 'None'}</td>
                    <td>${v.notes || '-'}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          `}
        </div>
      `;
    } else if (activeProfileTab === "timeline") {
      const events = data.timeline || [];
      tabBody = `
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:24px;">
          <h3 style="font-size:1rem; font-weight:700; margin-bottom:20px;"><i class="fa-solid fa-clock-rotate-left" style="color:#6366F1; margin-right:6px;"></i> Chronological Activity Stream</h3>
          ${events.length === 0 ? `<p style="color:#64748B; font-size:0.9rem;">No recorded timeline events.</p>` : `
            <div style="display:flex; flex-direction:column; gap:16px; position:relative; padding-left:20px; border-left:2px solid #E2E8F0;">
              ${events.map(ev => `
                <div style="position:relative;">
                  <div style="position:absolute; left:-27px; top:2px; width:12px; height:12px; border-radius:50%; background:#16A34A; border:2px solid #FFFFFF;"></div>
                  <div style="font-size:0.78rem; color:#64748B;">${new Date(ev.timestamp).toLocaleString()}</div>
                  <div style="font-weight:700; font-size:0.92rem; color:#1E293B;">${ev.title}</div>
                  <div style="font-size:0.85rem; color:#475569; margin-top:2px;">${ev.description || ''}</div>
                </div>
              `).join('')}
            </div>
          `}
        </div>
      `;
    } else if (activeProfileTab === "documents") {
      tabBody = `
        <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:24px; text-align:center;">
          <h3 style="font-size:1rem; font-weight:700; margin-bottom:12px;"><i class="fa-solid fa-file-pdf" style="color:#DC2626; margin-right:6px;"></i> Exportable Chicken Biography Card</h3>
          <p style="color:#64748B; font-size:0.85rem; margin-bottom:20px;">Download or print official pedigree and identification certificate sheets.</p>
          <div style="display:flex; gap:12px; justify-content:center;">
            <button class="btn btn-outline" onclick="if(window.triggerPrintCard) window.triggerPrintCard(${data.id});"><i class="fa-solid fa-print"></i> Print ID Card</button>
            <button class="btn btn-primary" onclick="if(window.exportChickensToCSV) window.exportChickensToCSV([currentProfileData]);"><i class="fa-solid fa-file-csv"></i> Download CSV Biography</button>
          </div>
        </div>
      `;
    }

    layoutOutput.innerHTML = heroHeader + tabBody;

    // Attach Photo Upload / Remove listeners
    const btnUpload = document.getElementById("btn-upload-photo");
    const inputUpload = document.getElementById("input-profile-photo-upload");
    const btnRemove = document.getElementById("btn-remove-photo");

    if (btnUpload && inputUpload) {
      btnUpload.addEventListener("click", () => inputUpload.click());
      inputUpload.addEventListener("change", (e) => {
        const file = e.target.files[0];
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) {
          alert("File size exceeds maximum allowed 5 MB limit.");
          return;
        }
        const reader = new FileReader();
        reader.onload = async (evt) => {
          const base64Photo = evt.target.result;
          try {
            // Update photo URL via PUT API
            await Api.put(`chickens/${data.id}`, {
              chickenCode: data.chickenCode,
              name: data.name,
              breed: data.breed,
              category: data.category,
              gender: data.gender,
              dateOfBirth: data.dateOfBirth,
              weight: data.weight,
              status: data.status,
              photoUrl: base64Photo
            });
            showSuccessToast("Chicken photo updated successfully.");
            openDetailWorkspace({ dbId: data.id });
          } catch (err) {
            console.error("Photo upload failed:", err);
          }
        };
        reader.readAsDataURL(file);
      });
    }

    if (btnRemove) {
      btnRemove.addEventListener("click", async () => {
        if (confirm("Remove chicken profile photo?")) {
          try {
            await Api.put(`chickens/${data.id}`, {
              chickenCode: data.chickenCode,
              name: data.name,
              breed: data.breed,
              category: data.category,
              gender: data.gender,
              dateOfBirth: data.dateOfBirth,
              weight: data.weight,
              status: data.status,
              photoUrl: ""
            });
            showSuccessToast("Chicken photo removed.");
            openDetailWorkspace({ dbId: data.id });
          } catch (err) {
            console.error(err);
          }
        }
      });
    }
  }

  function renderDetailedProfile(data) {
    currentProfileData = data;
    const titleHeader = document.getElementById("profile-title-header");
    if (titleHeader) titleHeader.textContent = `Chicken Profile: ${data.chickenCode}`;

    renderProfileTabContent(data);

    // Header buttons listeners
    const btnEditLnk = document.getElementById("btn-detail-edit-lnk");
    if (btnEditLnk) {
      btnEditLnk.onclick = () => {
        const bird = {
          id: data.chickenCode, dbId: data.id, name: data.name,
          gender: data.gender === "MALE" ? "Rooster" : "Hen",
          category: CATS_REV[data.category] || "Other", breed: BREEDS_REV[data.breed] || "Other",
          dob: data.dateOfBirth, weight: data.weight || 0.0, health: "Healthy",
          status: data.status, source: data.origin === "FARM_BORN" ? "Farm Born" : "Purchased",
          band: data.legBandNumber || "", notes: data.remarks || "", ageText: `${data.ageInDays || 0} Days`
        };
        openFormWorkspace(bird);
      };
    }

    // QR Download PNG & Print QR
    const btnDownloadQR = document.getElementById("btn-profile-download-qr");
    if (btnDownloadQR) {
      btnDownloadQR.onclick = () => {
        const canvas = document.createElement("canvas");
        canvas.id = "temp-qr-canvas";
        canvas.style.display = "none";
        document.body.appendChild(canvas);
        const qrStr = `Chicken ID: ${data.chickenCode} | Breed: ${data.breed} | Gender: ${data.gender} | DOB: ${data.dateOfBirth}`;
        if (window.drawQRCodeOnCanvas) window.drawQRCodeOnCanvas("temp-qr-canvas", qrStr);
        setTimeout(() => {
          const link = document.createElement("a");
          link.download = `${data.chickenCode}_qrcode.png`;
          link.href = canvas.toDataURL("image/png");
          link.click();
          document.body.removeChild(canvas);
        }, 100);
      };
    }

    const btnPrintQR = document.getElementById("btn-profile-print-qr");
    if (btnPrintQR) {
      btnPrintQR.onclick = () => {
        if (window.triggerPrintCard) window.triggerPrintCard(data.id);
      };
    }

    // Status Change Modal (PATCH)
    const btnChangeStatus = document.getElementById("btn-profile-change-status");
    const modalChangeStatus = document.getElementById("modal-change-status");
    if (btnChangeStatus && modalChangeStatus) {
      btnChangeStatus.onclick = () => {
        modalChangeStatus.style.display = "flex";
      };
    }

    const closeStatusModal = () => { if (modalChangeStatus) modalChangeStatus.style.display = "none"; };
    const btnCloseSM = document.getElementById("btn-close-status-modal");
    const btnCancelSM = document.getElementById("btn-cancel-status-modal");
    const btnSubmitSM = document.getElementById("btn-submit-status-modal");

    if (btnCloseSM) btnCloseSM.onclick = closeStatusModal;
    if (btnCancelSM) btnCancelSM.onclick = closeStatusModal;
    if (btnSubmitSM) {
      btnSubmitSM.onclick = async () => {
        const healthVal = document.getElementById("modal-patch-health").value;
        const statusVal = document.getElementById("modal-patch-status").value;
        const remarksVal = document.getElementById("modal-patch-remarks").value;

        try {
          await Api.patch(`chickens/${data.id}/status`, {
            status: statusVal,
            healthStatus: healthVal,
            remarks: remarksVal
          });
          showSuccessToast(`Chicken ${data.chickenCode} status updated successfully.`);
          closeStatusModal();
          openDetailWorkspace({ dbId: data.id });
        } catch (err) {
          console.error(err);
        }
      };
    }

    // Soft Delete Confirmation Modal
    const btnDeleteLnk = document.getElementById("btn-detail-delete-lnk");
    const modalDelete = document.getElementById("modal-confirm-delete-chicken");
    const inputConfirmDelete = document.getElementById("input-confirm-delete-str");
    const btnSubmitDelete = document.getElementById("btn-submit-delete-modal");
    const deleteModalId = document.getElementById("delete-modal-chicken-id");

    if (btnDeleteLnk && modalDelete) {
      btnDeleteLnk.onclick = () => {
        if (deleteModalId) deleteModalId.textContent = data.chickenCode;
        if (inputConfirmDelete) {
          inputConfirmDelete.value = "";
          if (btnSubmitDelete) {
            btnSubmitDelete.disabled = true;
            btnSubmitDelete.style.opacity = "0.5";
            btnSubmitDelete.style.cursor = "not-allowed";
          }
        }
        modalDelete.style.display = "flex";
      };
    }

    if (inputConfirmDelete && btnSubmitDelete) {
      inputConfirmDelete.oninput = (e) => {
        const isValid = e.target.value.trim() === "DELETE";
        btnSubmitDelete.disabled = !isValid;
        btnSubmitDelete.style.opacity = isValid ? "1" : "0.5";
        btnSubmitDelete.style.cursor = isValid ? "pointer" : "not-allowed";
      };
    }

    const closeDeleteModal = () => { if (modalDelete) modalDelete.style.display = "none"; };
    const btnCloseDM = document.getElementById("btn-close-delete-modal");
    const btnCancelDM = document.getElementById("btn-cancel-delete-modal");
    if (btnCloseDM) btnCloseDM.onclick = closeDeleteModal;
    if (btnCancelDM) btnCancelDM.onclick = closeDeleteModal;
    if (btnSubmitDelete) {
      btnSubmitDelete.onclick = async () => {
        try {
          await Api.delete(`chickens/${data.id}`);
          showSuccessToast(`Chicken ${data.chickenCode} soft deleted.`);
          closeDeleteModal();
          switchView(listWorkspace);
          loadChickensList();
        } catch (err) {
          console.error(err);
        }
      };
    }

    switchView(detailWorkspace);
  }

  window.triggerReportExport = () => {
    if (!birdsData || birdsData.length === 0) {
      showSuccessToast("No flock records available to export.");
      return;
    }
    const headers = ["ID", "Name", "Category", "Breed", "Gender", "Age", "Health", "Status", "Source"];
    const rows = birdsData.map(b => [
      `"${b.id}"`,
      `"${b.name}"`,
      `"${b.category}"`,
      `"${b.breed}"`,
      `"${b.gender}"`,
      `"${b.ageText || ''}"`,
      `"${b.health}"`,
      `"${b.status}"`,
      `"${b.source}"`
    ]);
    const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map(e => e.join(","))].join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `flock_records_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showSuccessToast(`Exported ${birdsData.length} chicken records to CSV.`);
  };

  loadChickensList();
});
