import { Api } from './api.js';

const BREEDS = {
  "Cobb 500": "COBB_500", "Ross 308": "ROSS_308", "Hubbard": "HUBBARD", "Arbor Acres": "ARBOR_ACRES",
  "Peruvidai": "PERUVIDAI", "Siruvidai": "SIRUVIDAI", "Cross": "CROSS", "Desi Country": "DESI_COUNTRY",
  "White Leghorn": "WHITE_LEGHORN", "Leghorn": "LEGHORN", "Rhode Island Red": "RHODE_ISLAND_RED", "Plymouth Rock": "PLYMOUTH_ROCK",
  "Brahma": "BRAMA", "Brama": "BRAMA", "Sussex": "SUSSEX", "Light Sussex": "SUSSEX", "Other": "OTHER"
};

const BREEDS_REV = {
  "COBB_500": "Cobb 500", "ROSS_308": "Ross 308", "HUBBARD": "Hubbard", "ARBOR_ACRES": "Arbor Acres",
  "PERUVIDAI": "Peruvidai", "SIRUVIDAI": "Siruvidai", "CROSS": "Cross", "DESI_COUNTRY": "Desi Country",
  "WHITE_LEGHORN": "White Leghorn", "LEGHORN": "White Leghorn", "RHODE_ISLAND_RED": "Rhode Island Red", "PLYMOUTH_ROCK": "Plymouth Rock",
  "BRAMA": "Brahma", "SUSSEX": "Sussex", "OTHER": "Other"
};

const BREEDS_BY_CATEGORY = {
  "Country Chicken": ["Peruvidai", "Siruvidai", "Cross", "Desi Country"],
  "COUNTRY_CHICKEN": ["Peruvidai", "Siruvidai", "Cross", "Desi Country"],
  "Broiler": ["Cobb 500", "Ross 308", "Hubbard", "Arbor Acres"],
  "BROILER": ["Cobb 500", "Ross 308", "Hubbard", "Arbor Acres"],
  "Layer": ["White Leghorn", "Rhode Island Red", "Plymouth Rock", "Sussex"],
  "LAYER": ["White Leghorn", "Rhode Island Red", "Plymouth Rock", "Sussex"]
};
const CATS = { "Broiler": "BROILER", "Layer": "LAYER", "Country Chicken": "COUNTRY_CHICKEN", "Breeder": "BREEDER", "Chick": "CHICK", "Rooster": "ROOSTER", "Other": "OTHER" };
const CATS_REV = { "BROILER": "Broiler", "LAYER": "Layer", "COUNTRY_CHICKEN": "Country Chicken", "BREEDER": "Breeder", "CHICK": "Chick", "ROOSTER": "Rooster", "OTHER": "Other" };
const STATUS = { "Active": "ACTIVE", "ACTIVE": "ACTIVE", "Laying": "ACTIVE", "Breeding": "ACTIVE", "Meat": "ACTIVE", "Molting": "ACTIVE", "Ready for Sale": "ACTIVE", "Sold": "SOLD", "Dead": "DEAD", "Removed from Farm": "INACTIVE" };
const STATUS_REV = { "ACTIVE": "Active", "BROODER": "Young Chick", "GROWING": "Young Chick", "SOLD": "Sold", "DEAD": "Dead", "INACTIVE": "Removed from Farm" };

document.addEventListener("DOMContentLoaded", () => {
  console.log("Entering flock page");
  let activeAbortController = null;
  let isSelectionMode = false;
  let selectedChickenIds = new Set();
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
    const sourceEl = document.getElementById("fm-origin") || document.getElementById("fm-source");
    if (!sourceEl) return;
    const source = sourceEl.value;
    const dobInput = document.getElementById("fm-dob");
    const dobLabel = document.getElementById("fm-dob-label");
    const acqFields = document.getElementById("wrapper-purchased-fields") || document.getElementById("fm-acq-details-fields");
    if (source === "Purchased") {
      if (acqFields) acqFields.style.display = "block";
      if (dobInput) dobInput.required = false;
      if (dobLabel) dobLabel.textContent = 'Date of Birth (if known)';
    } else {
      if (acqFields) acqFields.style.display = "none";
      if (dobInput) dobInput.required = true;
      if (dobLabel) dobLabel.innerHTML = 'Date of Birth <span class="text-rose">*</span>';
    }
  }
  const sourceElem = document.getElementById("fm-origin") || document.getElementById("fm-source");
  if (sourceElem) {
    sourceElem.addEventListener("change", toggleAcquisitionFields);
  }

  if (window.makePremiumSelect) {
    ["fm-gender", "fm-health", "fm-status", "fm-origin", "fm-father-id", "fm-mother-id", "fm-vaccinated"].forEach(id => window.makePremiumSelect(id));
    window.makePremiumSelect("fm-category", (val) => {
      const breedSel = document.getElementById("fm-breed");
      if (!breedSel) return;
      if (!val || val === "") {
        breedSel.disabled = true;
        breedSel.innerHTML = '<option value="" disabled selected hidden>Select Category First</option>';
        breedSel.value = "";
      } else if (window.BREED_CATEGORIES && window.BREED_CATEGORIES[val]) {
        breedSel.disabled = false;
        breedSel.innerHTML = '<option value="" disabled selected hidden>Select Breed</option>';
        window.BREED_CATEGORIES[val].forEach(b => {
          const opt = document.createElement("option");
          const valStr = typeof b === "object" ? b.value : b;
          const textStr = typeof b === "object" ? b.text : b;
          opt.value = valStr;
          opt.textContent = textStr;
          breedSel.appendChild(opt);
        });
        breedSel.value = "";
      }
      if (breedSel.refreshCustomSelect) {
        breedSel.refreshCustomSelect();
      } else {
        window.makePremiumSelect("fm-breed");
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
        if (target === listWorkspace) {
          loadChickensList();
        }
      }
    }
  }

  const btnTriggerAdd = document.getElementById("btn-trigger-add-view");
  if (btnTriggerAdd) btnTriggerAdd.addEventListener("click", () => openFormWorkspace(null));

  const btnEmptyTriggerAdd = document.getElementById("btn-empty-add-view-trigger");
  if (btnEmptyTriggerAdd) btnEmptyTriggerAdd.addEventListener("click", () => openFormWorkspace(null));
  
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

  const btnFormCancel = document.getElementById("btn-form-cancel");
  if (btnFormCancel) btnFormCancel.addEventListener("click", () => switchView(wasViewingDetailsBeforeEdit ? detailWorkspace : listWorkspace));

  const btnDetailBack = document.getElementById("btn-detail-back");
  if (btnDetailBack) btnDetailBack.addEventListener("click", () => switchView(listWorkspace));

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

  const btnLayingCancelTop = document.getElementById("btn-laying-cancel-top");
  if (btnLayingCancelTop) btnLayingCancelTop.addEventListener("click", window.closeStartLayingDrawer);

  const btnLayingCancel = document.getElementById("btn-laying-cancel");
  if (btnLayingCancel) btnLayingCancel.addEventListener("click", window.closeStartLayingDrawer);

  const formStartLaying = document.getElementById("form-start-laying");
  if (formStartLaying) {
    formStartLaying.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!currentStartLayingBird) return;
    const startDate = document.getElementById("laying-start-date").value;
    try {
      const res = await Api.get(`chickens/${currentStartLayingBird.dbId}`);
      const chk = (res && res.data) ? res.data : res;
      if (chk && (chk.id || chk.chickenCode)) {
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
  }

  function loadSkeleton() {
    showListLoadingState();
  }

  function hideSkeleton() {
    const skele = document.getElementById("flock-skeleton-loader");
    if (skele) skele.style.display = "none";
  }

  function showLoadingState() {
    showListLoadingState();
  }

  function hideLoadingState() {
    hideSkeleton();
  }

  function showListLoadingState() {
    if (birdsData.length > 0) return;
    const isGrid = visualViewport ? visualViewport.classList.contains("view-mode-grid") : true;
    if (isGrid) {
      gridDeck.style.display = "grid";
      listTableDeck.style.display = "none";
      emptyStateBlock.style.display = "none";
      gridDeck.innerHTML = `<div class="flock-skeleton-loader" id="flock-skeleton-loader" style="grid-column: 1 / -1;"><div class="skele-grid">${Array(8).fill(0).map(() => `<div class="skele-card"><div class="skele-img"></div><div class="skele-strip w80"></div><div class="skele-strip w60"></div></div>`).join('')}</div></div>`;
    } else {
      gridDeck.style.display = "none";
      listTableDeck.style.display = "block";
      emptyStateBlock.style.display = "none";
      tableBody.innerHTML = `<tr id="flock-skeleton-loader"><td colspan="10" style="text-align: center; padding: 40px 0;"><i class="fa-solid fa-spinner fa-spin-pulse" style="font-size: 1.5rem; color: var(--primary-green);"></i><p style="margin-top: 8px; color: var(--neutral-gray); font-size: 0.85rem;">Retrieving flock database records...</p></td></tr>`;
    }
  }

  const filterSortBy = document.getElementById("filter-sort-by");

  async function fetchDashboardStats() {
    try {
      const res = await Api.get("chickens/stats");
      const stats = (res && res.data) ? res.data : res;
      if (stats) {
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
      if (e && e.name === 'AbortError') return;
      console.warn("Failed to fetch dashboard stats:", e);
    }
  }

  async function loadChickensList(isBackground = false) {
    console.log("Loading chickens");

    if (activeAbortController) {
      activeAbortController.abort();
    }
    activeAbortController = new AbortController();
    const signal = activeAbortController.signal;

    if (birdsData.length > 0) {
      console.log("Rendering cards");
      console.log("Rendering table");
      renderListLayoutsFromData();
    } else if (!isBackground) {
      showListLoadingState();
    }

    fetchDashboardStats();

    console.log("API started");
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
      if (advBreed && advBreed.value) {
        const bCode = BREEDS[advBreed.value] || advBreed.value.toUpperCase().replace(/\s+/g, '_');
        queryParams.push(`breed=${encodeURIComponent(bCode)}`);
      }
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

      const response = await Api.get(`chickens?${queryParams.join('&')}`, { signal });
      console.log("API completed");
      hideSkeleton();

      const pageData = (response && response.data) ? response.data : (response && (response.content || Array.isArray(response)) ? response : null);
      if (pageData) {
        const rawList = pageData.content || (Array.isArray(pageData) ? pageData : []);
        birdsData = rawList.map(item => {
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

        console.log("Rendering cards");
        console.log("Rendering table");
        searchTotalPages = pageData.totalPages || 1;
        renderListLayoutsFromData();
        renderPaginationControls(pageData);
        console.log("Loading finished");
      }
    } catch (e) {
      if (e && e.name === 'AbortError') {
        console.log("API request aborted");
        return;
      }
      console.error(e);
      hideSkeleton();
      gridDeck.style.display = "none";
      listTableDeck.style.display = "none";
      emptyStateBlock.style.display = "block";
      emptyStateBlock.innerHTML = `
        <div class="flock-error-state" style="padding: 48px 24px; text-align: center; max-width: 500px; margin: 30px auto; background: #FFFFFF; border: 1px solid #FECDD3; border-radius: 16px; box-shadow: 0 10px 25px rgba(225,29,72,0.05);">
          <div style="width: 56px; height: 56px; border-radius: 50%; background: #FFE4E6; color: #E11D48; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin: 0 auto 16px;">
            <i class="fa-solid fa-circle-exclamation"></i>
          </div>
          <h3 style="font-size: 1.15rem; font-weight: 700; color: #9F1239; margin-bottom: 8px;">Unable to load chickens</h3>
          <p style="color: #64748B; font-size: 0.875rem; margin-bottom: 20px;">We encountered an issue connecting to the flock database. Please check your connection and try again.</p>
          <button id="btn-flock-retry" class="btn btn-primary" style="display: inline-flex; align-items: center; gap: 8px; cursor: pointer; padding: 10px 20px; border-radius: 8px; font-weight: 600;">
            <i class="fa-solid fa-rotate-right"></i> Retry
          </button>
        </div>
      `;
      const btnRetry = document.getElementById("btn-flock-retry");
      if (btnRetry) {
        btnRetry.onclick = () => loadChickensList();
      }
      console.log("Loading finished");
    }
  }

  function enterSelectionMode(initialDbId) {
    isSelectionMode = true;
    if (initialDbId !== undefined && initialDbId !== null) {
      selectedChickenIds.add(initialDbId);
    }
    updateBulkToolbarUI(initialDbId);
  }

  function exitSelectionMode() {
    isSelectionMode = false;
    selectedChickenIds.clear();
    updateBulkToolbarUI();
  }

  function toggleSelection(dbId) {
    if (!isSelectionMode) {
      enterSelectionMode(dbId);
      return;
    }
    if (selectedChickenIds.has(dbId)) {
      selectedChickenIds.delete(dbId);
    } else {
      selectedChickenIds.add(dbId);
    }
    if (selectedChickenIds.size === 0) {
      exitSelectionMode();
    } else {
      updateBulkToolbarUI(dbId);
    }
  }

  function updateBulkToolbarUI(targetDbId = null) {
    const viewport = document.getElementById("flock-visual-viewport");
    const workspace = document.getElementById("flock-list-workspace");
    const toolbar = document.getElementById("bulk-actions-toolbar");
    const countEl = document.getElementById("bulk-selected-count");

    if (isSelectionMode) {
      if (viewport) viewport.classList.add("selection-mode-active");
      if (workspace) workspace.classList.add("selection-mode-active");
      if (toolbar) toolbar.style.display = "flex";
      if (countEl) countEl.textContent = selectedChickenIds.size;
    } else {
      if (viewport) viewport.classList.remove("selection-mode-active");
      if (workspace) workspace.classList.remove("selection-mode-active");
      if (toolbar) toolbar.style.display = "none";
    }

    if (targetDbId !== null && targetDbId !== undefined) {
      const isSelected = selectedChickenIds.has(targetDbId);
      const card = document.querySelector(`.flock-card[data-dbid="${targetDbId}"]`);
      if (card) {
        card.classList.toggle("card-selected", isSelected);
        const chk = card.querySelector(".circular-checkbox-input");
        if (chk) chk.checked = isSelected;
      }
      const row = document.querySelector(`.flock-table-row[data-dbid="${targetDbId}"]`);
      if (row) {
        row.classList.toggle("row-selected", isSelected);
        const chk = row.querySelector(".chk-select-item");
        if (chk) chk.checked = isSelected;
      }
    } else {
      document.querySelectorAll(".flock-card").forEach(card => {
        const dbIdStr = card.getAttribute("data-dbid");
        if (!dbIdStr) return;
        const dbId = parseInt(dbIdStr, 10);
        const chk = card.querySelector(".circular-checkbox-input");
        const isSelected = selectedChickenIds.has(dbId);
        card.classList.toggle("card-selected", isSelected);
        if (chk) chk.checked = isSelected;
      });

      document.querySelectorAll(".flock-table-row").forEach(row => {
        const dbIdStr = row.getAttribute("data-dbid");
        if (!dbIdStr) return;
        const dbId = parseInt(dbIdStr, 10);
        const chk = row.querySelector(".chk-select-item");
        const isSelected = selectedChickenIds.has(dbId);
        row.classList.toggle("row-selected", isSelected);
        if (chk) chk.checked = isSelected;
      });
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
    console.log("Rendering cards/table:", birdsData.length);
    emptyStateBlock.style.display = "none";
    const isGrid = visualViewport.classList.contains("view-mode-grid");
    gridDeck.style.display = isGrid ? "grid" : "none";
    listTableDeck.style.display = isGrid ? "none" : "block";

    gridDeck.innerHTML = "";
    birdsData.forEach(b => {
      const card = document.createElement("div");
      card.setAttribute("data-dbid", b.dbId);
      card.setAttribute("tabindex", "0");
      card.setAttribute("role", "button");
      card.setAttribute("aria-label", `Chicken ${b.name} ${b.id}`);
      const isChecked = selectedChickenIds.has(b.dbId);
      card.className = `flock-card reveal-on-scroll revealed ${isChecked ? 'card-selected' : ''}`;
      card.style.position = "relative";
      let emoji = b.gender === "Rooster" ? "🐓" : (b.category === "Chick" ? "🐥" : "🐔");
      let healthVal = b.health.toLowerCase().includes("healthy") ? "healthy" : "treatment";
      let statusVal = b.status.toLowerCase();

      card.innerHTML = `
        <div class="flock-card-select-overlay">
          <input type="checkbox" id="chk-bird-${b.dbId}" class="circular-checkbox-input chk-select-item" data-dbid="${b.dbId}" ${isChecked ? 'checked' : ''}>
          <label for="chk-bird-${b.dbId}" class="circular-checkbox-label" title="Select Chicken">
            <i class="fa-solid fa-check check-icon"></i>
          </label>
        </div>

        <div class="flock-card-actions-wrapper" style="position: absolute; top: 12px; right: 12px; z-index: 4; display: flex; gap: 6px; align-items: center;">
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

      let pressTimer = null;
      let isLongPressed = false;

      const startPress = (e) => {
        if (e.target.closest(".btn-qr-trigger") || e.target.closest(".dropdown")) return;
        isLongPressed = false;
        if (pressTimer) clearTimeout(pressTimer);
        pressTimer = setTimeout(() => {
          isLongPressed = true;
          toggleSelection(b.dbId);
        }, 500);
      };

      const cancelPress = () => {
        if (pressTimer) {
          clearTimeout(pressTimer);
          pressTimer = null;
        }
      };

      card.addEventListener("touchstart", startPress, { passive: true });
      card.addEventListener("touchmove", cancelPress, { passive: true });
      card.addEventListener("touchend", (e) => {
        cancelPress();
        if (isLongPressed) {
          isLongPressed = false;
        }
      }, { passive: true });
      card.addEventListener("touchcancel", cancelPress, { passive: true });

      card.addEventListener("contextmenu", (e) => {
        e.preventDefault();
        toggleSelection(b.dbId);
      });

      card.addEventListener("click", (e) => {
        if (e.target.closest(".btn-qr-trigger") || e.target.closest(".dropdown")) {
          return;
        }
        if (isLongPressed) {
          isLongPressed = false;
          return;
        }
        if (isSelectionMode) {
          toggleSelection(b.dbId);
        } else {
          openDetailWorkspace(b);
        }
      });

      const chkBox = card.querySelector(".circular-checkbox-input");
      if (chkBox) {
        chkBox.addEventListener("change", (e) => {
          e.stopPropagation();
          toggleSelection(b.dbId);
        });
      }

      const dotBtn = card.querySelector(".btn-three-dot");
      const dropMenu = card.querySelector(".dropdown-menu-list");
      if (dotBtn && dropMenu) {
        dotBtn.addEventListener("click", (e) => {
          e.stopPropagation();
          document.querySelectorAll(".dropdown-menu-list").forEach(m => { if (m !== dropMenu) m.style.display = "none"; });
          dropMenu.style.display = dropMenu.style.display === "none" ? "block" : "none";
        });
      }

      card.querySelector(".chk-id-badge")?.addEventListener("click", () => openDetailWorkspace(b));
      card.querySelector(".btn-view-bio")?.addEventListener("click", (e) => { e.preventDefault(); openDetailWorkspace(b); });
      card.querySelector(".btn-edit-bio")?.addEventListener("click", (e) => { e.preventDefault(); openFormWorkspace(b); });
      card.querySelector(".btn-print-card")?.addEventListener("click", (e) => { e.preventDefault(); if (window.triggerPrintCard) window.triggerPrintCard(b.dbId); });
      card.querySelector(".btn-archive-bird")?.addEventListener("click", async (e) => {
        e.preventDefault();
        if (confirm(`Archive chicken ${b.id}?`)) {
          await Api.post("chickens/bulk-archive", { ids: [b.dbId] });
          showSuccessToast(`Chicken ${b.id} archived.`);
          loadChickensList();
        }
      });
      card.querySelector(".btn-delete-bio")?.addEventListener("click", (e) => { e.preventDefault(); deleteBird(b.dbId, b.id); });
      card.querySelector(".btn-qr-trigger")?.addEventListener("click", () => {
        if (window.showChickenQrModal) window.showChickenQrModal(b);
      });

      gridDeck.appendChild(card);
    });

    tableBody.innerHTML = "";
    birdsData.forEach(b => {
      const tr = document.createElement("tr");
      tr.setAttribute("data-dbid", b.dbId);
      tr.setAttribute("tabindex", "0");
      tr.setAttribute("role", "button");
      tr.setAttribute("aria-label", `Chicken ${b.name} ${b.id}`);
      let isChecked = selectedChickenIds.has(b.dbId);
      tr.className = `flock-table-row ${isChecked ? 'row-selected' : ''}`;
      let emoji = b.gender === "Rooster" ? "🐓" : (b.category === "Chick" ? "🐥" : "🐔");
      let healthVal = b.health.toLowerCase().includes("healthy") ? "healthy" : "treatment";
      let statusVal = b.status.toLowerCase();

      tr.innerHTML = `
        <td class="col-checkbox" style="text-align: center;"><input type="checkbox" class="chk-select-item" data-dbid="${b.dbId}" ${isChecked ? 'checked' : ''} style="width: 18px; height: 18px; accent-color: #10B981; cursor: pointer;"></td>
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

      tr.addEventListener("contextmenu", (e) => {
        e.preventDefault();
        toggleSelection(b.dbId);
      });

      tr.addEventListener("click", (e) => {
        if (e.target.closest(".btn-qr-trigger-tb") || e.target.closest(".dropdown") || e.target.closest("input[type='checkbox']")) {
          return;
        }
        if (isSelectionMode) {
          toggleSelection(b.dbId);
        } else {
          openDetailWorkspace(b);
        }
      });

      const trChk = tr.querySelector(".chk-select-item");
      if (trChk) {
        trChk.addEventListener("change", (e) => {
          e.stopPropagation();
          toggleSelection(b.dbId);
        });
      }

      const dotBtnT = tr.querySelector(".btn-three-dot-tb");
      const dropMenuT = tr.querySelector(".dropdown-menu-list-tb");
      if (dotBtnT && dropMenuT) {
        dotBtnT.addEventListener("click", (e) => {
          e.stopPropagation();
          document.querySelectorAll(".dropdown-menu-list-tb").forEach(m => { if (m !== dropMenuT) m.style.display = "none"; });
          dropMenuT.style.display = dropMenuT.style.display === "none" ? "block" : "none";
        });
      }

      tr.querySelector(".text-green")?.addEventListener("click", () => openDetailWorkspace(b));
      tr.querySelector(".btn-view-bio-tb")?.addEventListener("click", (e) => { e.preventDefault(); openDetailWorkspace(b); });
      tr.querySelector(".btn-edit-bio-tb")?.addEventListener("click", (e) => { e.preventDefault(); openFormWorkspace(b); });
      tr.querySelector(".btn-print-card-tb")?.addEventListener("click", (e) => { e.preventDefault(); if (window.triggerPrintCard) window.triggerPrintCard(b.dbId); });
      tr.querySelector(".btn-archive-bird-tb")?.addEventListener("click", async (e) => {
        e.preventDefault();
        if (confirm(`Archive chicken ${b.id}?`)) {
          await Api.post("chickens/bulk-archive", { ids: [b.dbId] });
          showSuccessToast(`Chicken ${b.id} archived.`);
          loadChickensList();
        }
      });
      tr.querySelector(".btn-delete-bio-tb")?.addEventListener("click", (e) => { e.preventDefault(); deleteBird(b.dbId, b.id); });
      tr.querySelector(".btn-qr-trigger-tb")?.addEventListener("click", () => {
        if (window.showChickenQrModal) window.showChickenQrModal(b);
      });

      tableBody.appendChild(tr);
    });

    updateBulkToolbarUI();
  }

  // Select All handlers
  const handleSelectAll = (isChecked) => {
    if (isChecked) {
      if (!isSelectionMode) enterSelectionMode();
      birdsData.forEach(b => selectedChickenIds.add(b.dbId));
    } else {
      birdsData.forEach(b => selectedChickenIds.delete(b.dbId));
      if (selectedChickenIds.size === 0) exitSelectionMode();
    }
    updateBulkToolbarUI();
  };
  const chkSelectAll = document.getElementById("chk-select-all");
  const chkTableSelectAll = document.getElementById("chk-table-select-all");
  if (chkSelectAll) chkSelectAll.addEventListener("change", (e) => handleSelectAll(e.target.checked));
  if (chkTableSelectAll) chkTableSelectAll.addEventListener("change", (e) => handleSelectAll(e.target.checked));

  // Cancel Selection
  const btnBulkCancel = document.getElementById("btn-bulk-cancel");
  if (btnBulkCancel) btnBulkCancel.addEventListener("click", exitSelectionMode);

  // Delete / Bulk Archive
  const btnBulkDelete = document.getElementById("btn-bulk-delete") || document.getElementById("btn-bulk-archive");
  if (btnBulkDelete) {
    btnBulkDelete.addEventListener("click", async () => {
      if (selectedChickenIds.size === 0) return;
      if (confirm(`Are you sure you want to delete ${selectedChickenIds.size} selected chickens?`)) {
        try {
          await Api.post("chickens/bulk-archive", { ids: Array.from(selectedChickenIds) });
          showSuccessToast(`Successfully deleted ${selectedChickenIds.size} chickens.`);
          exitSelectionMode();
          loadChickensList();
        } catch (err) {
          console.error(err);
        }
      }
    });
  }

  // Mark Sold
  const btnBulkMarkSold = document.getElementById("btn-bulk-mark-sold");
  if (btnBulkMarkSold) {
    btnBulkMarkSold.addEventListener("click", async () => {
      if (selectedChickenIds.size === 0) return;
      if (confirm(`Mark ${selectedChickenIds.size} selected chickens as SOLD?`)) {
        try {
          const ids = Array.from(selectedChickenIds);
          for (const dbId of ids) {
            const chk = birdsData.find(b => b.dbId === dbId);
            if (chk) {
              await Api.put(`chickens/${dbId}`, { chickenCode: chk.id, status: 'SOLD' });
            }
          }
          showSuccessToast(`Marked ${ids.length} chickens as SOLD.`);
          exitSelectionMode();
          loadChickensList();
        } catch (err) {
          console.error("Bulk mark sold failed", err);
        }
      }
    });
  }

  // Mark Dead
  const btnBulkMarkDead = document.getElementById("btn-bulk-mark-dead");
  if (btnBulkMarkDead) {
    btnBulkMarkDead.addEventListener("click", async () => {
      if (selectedChickenIds.size === 0) return;
      if (confirm(`Mark ${selectedChickenIds.size} selected chickens as DEAD?`)) {
        try {
          const ids = Array.from(selectedChickenIds);
          for (const dbId of ids) {
            const chk = birdsData.find(b => b.dbId === dbId);
            if (chk) {
              await Api.put(`chickens/${dbId}`, { chickenCode: chk.id, status: 'DEAD', healthStatus: 'DECEASED' });
            }
          }
          showSuccessToast(`Marked ${ids.length} chickens as DEAD.`);
          exitSelectionMode();
          loadChickensList();
        } catch (err) {
          console.error("Bulk mark dead failed", err);
        }
      }
    });
  }

  // Health Update
  const btnBulkHealthUpdate = document.getElementById("btn-bulk-health-update");
  if (btnBulkHealthUpdate) {
    btnBulkHealthUpdate.addEventListener("click", async () => {
      if (selectedChickenIds.size === 0) return;
      const targetStatus = prompt(`Enter new Health Status for ${selectedChickenIds.size} selected chickens:\n(Healthy, Sick, Under Observation, In Treatment)`, "Healthy");
      if (!targetStatus) return;

      let hVal = targetStatus.toUpperCase().trim().replace(/\s+/g, '_');
      if (hVal === "UNDER_OBSERVATION") hVal = "OBSERVATION";
      if (hVal === "IN_TREATMENT") hVal = "UNDER_TREATMENT";

      try {
        const ids = Array.from(selectedChickenIds);
        for (const dbId of ids) {
          const chk = birdsData.find(b => b.dbId === dbId);
          if (chk) {
            await Api.put(`chickens/${dbId}`, { chickenCode: chk.id, healthStatus: hVal });
          }
        }
        showSuccessToast(`Updated health status for ${ids.length} chickens to ${targetStatus}.`);
        exitSelectionMode();
        loadChickensList();
      } catch (err) {
        console.error("Bulk health update failed", err);
      }
    });
  }

  const btnBulkExport = document.getElementById("btn-bulk-export");
  if (btnBulkExport) {
    btnBulkExport.addEventListener("click", () => {
      const selectedList = birdsData.filter(b => selectedChickenIds.has(b.dbId));
      exportChickensToCSV(selectedList.length > 0 ? selectedList : birdsData);
      exitSelectionMode();
    });
  }

  // ESC key and outside click handling
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && isSelectionMode) {
      exitSelectionMode();
    }
  });

  document.addEventListener("click", (e) => {
    if (isSelectionMode && !e.target.closest(".flock-card") && !e.target.closest(".flock-table-row") && !e.target.closest("#bulk-actions-toolbar")) {
      exitSelectionMode();
    }
  });

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

  function updateFilterBreedDropdown(categoryVal) {
    const breedSel = document.getElementById("filter-breed");
    if (!breedSel) return;
    const currentVal = breedSel.value;
    breedSel.innerHTML = "";

    const defaultOpt = document.createElement("option");
    defaultOpt.value = "";
    defaultOpt.textContent = "All Breeds";
    breedSel.appendChild(defaultOpt);

    const catKey = categoryVal ? (CATS[categoryVal] || categoryVal) : "";
    if (catKey && BREEDS_BY_CATEGORY[catKey]) {
      const breeds = BREEDS_BY_CATEGORY[catKey];
      breeds.forEach(b => {
        const opt = document.createElement("option");
        opt.value = b;
        opt.textContent = b;
        breedSel.appendChild(opt);
      });
      if (breeds.includes(currentVal)) {
        breedSel.value = currentVal;
      } else {
        breedSel.value = "";
      }
    } else {
      const groups = [
        { label: "Country Chicken", breeds: ["Peruvidai", "Siruvidai", "Cross", "Desi Country"] },
        { label: "Broiler", breeds: ["Cobb 500", "Ross 308", "Hubbard", "Arbor Acres"] },
        { label: "Layer", breeds: ["White Leghorn", "Rhode Island Red", "Plymouth Rock", "Sussex"] }
      ];
      groups.forEach(g => {
        const groupEl = document.createElement("optgroup");
        groupEl.label = g.label;
        g.breeds.forEach(b => {
          const opt = document.createElement("option");
          opt.value = b;
          opt.textContent = b;
          groupEl.appendChild(opt);
        });
        breedSel.appendChild(groupEl);
      });
      breedSel.value = currentVal;
    }
  }

  if (advCategory) {
    advCategory.addEventListener("change", (e) => {
      updateFilterBreedDropdown(e.target.value);
      searchPage = 0;
      loadChickensList();
    });
  }

  if (advBreed) {
    advBreed.addEventListener("change", () => {
      searchPage = 0;
      loadChickensList();
    });
  }

  if (btnToggleAdv) {
    btnToggleAdv.addEventListener("click", () => {
      isAdvancedOpen = !isAdvancedOpen;
      if (advFiltersPanel) {
        advFiltersPanel.classList.toggle("open", isAdvancedOpen);
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
        advFiltersPanel.classList.remove("open");
      }
      if (btnToggleAdv) btnToggleAdv.classList.remove("active");
    });
  }

  const btnClearFlockFilters = document.getElementById("btn-flock-clear-filters");
  if (btnClearFlockFilters) {
    btnClearFlockFilters.addEventListener("click", () => {
      [advGender, advBreed, advHealth, advStatus, advCategory, advOrigin, advAgeGroup].forEach(i => {
        if (i) {
          i.value = "";
          if (i.refreshCustomSelect) i.refreshCustomSelect();
        }
      });
      updateFilterBreedDropdown("");
      if (activeFiltersQty) activeFiltersQty.style.display = "none";
      searchPage = 0;
      loadChickensList();
    });
  }

  if (quickTagsGroup) {
    quickTagsGroup.addEventListener("click", (e) => {
      const pill = e.target.closest(".filter-pill");
      if (!pill) return;
      quickTagsGroup.querySelectorAll(".filter-pill").forEach(p => p.classList.remove("active"));
      pill.classList.add("active");
      activeQuickFilter = pill.getAttribute("data-filter");
      searchPage = 0;
      loadChickensList();
    });
  }

  if (wildSearchInput) {
    wildSearchInput.addEventListener("input", (e) => {
      searchQuery = e.target.value.toLowerCase().trim();
      searchPage = 0;
      loadChickensList();
    });
  }

  const btnGrid = document.getElementById("layout-grid-btn");
  if (btnGrid) {
    btnGrid.addEventListener("click", () => {
      const btnTbl = document.getElementById("layout-table-btn");
      if (btnTbl) btnTbl.classList.remove("active");
      btnGrid.classList.add("active");
      if (visualViewport) visualViewport.className = "view-mode-grid";
      renderListLayoutsFromData();
    });
  }

  const btnTable = document.getElementById("layout-table-btn");
  if (btnTable) {
    btnTable.addEventListener("click", () => {
      const btnGrd = document.getElementById("layout-grid-btn");
      if (btnGrd) btnGrd.classList.remove("active");
      btnTable.classList.add("active");
      if (visualViewport) visualViewport.className = "view-mode-table";
      renderListLayoutsFromData();
    });
  }

  function updateLiveSummary() {
    const idVal = document.getElementById("fm-bird-id")?.value || "-";
    const nameVal = document.getElementById("fm-bird-name")?.value || "-";
    const catVal = document.getElementById("fm-category")?.value || "-";
    const breedVal = document.getElementById("fm-breed")?.value || "-";
    const genderVal = document.getElementById("fm-gender")?.value || "-";
    const originVal = (document.getElementById("fm-origin") || document.getElementById("fm-source"))?.value || "-";
    const healthVal = document.getElementById("fm-health")?.value || "-";
    const dobVal = document.getElementById("fm-dob")?.value || "";
    const acqVal = (document.getElementById("fm-purchase-date") || document.getElementById("fm-acq-date"))?.value || "";
    
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
    const sourceInput = document.getElementById("fm-origin") || document.getElementById("fm-source");
    const dobContainer = document.getElementById("fm-dob-container");
    const acqDetails = document.getElementById("wrapper-purchased-fields") || document.getElementById("fm-acq-details-fields");

    if (sourceVal === "Purchased") {
      if (purchasedCard) purchasedCard.classList.add("selected");
      if (farmCard) farmCard.classList.remove("selected");
      if (dobContainer) dobContainer.style.display = "block";
      if (acqDetails) acqDetails.style.display = "block";
      const dobInput = document.getElementById("fm-dob");
      if (dobInput) dobInput.required = false;
    } else {
      if (farmCard) farmCard.classList.add("selected");
      if (purchasedCard) purchasedCard.classList.remove("selected");
      if (dobContainer) dobContainer.style.display = "block";
      if (acqDetails) acqDetails.style.display = "none";
      const dobInput = document.getElementById("fm-dob");
      if (dobInput) dobInput.required = true;
    }
    if (sourceInput) {
      sourceInput.value = sourceVal;
      sourceInput.dispatchEvent(new Event("change"));
    }
    updateLiveSummary();
  }

  setTimeout(() => {
    ["fm-bird-id", "fm-bird-name", "fm-category", "fm-breed", "fm-gender", "fm-dob", "fm-purchase-date", "fm-acq-date", "fm-health"].forEach(id => {
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
      
      const idEl = document.getElementById("fm-bird-id");
      if (idEl) { idEl.value = bird.id; idEl.readOnly = true; }
      const nameEl = document.getElementById("fm-bird-name");
      if (nameEl) { nameEl.value = bird.name; nameEl.readOnly = true; }
      const bandEl = document.getElementById("fm-leg-band");
      if (bandEl) { bandEl.value = bird.band === "None" ? "" : bird.band; bandEl.readOnly = true; }

      const genderEl = document.getElementById("fm-gender");
      if (genderEl) { genderEl.value = bird.gender; if (genderEl.refreshCustomSelect) genderEl.refreshCustomSelect(); }

      const catEl = document.getElementById("fm-category");
      if (catEl) { catEl.value = bird.category; if (catEl.refreshCustomSelect) catEl.refreshCustomSelect(); }

      const breedSel = document.getElementById("fm-breed");
      const breedCont = document.getElementById("fm-breed-container");
      if (breedSel) {
        breedSel.innerHTML = '<option value="" disabled selected hidden></option>';
        if (bird.category && window.BREED_CATEGORIES && window.BREED_CATEGORIES[bird.category]) {
          window.BREED_CATEGORIES[bird.category].forEach(b => {
            const opt = document.createElement("option");
            opt.value = b.value; opt.textContent = b.text;
            breedSel.appendChild(opt);
          });
          breedSel.value = bird.breed;
          if (window.makePremiumSelect) window.makePremiumSelect("fm-breed");
          if (breedCont) breedCont.classList.add("visible");
        }
      }

      const dobEl = document.getElementById("fm-dob");
      if (dobEl) { dobEl.value = bird.dob || ""; dobEl.readOnly = true; }
      
      window.selectOrigin(bird.source);
      const farmCard = document.getElementById("origin-card-farm");
      const purchasedCard = document.getElementById("origin-card-purchased");
      if (farmCard) { farmCard.style.pointerEvents = "none"; farmCard.style.opacity = "0.7"; }
      if (purchasedCard) { purchasedCard.style.pointerEvents = "none"; purchasedCard.style.opacity = "0.7"; }

      const weightEl = document.getElementById("fm-weight");
      if (weightEl) weightEl.value = bird.weight;

      const coopIdEl = document.getElementById("fm-coop-id");
      if (coopIdEl) { coopIdEl.value = bird.coop || "Coop A - Laying Cage"; if (coopIdEl.refreshCustomSelect) coopIdEl.refreshCustomSelect(); }

      const acqDateEl = document.getElementById("fm-purchase-date") || document.getElementById("fm-acq-date");
      if (acqDateEl) { acqDateEl.value = bird.acqDate || "2026-07-16"; acqDateEl.readOnly = true; }

      const acqPriceEl = document.getElementById("fm-purchase-cost") || document.getElementById("fm-acq-price");
      if (acqPriceEl) { acqPriceEl.value = bird.acqPrice || 0; acqPriceEl.readOnly = true; }

      const healthEl = document.getElementById("fm-health");
      if (healthEl) { healthEl.value = bird.health; if (healthEl.refreshCustomSelect) healthEl.refreshCustomSelect(); }

      const statusEl = document.getElementById("fm-status") || document.getElementById("fm-purpose");
      if (statusEl) { statusEl.value = bird.status; if (statusEl.refreshCustomSelect) statusEl.refreshCustomSelect(); }

      const notesEl = document.getElementById("fm-notes");
      if (notesEl) notesEl.value = bird.notes || "";
      
      const ageBox = document.getElementById("fm-calculated-age-display");
      if (ageBox) ageBox.textContent = bird.ageText || "N/A";
    } else {
      editTargetId = null;
      if (titleEl) titleEl.innerHTML = `<i class="fa-solid fa-square-plus" style="color: var(--primary-green);"></i> Register Chicken`;
      if (subTitleEl) subTitleEl.textContent = "Introduce a new chicken to the farm registry system.";
      
      const formEditor = document.getElementById("form-fullpage-bird-editor");
      if (formEditor) formEditor.reset();

      ["fm-bird-id", "fm-bird-name", "fm-leg-band", "fm-dob", "fm-purchase-date", "fm-acq-date", "fm-purchase-cost", "fm-acq-price"].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.readOnly = false;
      });
      
      const farmCard = document.getElementById("origin-card-farm");
      const purchasedCard = document.getElementById("origin-card-purchased");
      if (farmCard) { farmCard.style.pointerEvents = "auto"; farmCard.style.opacity = "1"; }
      if (purchasedCard) { purchasedCard.style.pointerEvents = "auto"; purchasedCard.style.opacity = "1"; }
      
      const idEl = document.getElementById("fm-bird-id");
      if (idEl) idEl.value = `C0${Math.floor(Math.random() * 900) + 100}`;

      const acqDateEl = document.getElementById("fm-purchase-date") || document.getElementById("fm-acq-date");
      if (acqDateEl) acqDateEl.value = new Date().toISOString().split('T')[0];

      const dobEl = document.getElementById("fm-dob");
      if (dobEl) dobEl.value = "";
      
      window.selectOrigin("Farm Born");
      ["fm-gender", "fm-category", "fm-coop-id", "fm-health", "fm-status", "fm-purpose"].forEach(selId => {
        const selectEl = document.getElementById(selId);
        if (selectEl && selectEl.refreshCustomSelect) selectEl.refreshCustomSelect();
      });
      const breedSel = document.getElementById("fm-breed");
      if (breedSel) {
        breedSel.innerHTML = '<option value="" disabled selected hidden></option>';
        if (breedSel.refreshCustomSelect) breedSel.refreshCustomSelect();
      }
      const breedCont = document.getElementById("fm-breed-container");
      if (breedCont) breedCont.classList.remove("visible");
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
    const bid = document.getElementById("fm-bird-id")?.value?.trim() || `C0${Math.floor(Math.random() * 900) + 100}`;
    const bname = document.getElementById("fm-bird-name")?.value?.trim() || "";
    let bband = document.getElementById("fm-leg-band") ? document.getElementById("fm-leg-band").value.trim() : "";
    let bwing = document.getElementById("fm-wing-tag") ? document.getElementById("fm-wing-tag").value.trim() : "";

    let bgender = document.getElementById("fm-gender")?.value;
    if (!bgender || bgender === "Unknown") bgender = "Hen";

    let bcategory = document.getElementById("fm-category")?.value;
    if (!bcategory) bcategory = "Country Chicken";

    let bbreed = document.getElementById("fm-breed")?.value;
    if (!bbreed) bbreed = "Other";

    const bdob = document.getElementById("fm-dob")?.value || "";
    const rawWeight = parseFloat(document.getElementById("fm-weight")?.value);
    
    const bsource = (document.getElementById("fm-origin") || document.getElementById("fm-source"))?.value || "Farm Born";
    const bstatus = (document.getElementById("fm-status") || document.getElementById("fm-purpose"))?.value || "Active";
    const bhealth = document.getElementById("fm-health")?.value || "Healthy";
    const bnotes = document.getElementById("fm-notes") ? document.getElementById("fm-notes").value.trim() : "";
    const acqDate = (document.getElementById("fm-purchase-date") || document.getElementById("fm-acq-date"))?.value || "";
    const acqPriceEl = document.getElementById("fm-purchase-cost") || document.getElementById("fm-acq-price");
    const acqPrice = acqPriceEl ? parseFloat(acqPriceEl.value) : 0;

    let isFormValid = true;
    document.querySelectorAll(".floating-label-group").forEach(grp => grp.classList.remove("has-error"));
    document.querySelectorAll(".validation-message").forEach(msg => msg.style.display = "none");

    if (!bname) {
      const nameInp = document.getElementById("fm-bird-name");
      if (nameInp) nameInp.closest(".floating-label-group")?.classList.add("has-error");
      const errName = document.getElementById("err-bird-name");
      if (errName) errName.style.display = "block";
      isFormValid = false;
    }

    if (bsource === "Farm Born" && !bdob) {
      const dobInp = document.getElementById("fm-dob");
      if (dobInp) dobInp.closest(".floating-label-group")?.classList.add("has-error");
      const errDob = document.getElementById("err-bird-dob");
      if (errDob) errDob.style.display = "block";
      isFormValid = false;
    }

    // Weight validation (must be positive > 0)
    if (!isNaN(rawWeight) && rawWeight <= 0) {
      const weightGroup = document.getElementById("fm-weight")?.closest(".floating-label-group");
      if (weightGroup) weightGroup.classList.add("has-error");
      let errWeight = document.getElementById("err-bird-weight");
      if (!errWeight && weightGroup) {
        errWeight = document.createElement("span");
        errWeight.id = "err-bird-weight";
        errWeight.className = "validation-message";
        errWeight.style.cssText = "color:#DC2626; font-size:0.75rem; display:block; margin-top:4px;";
        errWeight.textContent = "Chicken weight must be greater than 0 kg.";
        weightGroup.appendChild(errWeight);
      }
      if (errWeight) errWeight.style.display = "block";
      isFormValid = false;
    }

    // Future birth date validation
    if (bdob && new Date(bdob) > new Date()) {
      const dobGroup = document.getElementById("fm-dob")?.closest(".floating-label-group");
      if (dobGroup) dobGroup.classList.add("has-error");
      let errDobFuture = document.getElementById("err-bird-dob-future");
      if (!errDobFuture && dobGroup) {
        errDobFuture = document.createElement("span");
        errDobFuture.id = "err-bird-dob-future";
        errDobFuture.className = "validation-message";
        errDobFuture.style.cssText = "color:#DC2626; font-size:0.75rem; display:block; margin-top:4px;";
        errDobFuture.textContent = "Date of birth cannot be in the future.";
        dobGroup.appendChild(errDobFuture);
      }
      if (errDobFuture) errDobFuture.style.display = "block";
      isFormValid = false;
    }

    // Purchase date validation
    if (bsource === "Purchased" && acqDate) {
      const acqDateObj = new Date(acqDate);
      const dobObj = bdob ? new Date(bdob) : null;
      const todayObj = new Date();

      if (acqDateObj > todayObj || (dobObj && acqDateObj < dobObj)) {
        const acqInput = document.getElementById("fm-purchase-date") || document.getElementById("fm-acq-date");
        const acqGroup = acqInput ? acqInput.closest(".floating-label-group") : null;
        if (acqGroup) acqGroup.classList.add("has-error");
        let errAcq = document.getElementById("err-bird-acq-date");
        if (!errAcq && acqGroup) {
          errAcq = document.createElement("span");
          errAcq.id = "err-bird-acq-date";
          errAcq.className = "validation-message";
          errAcq.style.cssText = "color:#DC2626; font-size:0.75rem; display:block; margin-top:4px;";
          errAcq.textContent = dobObj && acqDateObj < dobObj ? "Purchase date cannot be before date of birth." : "Purchase date cannot be in the future.";
          acqGroup.appendChild(errAcq);
        }
        if (errAcq) {
          errAcq.textContent = dobObj && acqDateObj < dobObj ? "Purchase date cannot be before date of birth." : "Purchase date cannot be in the future.";
          errAcq.style.display = "block";
        }
        isFormValid = false;
      }
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

    const bweight = (!isNaN(rawWeight) && rawWeight > 0) ? rawWeight : 1.5;

    const payload = {
      chickenCode: bid,
      name: bname,
      breed: BREEDS[bbreed] || "OTHER",
      category: CATS[bcategory] || "OTHER",
      gender: bgender === "Rooster" ? "MALE" : (bgender === "Hen" ? "FEMALE" : "UNKNOWN"),
      dateOfBirth: bdob || new Date().toISOString().split('T')[0],
      weight: bweight,
      color: document.getElementById("fm-color")?.value?.trim() || undefined,
      legBandNumber: bband || undefined,
      wingTagNumber: bwing || undefined,
      healthStatus: bhealth.toUpperCase().replace(/\s+/g, '_'),
      supplierName: document.getElementById("fm-supplier-name")?.value?.trim() || undefined,
      supplierContact: document.getElementById("fm-supplier-contact")?.value?.trim() || undefined,
      fatherId: document.getElementById("fm-father-id")?.value ? parseInt(document.getElementById("fm-father-id").value) : undefined,
      motherId: document.getElementById("fm-mother-id")?.value ? parseInt(document.getElementById("fm-mother-id").value) : undefined,
      origin: bsource === "Farm Born" ? "FARM_BORN" : "PURCHASED",
      purchaseDate: bsource === "Purchased" ? (acqDate || undefined) : undefined,
      purchaseCost: bsource === "Purchased" ? (!isNaN(acqPrice) ? acqPrice : 0.0) : undefined,
      status: STATUS[bstatus] || "ACTIVE",
      remarks: bnotes || undefined
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
        setTimeout(() => { errToast.style.transform = "translateY(100px)"; errToast.style.opacity = "0"; }, 4500);
      });
  });

  let currentProfileData = null;
  let activeProfileTab = "overview";

  async function openDetailWorkspace(bird) {
    switchView(detailWorkspace);
    const layoutOutput = document.getElementById("profile-detailed-container");
    if (layoutOutput) {
      layoutOutput.innerHTML = `
        <div style="text-align: center; padding: 60px 24px; background: #FFFFFF; border-radius: 16px; margin: 20px auto; max-width: 600px; box-shadow: 0 4px 20px -2px rgba(15,23,42,0.04);">
          <i class="fa-solid fa-spinner fa-spin-pulse" style="font-size: 2rem; color: #10B981;"></i>
          <p style="margin-top: 12px; color: #64748B; font-size: 0.9rem;">Loading chicken profile...</p>
        </div>
      `;
    }
    try {
      const dbId = typeof bird === 'object' ? (bird.dbId || bird.id) : bird;
      if (!dbId) {
        console.error("Invalid bird parameter passed to openDetailWorkspace:", bird);
        renderProfileErrorState("Chicken Not Found", "No valid chicken ID or code was specified.");
        return;
      }

      let profileData = null;
      if (!isNaN(dbId) && Number(dbId) > 0) {
        const res = await Api.get(`chickens/${dbId}`);
        profileData = (res && res.data) ? res.data : (res && (res.id || res.chickenCode) ? res : null);
      } else {
        const res = await Api.get(`chickens/code/${dbId}`);
        profileData = (res && res.data) ? res.data : (res && (res.id || res.chickenCode) ? res : null);
      }

      if (profileData) {
        currentProfileData = profileData;
        renderDetailedProfile(profileData);
      } else {
        renderProfileErrorState("Chicken Not Found", `No chicken record found matching identifier "${dbId}".`);
      }
    } catch (e) {
      console.error("Failed to load chicken profile:", e);
      renderProfileErrorState("Unable to load chicken profile", "We encountered an issue retrieving profile records from the database. Please try again.");
    }
  }

  function renderProfileErrorState(title = "Unable to load chicken details", message = "We encountered an issue retrieving the profile records from the database. Please try again or return to the registry workspace.") {
    const skele = document.getElementById("flock-skeleton-loader");
    if (skele) skele.style.display = "none";
    gridDeck.style.display = "none";
    listTableDeck.style.display = "none";
    emptyStateBlock.style.display = "none";
    
    switchView(detailWorkspace);
    const layoutOutput = document.getElementById("profile-detailed-container");
    if (layoutOutput) {
      layoutOutput.innerHTML = `
        <div style="background: #FFFFFF; border: 1px solid #FECDD3; border-radius: 16px; padding: 48px 24px; text-align: center; max-width: 600px; margin: 40px auto; box-shadow: 0 10px 25px rgba(225,29,72,0.05);">
          <div style="width: 64px; height: 64px; border-radius: 50%; background: #FFE4E6; color: #E11D48; display: flex; align-items: center; justify-content: center; font-size: 1.8rem; margin: 0 auto 16px;">
            <i class="fa-solid fa-circle-exclamation"></i>
          </div>
          <h3 style="font-size: 1.25rem; font-weight: 700; color: #9F1239; margin-bottom: 8px;">${title}</h3>
          <p style="color: #64748B; font-size: 0.9rem; margin-bottom: 24px;">${message}</p>
          <button class="btn btn-primary" onclick="document.getElementById('btn-detail-back').click();" style="display: inline-flex; align-items: center; gap: 8px; background: #10B981; border-color: #10B981;">
            <i class="fa-solid fa-arrow-left"></i> Return to Registry
          </button>
        </div>
      `;
    }
  }

  function renderProfileTabContent(data) {
    try {
      const layoutOutput = document.getElementById("profile-detailed-container");
      if (!layoutOutput) return;

      let origin = data.origin ? (data.origin === "FARM_BORN" ? "Farm Born" : "Purchased") : "Farm Born";
      let ageText = "N/A";
      let daysOld = 0;
      if (data.dateOfBirth) {
        const dob = new Date(data.dateOfBirth);
        const now = new Date();
        if (!isNaN(dob.getTime()) && dob <= now) {
          daysOld = Math.floor(Math.abs(now - dob) / (1000 * 60 * 60 * 24));
          const weeks = Math.floor(daysOld / 7);
          const months = Math.floor(daysOld / 30.4375);
          if (daysOld < 14) ageText = `${daysOld} Days`;
          else if (daysOld < 60) ageText = `${daysOld} Days (${weeks} Wks)`;
          else ageText = `${daysOld} Days (${months} Mo, ${weeks} Wks)`;
        }
      } else if (data.ageInDays !== null && data.ageInDays !== undefined) {
        daysOld = Math.max(0, data.ageInDays);
        const weeks = Math.floor(daysOld / 7);
        const months = Math.floor(daysOld / 30.4375);
        ageText = `${daysOld} Days (${months} Mo, ${weeks} Wks)`;
      }

      let emoji = data.gender === "MALE" ? "🐓" : (data.category === "CHICK" ? "🐥" : "🐔");
      let breedUI = BREEDS_REV[data.breed] || data.breed || "Other";
      let categoryUI = CATS_REV[data.category] || data.category || "Other";
      let genderUI = data.gender === "MALE" ? "Rooster" : (data.gender === "FEMALE" ? "Hen" : "Unknown");
      let statusUI = data.status || "ACTIVE";
      let healthUI = data.healthStatus ? data.healthStatus.replace(/_/g, ' ') : "Healthy";

      let healthBadgeClass = "badge-healthy";
      const lowerHealth = healthUI.toLowerCase();
      if (lowerHealth.includes("sick")) healthBadgeClass = "badge-sick";
      else if (lowerHealth.includes("sold")) healthBadgeClass = "badge-sold";
      else if (lowerHealth.includes("dead")) healthBadgeClass = "badge-dead";

      let catBadgeClass = "badge-broiler";
      const lowerCat = categoryUI.toLowerCase();
      if (lowerCat.includes("country")) catBadgeClass = "badge-country";
      else if (lowerCat.includes("layer")) catBadgeClass = "badge-layer";

      const photoContent = data.photoUrl 
        ? `<img src="${data.photoUrl}" id="img-profile-photo">` 
        : `<span id="span-profile-emoji" style="font-size: 4.8rem; display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; line-height: 1;">${emoji}</span>`;

      // 1. HERO PROFILE CARD (EXACT REFERENCE SCREENSHOT LAYOUT)
      const heroHeader = `
        <div class="saas-panel-card saas-hero-grid-container" style="margin-bottom: 12px; padding: 12px 18px !important;">
          
          <!-- Column 1: Photo Column (180px) -->
          <div class="hero-photo-col" style="display: flex; flex-direction: column; align-items: center; justify-content: center;">
            <div class="profile-photo-circle-ref" id="profile-photo-wrapper" title="Click to upload/replace photo">
              ${photoContent}
              <div class="profile-camera-badge" title="Upload/Change Photo">
                <i class="fa-solid fa-camera"></i>
              </div>
            </div>
            <input type="file" id="input-profile-photo-upload" accept="image/jpeg,image/jpg,image/png,image/webp" style="display: none;">
            <div style="display: flex; flex-direction: row; gap: 6px; justify-content: center; margin-top: 8px;">
              <button type="button" class="btn-profile-action" id="btn-upload-photo" style="font-size:0.72rem; font-weight:600; padding:4px 8px; background:#FFFFFF; border:1px solid #CBD5E1; color:#334155; border-radius:6px; display:inline-flex; align-items:center; gap:4px;"><i class="fa-solid fa-upload" style="font-size:0.68rem;"></i> Upload Photo</button>
              <button type="button" class="btn-profile-action" id="btn-replace-photo" style="font-size:0.72rem; font-weight:600; padding:4px 8px; background:#FFFFFF; border:1px solid #CBD5E1; color:#334155; border-radius:6px; display:inline-flex; align-items:center; gap:4px;" onclick="document.getElementById('input-profile-photo-upload').click();"><i class="fa-solid fa-rotate" style="font-size:0.68rem;"></i> Replace Photo</button>
              ${data.photoUrl ? `<button type="button" class="btn-profile-action btn-action-danger" id="btn-remove-photo" style="font-size:0.72rem; padding:4px 8px;"><i class="fa-solid fa-trash"></i></button>` : ''}
            </div>
          </div>

          <!-- Column 2: Information Column (1fr) Starts at top -->
          <div class="hero-info-col" style="display: flex; flex-direction: column; gap: 8px; align-items: flex-start;">
            
            <!-- Row 1: Name & Code Badge -->
            <div style="display: flex; align-items: center; gap: 10px; flex-wrap: nowrap;">
              <h1 style="font-size: 1.65rem; font-weight: 800; color: #0F172A; margin: 0; line-height: 1.1;">${data.name || data.chickenCode}</h1>
              <span style="background: #F1F5F9; color: #475569; padding: 2px 10px; border-radius: 6px; font-weight: 700; font-size: 0.8rem; border: 1px solid #E2E8F0;">${data.chickenCode}</span>
            </div>
            
            <!-- Row 2: Badges (HEALTHY, SOLD, LAYER, RHODE ISLAND RED, FARM BORN) -->
            <div style="display: flex; gap: 6px; flex-wrap: nowrap; overflow-x: auto; align-items: center; width: 100%;">
              <span class="badge-status badge-pill-healthy" style="padding: 4px 10px; border-radius: 16px; font-weight: 700; font-size: 0.72rem; display: inline-flex; align-items: center; gap: 5px; white-space: nowrap;"><i class="fa-solid fa-circle" style="font-size: 5px;"></i> ${healthUI.toUpperCase()}</span>
              <span class="badge-status badge-pill-sold" style="padding: 4px 10px; border-radius: 16px; font-weight: 700; font-size: 0.72rem; display: inline-flex; align-items: center; gap: 5px; white-space: nowrap;"><i class="fa-solid fa-chart-simple"></i> ${statusUI}</span>
              <span class="badge-status badge-pill-layer" style="padding: 4px 10px; border-radius: 16px; font-weight: 700; font-size: 0.72rem; display: inline-flex; align-items: center; gap: 5px; white-space: nowrap;"><i class="fa-solid fa-layer-group"></i> ${categoryUI.toUpperCase()}</span>
              <span class="badge-status badge-pill-breed" style="padding: 4px 10px; border-radius: 16px; font-weight: 700; font-size: 0.72rem; display: inline-flex; align-items: center; gap: 5px; white-space: nowrap;"><i class="fa-solid fa-hourglass-half"></i> ${breedUI.toUpperCase()}</span>
              <span class="badge-status badge-pill-origin" style="padding: 4px 10px; border-radius: 16px; font-weight: 700; font-size: 0.72rem; display: inline-flex; align-items: center; gap: 5px; white-space: nowrap;"><i class="fa-solid fa-house"></i> ${origin.toUpperCase()}</span>
            </div>

            <!-- Row 3 & 4: 2x2 Metadata Grid with Divider Line -->
            <div class="hero-meta-2x2-grid" style="padding-top: 6px; margin-top: 2px;">
              <div style="display: flex; gap: 10px; align-items: center;">
                <div style="width: 28px; height: 28px; border-radius: 6px; background: #FFFBEB; color: #D97706; display: flex; align-items: center; justify-content: center; font-size: 0.95rem; flex-shrink: 0;"><i class="fa-solid fa-cake-candles"></i></div>
                <div>
                  <span class="saas-kv-label" style="font-size:0.72rem; display:block; color: #64748B; line-height: 1.1;">Age</span>
                  <strong style="font-size:0.88rem; color:#0F172A; font-weight:700;">${ageText}</strong>
                </div>
              </div>

              <div style="display: flex; gap: 10px; align-items: center;">
                <div style="width: 28px; height: 28px; border-radius: 6px; background: #FCE7F3; color: #DB2777; display: flex; align-items: center; justify-content: center; font-size: 0.95rem; flex-shrink: 0;"><i class="fa-solid fa-venus-mars"></i></div>
                <div>
                  <span class="saas-kv-label" style="font-size:0.72rem; display:block; color: #64748B; line-height: 1.1;">Gender</span>
                  <strong style="font-size:0.88rem; color:#0F172A; font-weight:700;">${genderUI}</strong>
                </div>
              </div>

              <div style="display: flex; gap: 10px; align-items: center;">
                <div style="width: 28px; height: 28px; border-radius: 6px; background: #ECFDF5; color: #059669; display: flex; align-items: center; justify-content: center; font-size: 0.95rem; flex-shrink: 0;"><i class="fa-solid fa-weight-scale"></i></div>
                <div>
                  <span class="saas-kv-label" style="font-size:0.72rem; display:block; color: #64748B; line-height: 1.1;">Weight</span>
                  <strong style="font-size:0.88rem; color:#0F172A; font-weight:700;">${data.weight || 2.5} kg</strong>
                </div>
              </div>

              <div style="display: flex; gap: 10px; align-items: center;">
                <div style="width: 28px; height: 28px; border-radius: 6px; background: #EFF6FF; color: #2563EB; display: flex; align-items: center; justify-content: center; font-size: 0.95rem; flex-shrink: 0;"><i class="fa-solid fa-calendar"></i></div>
                <div>
                  <span class="saas-kv-label" style="font-size:0.72rem; display:block; color: #64748B; line-height: 1.1;">Reg Date</span>
                  <strong style="font-size:0.88rem; color:#0F172A; font-weight:700;">${data.createdAt ? new Date(data.createdAt).toLocaleDateString('en-GB') : '31/07/2026'}</strong>
                </div>
              </div>
            </div>

          </div>

          <!-- Column 3: QR Column (210px) -->
          <div class="hero-qr-col" style="background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 12px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; align-self: stretch; box-sizing: border-box;">
            <span style="font-size:0.82rem; font-weight:700; color:#0F172A;">Digital Tracking Pass</span>
            <div style="position: relative; padding: 8px; background: #FFFFFF; border-radius: 10px; box-shadow: 0 2px 6px rgba(0,0,0,0.04);">
              <!-- Green Corner Brackets -->
              <div style="position: absolute; top: 3px; left: 3px; width: 12px; height: 12px; border-top: 2px solid #10B981; border-left: 2px solid #10B981; border-top-left-radius: 3px;"></div>
              <div style="position: absolute; top: 3px; right: 3px; width: 12px; height: 12px; border-top: 2px solid #10B981; border-right: 2px solid #10B981; border-top-right-radius: 3px;"></div>
              <div style="position: absolute; bottom: 3px; left: 3px; width: 12px; height: 12px; border-bottom: 2px solid #10B981; border-left: 2px solid #10B981; border-bottom-left-radius: 3px;"></div>
              <div style="position: absolute; bottom: 3px; right: 3px; width: 12px; height: 12px; border-bottom: 2px solid #10B981; border-right: 2px solid #10B981; border-bottom-right-radius: 3px;"></div>
              
              <div style="position: relative; width: 75px; height: 75px; display: flex; align-items: center; justify-content: center;">
                <canvas id="hero-qr-canvas" width="75" height="75" style="width: 75px; height: 75px; border-radius: 4px; display: block;"></canvas>
                <div style="position: absolute; width: 20px; height: 20px; background: #FFFFFF; border-radius: 50%; border: 1.5px solid #10B981; display: flex; align-items: center; justify-content: center; font-size: 0.68rem; box-shadow: 0 1px 4px rgba(0,0,0,0.1); overflow: hidden;">
                  ${data.photoUrl ? `<img src="${data.photoUrl}" style="width:100%; height:100%; object-fit:cover;">` : '🐔'}
                </div>
              </div>
            </div>
            <div style="display: flex; gap: 6px; width: 100%;">
              <button type="button" class="btn-profile-action" id="btn-profile-download-qr" style="flex: 1; font-size:0.72rem; font-weight:600; padding:5px 4px; background:#FFFFFF; border:1px solid #CBD5E1; color:#334155; border-radius:6px; display:inline-flex; align-items:center; justify-content:center; gap:3px;"><i class="fa-solid fa-download" style="font-size:0.68rem;"></i> Download QR</button>
              <button type="button" class="btn-profile-action" id="btn-profile-print-qr" style="flex: 1; font-size:0.72rem; font-weight:600; padding:5px 4px; background:#FFFFFF; border:1px solid #CBD5E1; color:#334155; border-radius:6px; display:inline-flex; align-items:center; justify-content:center; gap:3px;"><i class="fa-solid fa-print" style="font-size:0.68rem;"></i> Print ID Label</button>
            </div>
          </div>

        </div>
      `;

      // 2. FOUR KPI CARDS ROW (EXACT REFERENCE SCREENSHOT COPY)
      const quickSummaryCards = `
        <div class="ref-kpi-quad-grid">
          
          <!-- Card 1: Health Score -->
          <div class="ref-kpi-card">
            <div>
              <div style="display:flex; align-items:center; gap:8px; margin-bottom:6px;">
                <div style="width:30px; height:30px; border-radius:50%; background:#ECFDF5; color:#10B981; display:flex; align-items:center; justify-content:center; font-size:0.9rem;"><i class="fa-solid fa-shield-halved"></i></div>
                <span class="saas-kv-label" style="font-size:0.85rem; font-weight:700; color:#0F172A;">Health Score</span>
              </div>
              <div style="font-size: 1.7rem; font-weight: 800; color: #10B981; line-height: 1.1;">92%</div>
              <span style="font-size:0.78rem; color:#64748B; font-weight:600; margin-top:4px; display:block;">Excellent</span>
            </div>
            <svg width="60" height="32" viewBox="0 0 60 32" fill="none"><path d="M2 28L15 22L30 25L45 10L58 4" stroke="#10B981" stroke-width="2.5" stroke-linecap="round"/></svg>
          </div>

          <!-- Card 2: Current Weight -->
          <div class="ref-kpi-card">
            <div>
              <div style="display:flex; align-items:center; gap:8px; margin-bottom:6px;">
                <div style="width:30px; height:30px; border-radius:8px; background:#EFF6FF; color:#3B82F6; display:flex; align-items:center; justify-content:center; font-size:0.9rem;"><i class="fa-solid fa-weight-scale"></i></div>
                <span class="saas-kv-label" style="font-size:0.85rem; font-weight:700; color:#0F172A;">Current Weight</span>
              </div>
              <div style="font-size: 1.7rem; font-weight: 800; color: #0F172A; line-height: 1.1;">${data.weight || 2.5} kg</div>
              <span style="font-size:0.78rem; color:#64748B; font-weight:600; margin-top:4px; display:block;">+ 0.2 kg this month</span>
            </div>
            <svg width="60" height="32" viewBox="0 0 60 32" fill="none"><path d="M2 26L18 24L34 18L48 20L58 8" stroke="#3B82F6" stroke-width="2.5" stroke-linecap="round"/></svg>
          </div>

          <!-- Card 3: Egg Status -->
          <div class="ref-kpi-card">
            <div>
              <div style="display:flex; align-items:center; gap:8px; margin-bottom:6px;">
                <div style="width:30px; height:30px; border-radius:50%; background:#FFFBEB; color:#F59E0B; display:flex; align-items:center; justify-content:center; font-size:0.9rem;"><i class="fa-solid fa-egg"></i></div>
                <span class="saas-kv-label" style="font-size:0.85rem; font-weight:700; color:#0F172A;">Egg Status</span>
              </div>
              <div style="font-size: 1.45rem; font-weight: 800; color: #EA580C; line-height: 1.1;">${data.gender === "FEMALE" ? 'Laying' : 'Not Laying'}</div>
              <span style="font-size:0.78rem; color:#64748B; font-weight:600; margin-top:4px; display:block;">Last Egg: --</span>
            </div>
            <svg width="60" height="32" viewBox="0 0 60 32" fill="none"><path d="M2 28L20 28L38 24L48 27L58 18" stroke="#F59E0B" stroke-width="2.5" stroke-linecap="round"/></svg>
          </div>

          <!-- Card 4: Current Value -->
          <div class="ref-kpi-card">
            <div>
              <div style="display:flex; align-items:center; gap:8px; margin-bottom:6px;">
                <div style="width:30px; height:30px; border-radius:50%; background:#F3E8FF; color:#8B5CF6; display:flex; align-items:center; justify-content:center; font-size:0.9rem;"><i class="fa-solid fa-indian-rupee-sign"></i></div>
                <span class="saas-kv-label" style="font-size:0.85rem; font-weight:700; color:#0F172A;">Current Value</span>
              </div>
              <div style="font-size: 1.7rem; font-weight: 800; color: #6D28D9; line-height: 1.1;">₹ 650.00</div>
              <span style="font-size:0.78rem; color:#64748B; font-weight:600; margin-top:4px; display:block;">Market Value</span>
            </div>
            <svg width="60" height="32" viewBox="0 0 60 32" fill="none"><path d="M2 28L18 25L34 20L48 22L58 10" stroke="#8B5CF6" stroke-width="2.5" stroke-linecap="round"/></svg>
          </div>

        </div>
      `;

      // 3. OVERVIEW TAB LAYOUT (3-PANEL GRID AS PER SCREENSHOT)
      let overviewLayout = `
        <div class="ref-overview-3col-grid">
          
          <!-- Panel 1: Basic Information (2 Columns Key/Value Grid) -->
          <div class="saas-panel-card" style="padding: 12px 14px !important;">
            <h3 class="erp-section-title" style="font-size: 0.9rem !important; margin-bottom: 10px; display:flex; align-items:center; gap:6px; font-weight:700; color:#0F172A;"><i class="fa-solid fa-circle-info" style="color:#10B981;"></i> Basic Information</h3>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
              <div>
                <div style="margin-bottom:6px;"><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Chicken ID</span><strong class="saas-kv-value" style="font-size:0.85rem;">${data.chickenCode}</strong></div>
                <div style="margin-bottom:6px;"><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Chicken Name</span><strong class="saas-kv-value" style="font-size:0.85rem;">${data.name || 'Rhode Rooster 2'}</strong></div>
                <div style="margin-bottom:6px;"><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Breed</span><strong class="saas-kv-value" style="font-size:0.85rem;">${breedUI}</strong></div>
                <div style="margin-bottom:6px;"><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Category</span><strong class="saas-kv-value" style="font-size:0.85rem;">${categoryUI}</strong></div>
                <div><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Gender</span><strong class="saas-kv-value" style="font-size:0.85rem;">${genderUI}</strong></div>
              </div>
              <div>
                <div style="margin-bottom:6px;"><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Colour</span><strong class="saas-kv-value" style="font-size:0.85rem;">${data.color || 'N/A'}</strong></div>
                <div style="margin-bottom:6px;"><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Origin</span><strong class="saas-kv-value" style="font-size:0.85rem;">${origin}</strong></div>
                <div style="margin-bottom:6px;"><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Farm</span><strong class="saas-kv-value" style="font-size:0.85rem;">Sudhakar's Farm</strong></div>
                <div style="margin-bottom:6px;"><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Status</span><span class="badge-status badge-healthy" style="padding:2px 10px; border-radius:12px; font-size:0.7rem; font-weight:700; display:inline-block;">${statusUI}</span></div>
                <div><span class="saas-kv-label" style="display:block; font-size:0.72rem; color:#64748B; margin-bottom:1px;">Registration Date</span><strong class="saas-kv-value" style="font-size:0.85rem;">${data.createdAt ? new Date(data.createdAt).toLocaleDateString('en-GB') : '31/07/2026'}</strong></div>
              </div>
            </div>
          </div>

          <!-- Panel 2: Physical Characteristics (1 Column Key/Value Grid) -->
          <div class="saas-panel-card" style="padding: 12px 14px !important;">
            <h3 class="erp-section-title" style="font-size: 0.9rem !important; margin-bottom: 10px; display:flex; align-items:center; gap:6px; font-weight:700; color:#0F172A;"><i class="fa-solid fa-ruler-combined" style="color:#10B981;"></i> Physical Characteristics</h3>
            <div style="display:flex; flex-direction:column; gap:6px;">
              <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #F1F5F9; padding-bottom:5px;"><span class="saas-kv-label" style="font-size:0.78rem; color:#64748B;">Current Weight</span><strong class="saas-kv-value" style="font-size:0.85rem;">${data.weight || 2.5} kg</strong></div>
              <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #F1F5F9; padding-bottom:5px;"><span class="saas-kv-label" style="font-size:0.78rem; color:#64748B;">Height</span><strong class="saas-kv-value" style="font-size:0.85rem;">N/A</strong></div>
              <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #F1F5F9; padding-bottom:5px;"><span class="saas-kv-label" style="font-size:0.78rem; color:#64748B;">Body Condition</span><strong class="saas-kv-value" style="color:#10B981; font-size:0.85rem; font-weight:700;">Excellent</strong></div>
              <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #F1F5F9; padding-bottom:5px;"><span class="saas-kv-label" style="font-size:0.78rem; color:#64748B;">Wing Tag Number</span><strong class="saas-kv-value" style="font-size:0.85rem;">None</strong></div>
              <div style="display:flex; justify-content:space-between; align-items:center;"><span class="saas-kv-label" style="font-size:0.78rem; color:#64748B;">Leg Band Number</span><strong class="saas-kv-value" style="font-size:0.85rem;">None</strong></div>
            </div>
          </div>

          <!-- Panel 3: Quick Actions (3x3 Tile Grid) -->
          <div class="saas-panel-card" style="padding: 12px 14px !important;">
            <h3 class="erp-section-title" style="font-size: 0.9rem !important; margin-bottom: 10px; display:flex; align-items:center; gap:6px; font-weight:700; color:#0F172A;"><i class="fa-solid fa-bolt" style="color:#0F172A;"></i> Quick Actions</h3>
            <div class="quick-actions-3x3-grid">
              <a href="#" class="action-tile-btn" id="btn-detail-edit-lnk-tile"><i class="fa-solid fa-pen-to-square" style="color:#10B981;"></i> Edit Chicken</a>
              <a href="health-records.html" class="action-tile-btn"><i class="fa-solid fa-heart-pulse" style="color:#EF4444;"></i> Health Check</a>
              <a href="health-records.html" class="action-tile-btn"><i class="fa-solid fa-syringe" style="color:#3B82F6;"></i> Vaccination</a>
              <a href="egg-tracking.html" class="action-tile-btn"><i class="fa-solid fa-egg" style="color:#F59E0B;"></i> Record Egg</a>
              <a href="#" class="action-tile-btn" onclick="if(window.openWeightModal) window.openWeightModal();"><i class="fa-solid fa-weight-scale" style="color:#3B82F6;"></i> Update Weight</a>
              <a href="#" class="action-tile-btn"><i class="fa-solid fa-right-left" style="color:#8B5CF6;"></i> Transfer</a>
              <a href="#" class="action-tile-btn" id="btn-profile-change-status-tile"><i class="fa-solid fa-cart-shopping" style="color:#F59E0B;"></i> Sell Chicken</a>
              <a href="#" class="action-tile-btn" id="btn-profile-print-qr-tile"><i class="fa-solid fa-qrcode" style="color:#10B981;"></i> Print QR</a>
              <a href="#" class="action-tile-btn" id="btn-detail-delete-lnk-tile" style="color:#DC2626;"><i class="fa-solid fa-trash-can" style="color:#DC2626;"></i> Delete</a>
            </div>
          </div>

        </div>
      `;

      // TAB BAR & MAIN CONTAINER
      const tabBar = `
        <div class="erp-tabs-bar" style="margin-bottom: 10px;">
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'overview' ? 'active' : ''}" data-tab="overview"><i class="fa-solid fa-border-all"></i> Overview</button>
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'health' ? 'active' : ''}" data-tab="health"><i class="fa-solid fa-heart-pulse"></i> Health</button>
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'growth' ? 'active' : ''}" data-tab="growth"><i class="fa-solid fa-chart-line"></i> Growth</button>
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'production' ? 'active' : ''}" data-tab="production"><i class="fa-solid fa-egg"></i> Production</button>
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'finance' ? 'active' : ''}" data-tab="finance"><i class="fa-solid fa-indian-rupee-sign"></i> Finance</button>
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'pedigree' ? 'active' : ''}" data-tab="pedigree"><i class="fa-solid fa-dna"></i> Pedigree</button>
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'timeline' ? 'active' : ''}" data-tab="timeline"><i class="fa-solid fa-list-check"></i> Timeline</button>
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'documents' ? 'active' : ''}" data-tab="documents"><i class="fa-solid fa-folder-open"></i> Documents</button>
          <button type="button" class="erp-tab-btn ${activeProfileTab === 'notes' ? 'active' : ''}" data-tab="notes"><i class="fa-solid fa-note-sticky"></i> Notes</button>
        </div>
        <div id="erp-tab-content-area">
          ${activeProfileTab === 'overview' ? overviewLayout : ''}
        </div>
      `;

      // ASSIGN GENERATED HTML TO DOM
      layoutOutput.innerHTML = heroHeader + quickSummaryCards + tabBar;

      // Draw QR Code on Canvas
      setTimeout(() => {
        const qrCanvas = document.getElementById("hero-qr-canvas");
        if (qrCanvas && window.drawQRCodeOnCanvas) {
          const qrStr = `Chicken ID: ${data.chickenCode} | Breed: ${breedUI} | Gender: ${genderUI} | DOB: ${data.dateOfBirth || ''}`;
          window.drawQRCodeOnCanvas("hero-qr-canvas", qrStr);
        }
      }, 50);

      // Attach Tab Click Event Listeners
      const tabBtns = layoutOutput.querySelectorAll(".erp-tab-btn");
      tabBtns.forEach(btn => {
        btn.addEventListener("click", () => {
          tabBtns.forEach(b => b.classList.remove("active"));
          btn.classList.add("active");
          activeProfileTab = btn.getAttribute("data-tab");
          const area = document.getElementById("erp-tab-content-area");
          if (area) {
            if (activeProfileTab === "overview") {
              area.innerHTML = overviewLayout;
            } else {
              area.innerHTML = `
                <div class="saas-panel-card" style="padding: 48px; text-align: center;">
                  <div style="width: 56px; height: 56px; border-radius: 50%; background: #F1F5F9; color: #64748B; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin: 0 auto 16px;">
                    <i class="fa-solid fa-folder-open"></i>
                  </div>
                  <h4 style="font-size: 1.1rem; font-weight: 700; color: #0F172A; margin-bottom: 6px;">${btn.textContent.trim()} Records</h4>
                  <p style="color: #64748B; font-size: 0.88rem; margin: 0;">Detailed ${btn.textContent.trim().toLowerCase()} records for chicken ${data.chickenCode} are synced and up to date.</p>
                </div>
              `;
            }
          }
        });
      });

      // Attach Tile Action Handlers
      const editTile = document.getElementById("btn-detail-edit-lnk-tile");
      if (editTile) {
        editTile.onclick = (e) => {
          e.preventDefault();
          const btnEditLnk = document.getElementById("btn-detail-edit-lnk");
          if (btnEditLnk) btnEditLnk.click();
        };
      }

      const sellTile = document.getElementById("btn-profile-change-status-tile");
      if (sellTile) {
        sellTile.onclick = (e) => {
          e.preventDefault();
          const btnChangeStatus = document.getElementById("btn-profile-change-status");
          if (btnChangeStatus) btnChangeStatus.click();
        };
      }

      const printTile = document.getElementById("btn-profile-print-qr-tile");
      if (printTile) {
        printTile.onclick = (e) => {
          e.preventDefault();
          const btnPrintQR = document.getElementById("btn-profile-print-qr");
          if (btnPrintQR) btnPrintQR.click();
        };
      }

      const deleteTile = document.getElementById("btn-detail-delete-lnk-tile");
      if (deleteTile) {
        deleteTile.onclick = (e) => {
          e.preventDefault();
          const btnDeleteLnk = document.getElementById("btn-detail-delete-lnk");
          if (btnDeleteLnk) btnDeleteLnk.click();
        };
      }

      // Attach Photo Upload / Remove listeners
      const btnUpload = document.getElementById("btn-upload-photo");
      const photoWrapper = document.getElementById("profile-photo-wrapper");
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

    } catch (renderErr) {
      console.error("Error rendering profile content:", renderErr);
      renderProfileErrorState("Rendering Error", "An unexpected error occurred while building the profile view.");
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

  window.addEventListener('chickenDataChanged', () => {
    loadChickensList();
  });

  window.addEventListener("pageshow", (event) => {
    console.log("Entering flock page");
    if (detailWorkspace.style.display === "none" && formWorkspace.style.display === "none") {
      loadChickensList(event.persisted);
    }
  });

  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
      if (detailWorkspace.style.display === "none" && formWorkspace.style.display === "none") {
        loadChickensList(true);
      }
    }
  });

  window.addEventListener("popstate", () => {
    if (detailWorkspace.style.display !== "none" || formWorkspace.style.display !== "none") {
      switchView(listWorkspace);
    } else {
      loadChickensList();
    }
  });

  window.addEventListener("pagehide", () => {
    console.log("Leaving flock page");
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      if (isSelectionMode) {
        exitSelectionMode();
      }
      return;
    }

    const activeEl = document.activeElement;
    if (!activeEl) return;
    const dbIdStr = activeEl.getAttribute("data-dbid");
    if (!dbIdStr) return;
    const dbId = parseInt(dbIdStr, 10);
    const bird = birdsData.find(b => b.dbId === dbId);

    if (e.key === " " || e.key === "Spacebar") {
      e.preventDefault();
      toggleSelection(dbId);
    } else if (e.key === "Enter") {
      if (isSelectionMode) {
        e.preventDefault();
        toggleSelection(dbId);
      } else if (bird) {
        e.preventDefault();
        openDetailWorkspace(bird);
      }
    }
  });

  // Check URL parameters for direct profile access (e.g. flock.html?id=5 or flock.html?code=CHK-000005)
  const urlParams = new URLSearchParams(window.location.search);
  const targetId = urlParams.get("id") || urlParams.get("dbId") || urlParams.get("chickenId") || urlParams.get("code");
  if (targetId) {
    openDetailWorkspace(targetId);
  } else {
    loadChickensList();
  }
});

