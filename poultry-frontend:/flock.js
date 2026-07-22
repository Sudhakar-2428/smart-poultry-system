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

  async function loadChickensList() {
    showListLoadingState();
    try {
      let queryParams = [`page=${searchPage}`, `size=${searchPageSize}`, `sort=id,desc`];
      if (searchQuery) {
        if (/^[A-Za-z0-9\-]+$/.test(searchQuery)) queryParams.push(`chickenCode=${encodeURIComponent(searchQuery)}`);
        else queryParams.push(`name=${encodeURIComponent(searchQuery)}`);
      }
      if (activeQuickFilter !== "all") {
        if (activeQuickFilter === "Hen") queryParams.push(`gender=FEMALE`);
        else if (activeQuickFilter === "Rooster") queryParams.push(`gender=MALE`);
        else if (activeQuickFilter === "Chick") queryParams.push(`category=CHICK`);
        else if (activeQuickFilter === "Ready for Sale") queryParams.push(`status=ACTIVE`);
      }
      if (advGender.value) {
        queryParams.push(`gender=${advGender.value === "Hen" ? "FEMALE" : "MALE"}`);
      }
      if (advBreed.value) queryParams.push(`breed=${BREEDS[advBreed.value] || "OTHER"}`);
      if (advCategory.value) queryParams.push(`category=${CATS[advCategory.value] || "OTHER"}`);
      if (advStatus.value) queryParams.push(`status=${STATUS[advStatus.value] || "ACTIVE"}`);
      if (advAgeGroup.value) {
        if (advAgeGroup.value === "chick") queryParams.push(`maxAgeDays=60`);
        else if (advAgeGroup.value === "grower") queryParams.push(`minAgeDays=61`, `maxAgeDays=150`);
        else if (advAgeGroup.value === "adult") queryParams.push(`minAgeDays=151`);
      }

      const response = await Api.get(`chickens?${queryParams.join('&')}`);
      if (response && response.success && response.data) {
        const pageData = response.data;
        birdsData = pageData.content.map(item => {
          let origin = "Farm Born", notes = item.remarks || "";
          if (item.remarks && item.remarks.startsWith("[Origin: ")) {
            const match = item.remarks.match(/^\[Origin:\s*([^\]]+)\]\s*(.*)$/);
            if (match) { origin = match[1]; notes = match[2]; }
          }
          let ageText = "N/A";
          if (item.ageInDays !== null) {
            if (item.ageInDays < 60) ageText = `${item.ageInDays} Days`;
            else if (item.ageInMonths !== null) ageText = `${item.ageInMonths} Months`;
          }
          return {
            id: item.chickenCode, dbId: item.id, name: item.name || "Unnamed",
            gender: item.gender === "MALE" ? "Rooster" : "Hen",
            category: CATS_REV[item.category] || "Other", breed: BREEDS_REV[item.breed] || "Other",
            dob: item.dateOfBirth, weight: item.weight || 0.0, health: "Healthy",
            status: STATUS_REV[item.status] || "Laying", source: origin, band: item.color || "None",
            coop: "Coop A - Laying Cage", notes, ageText
          };
        });

        searchTotalPages = pageData.totalPages || 1;
        let dashTotal = 0, dashActive = 0;
        try {
          const dashRes = await Api.get("reports/dashboard");
          if (dashRes && dashRes.success) {
            dashTotal = dashRes.data.totalChickens;
            dashActive = dashRes.data.activeChickens;
          }
        } catch (e) {
          dashTotal = pageData.totalElements;
          dashActive = pageData.totalElements;
        }
        if (cardTotal) cardTotal.textContent = dashTotal;
        if (cardHealthy) cardHealthy.textContent = dashActive;
        if (cardHens) cardHens.textContent = dashActive;
        if (cardRoosters) cardRoosters.textContent = (dashTotal - dashActive);

        renderListLayoutsFromData();
        renderPaginationControls(pageData);
      }
    } catch (e) {
      console.error(e);
      tableBody.innerHTML = `<tr><td colspan="10" style="text-align: center; color: var(--neutral-gray); padding: 40px 0;"><i class="fa-solid fa-triangle-exclamation" style="font-size: 1.5rem; color: #D32F2F;"></i><p style="margin-top: 8px;">Failed to load data: ${e.message || e}</p></td></tr>`;
    }
  }

  function renderListLayoutsFromData() {
    if (birdsData.length === 0) {
      gridDeck.style.display = "none";
      listTableDeck.style.display = "none";
      emptyStateBlock.style.display = "block";
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
      let emoji = b.gender === "Rooster" ? "🐓" : (b.category === "Chick" ? "🐥" : "🐔");
      let healthVal = "healthy", statusVal = b.status.toLowerCase().replace(/\s+/g, '-');
      card.innerHTML = `
        <div class="flock-card-photo-box">${emoji}</div>
        <div class="flock-card-info">
          <div class="flock-card-header-row"><h4>${b.name}</h4><span class="chk-id-badge">${b.id}</span></div>
          <span class="flock-card-subspec">Category: <strong>${b.category}</strong></span>
          <span class="flock-card-subspec">Breed: ${b.breed}</span>
          <span class="flock-card-subspec">Gender: ${b.gender}</span>
          <span class="flock-card-subspec">Age: ${b.ageText || 'N/A'}</span>
          <span class="flock-card-subspec">Origin: ${b.source}</span>
          <div class="flock-card-status-badges" style="margin-top: 8px;"><span class="chk-status-pill ${healthVal}">${b.health}</span><span class="chk-status-pill ${statusVal}">${b.status}</span></div>
          <div style="display:flex; gap:6px; margin-top:14px; width:100%;">
            <button class="btn btn-outline btn-view-bio" style="flex:1; padding:6px; font-size:0.75rem;"><i class="fa-solid fa-eye"></i> View</button>
            <button class="btn btn-outline btn-edit-bio" style="flex:1; padding:6px; font-size:0.75rem;"><i class="fa-solid fa-pen-to-square"></i> Edit</button>
            <button class="btn btn-outline btn-delete-bio" style="padding:6px; font-size:0.75rem; border-color: rgba(220,38,38,0.3); color:#DC2626;"><i class="fa-solid fa-trash-can"></i></button>
          </div>
          ${b.gender === 'Hen' && b.status !== 'Egg Laying' && b.status !== 'Sold' && b.status !== 'Dead' && b.status !== 'Removed from Farm' ? `<div style="margin-top: 10px; width:100%;"><button class="btn btn-primary btn-start-laying" style="width: 100%; padding: 8px; font-size: 0.82rem; height: 38px; display: flex; align-items: center; justify-content: center; gap: 6px;"><i class="fa-solid fa-play"></i> Start Egg Laying</button></div>` : ''}
        </div>
      `;
      card.querySelector(".btn-view-bio").addEventListener("click", () => openDetailWorkspace(b));
      card.querySelector(".btn-edit-bio").addEventListener("click", () => openFormWorkspace(b));
      card.querySelector(".btn-delete-bio").addEventListener("click", () => deleteBird(b.dbId, b.id));
      const slBtn = card.querySelector(".btn-start-laying");
      if (slBtn) slBtn.addEventListener("click", (e) => { e.stopPropagation(); window.openStartLayingDrawer(b); });
      gridDeck.appendChild(card);
    });

    tableBody.innerHTML = "";
    birdsData.forEach(b => {
      const tr = document.createElement("tr");
      tr.className = "flock-table-row";
      let emoji = b.gender === "Rooster" ? "🐓" : (b.category === "Chick" ? "🐥" : "🐔");
      let healthVal = "healthy", statusVal = b.status.toLowerCase().replace(/\s+/g, '-');
      tr.innerHTML = `
        <td><span class="table-emoji-avatar">${emoji}</span></td>
        <td><strong class="text-green">${b.id}</strong></td>
        <td>${b.category}</td>
        <td>${b.breed}</td>
        <td>${b.gender}</td>
        <td>${b.ageText || 'N/A'}</td>
        <td><span class="chk-status-pill ${healthVal}">${b.health}</span></td>
        <td><span class="chk-status-pill ${statusVal}">${b.status}</span></td>
        <td>${b.source}</td>
        <td>
          <div class="table-actions-flex">
            <button class="action-mini-btn btn-view" title="View"><i class="fa-solid fa-eye"></i></button>
            <button class="action-mini-btn btn-edit" title="Edit"><i class="fa-solid fa-pen-to-square"></i></button>
            ${b.gender === 'Hen' && b.status !== 'Egg Laying' && b.status !== 'Sold' && b.status !== 'Dead' && b.status !== 'Removed from Farm' ? `<button class="action-mini-btn btn-start-laying-table" title="Start Egg Laying" style="color: var(--primary-green-dark);"><i class="fa-solid fa-egg"></i></button>` : ''}
            <button class="action-mini-btn btn-delete" style="color:#DC2626;" title="Delete"><i class="fa-solid fa-trash-can"></i></button>
          </div>
        </td>
      `;
      tr.querySelector(".btn-view").addEventListener("click", () => openDetailWorkspace(b));
      tr.querySelector(".btn-edit").addEventListener("click", () => openFormWorkspace(b));
      tr.querySelector(".btn-delete").addEventListener("click", () => deleteBird(b.dbId, b.id));
      const slBtnT = tr.querySelector(".btn-start-laying-table");
      if (slBtnT) slBtnT.addEventListener("click", (e) => { e.stopPropagation(); window.openStartLayingDrawer(b); });
      tableBody.appendChild(tr);
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

  async function openDetailWorkspace(bird) {
    showListLoadingState();
    try {
      const res = await Api.get(`chickens/${bird.dbId}`);
      if (res && res.success && res.data) {
        renderDetailedProfile(res.data);
      }
    } catch (e) {
      console.error(e);
    }
  }

  function renderDetailedProfile(data) {
    let origin = "Farm Born", notes = data.remarks || "";
    if (data.remarks && data.remarks.startsWith("[Origin: ")) {
      const match = data.remarks.match(/^\[Origin:\s*([^\]]+)\]\s*(.*)$/);
      if (match) { origin = match[1]; notes = match[2]; }
    }
    let ageText = "N/A";
    if (data.ageInDays !== null) {
      if (data.ageInDays < 60) ageText = `${data.ageInDays} Days`;
      else if (data.ageInMonths !== null) ageText = `${data.ageInMonths} Months`;
    }

    let emoji = data.gender === "MALE" ? "🐓" : (data.category === "CHICK" ? "🐥" : "🐔");
    let breedUI = BREEDS_REV[data.breed] || data.breed || "Other";
    let categoryUI = CATS_REV[data.category] || data.category || "Other";
    let genderUI = data.gender === "MALE" ? "Rooster" : "Hen";
    let statusUI = STATUS_REV[data.status] || "Laying";
    let healthBadgeClass = "healthy", classificationBadgeClass = statusUI.toLowerCase().replace(/\s+/g, '-');

    const layoutOutput = document.getElementById("profile-detailed-container");
    layoutOutput.innerHTML = `
      <div class="detail-main-content-card" style="display: flex; flex-direction: column; gap: 24px; flex: 1.6;">
        <div class="form-section-card" style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:8px; padding:24px;">
          <div class="card-title-row" style="display:flex; align-items:center; gap:8px; margin-bottom:16px; border-bottom:1px dashed var(--neutral-light-gray); padding-bottom:12px;"><i class="fa-solid fa-circle-info" style="color:var(--primary-green);"></i><h3 style="font-size:1rem; font-weight:700; margin:0;">Basic Information</h3></div>
          <table class="drawer-data-table">
            <tr><td><strong>Chicken Code</strong></td><td><strong>${data.chickenCode}</strong></td></tr>
            <tr><td><strong>Name</strong></td><td>${data.name || 'Unnamed'}</td></tr>
            <tr><td><strong>Leg Band / Color</strong></td><td>${data.color || 'None'}</td></tr>
            <tr><td><strong>Category</strong></td><td>${categoryUI}</td></tr>
            <tr><td><strong>Breed</strong></td><td>${breedUI}</td></tr>
            <tr><td><strong>Gender</strong></td><td>${genderUI}</td></tr>
            <tr><td><strong>Farm Origin</strong></td><td>${origin}</td></tr>
          </table>
        </div>
        <div class="form-section-card" style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:8px; padding:24px;">
          <div class="card-title-row" style="display:flex; align-items:center; gap:8px; margin-bottom:16px; border-bottom:1px dashed var(--neutral-light-gray); padding-bottom:12px;"><i class="fa-solid fa-calendar-days" style="color:var(--primary-green);"></i><h3 style="font-size:1rem; font-weight:700; margin:0;">Date Information</h3></div>
          <table class="drawer-data-table">
            <tr><td><strong>Date of Birth (DOB)</strong></td><td>${data.dateOfBirth || 'Unknown'}</td></tr>
            <tr><td><strong>Age</strong></td><td><strong>${ageText}</strong></td></tr>
            <tr><td><strong>Registered At</strong></td><td>${data.createdAt ? new Date(data.createdAt).toLocaleDateString() : 'N/A'}</td></tr>
          </table>
        </div>
        <div class="form-section-card" style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:8px; padding:24px;">
          <div class="card-title-row" style="display:flex; align-items:center; gap:8px; margin-bottom:16px; border-bottom:1px dashed var(--neutral-light-gray); padding-bottom:12px;"><i class="fa-solid fa-heart-pulse" style="color:var(--primary-green);"></i><h3 style="font-size:1rem; font-weight:700; margin:0;">Health & Comments</h3></div>
          <table class="drawer-data-table">
            <tr><td><strong>Current Weight</strong></td><td>${data.weight || 0} kg</td></tr>
            <tr><td><strong>Health Status</strong></td><td><span class="chk-status-pill ${healthBadgeClass}">Healthy</span></td></tr>
            <tr><td><strong>Status</strong></td><td><span class="chk-status-pill ${classificationBadgeClass}">${statusUI}</span></td></tr>
            <tr><td><strong>Remarks / Notes</strong></td><td>${notes || 'No remarks.'}</td></tr>
          </table>
        </div>
      </div>
      <div class="detail-side-sidebar-card" style="display: flex; flex-direction: column; gap: 24px; flex: 1;">
        <div class="form-section-card detail-profile-hero-card" style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:8px; padding:24px; text-align:center; display:flex; flex-direction:column; align-items:center;">
          <div style="width: 100px; height: 100px; border-radius: 50%; border: 1.5px solid var(--neutral-light-gray); display: flex; align-items: center; justify-content: center; font-size: 3.5rem; margin-bottom: 12px; background: #F8FAFC;">${emoji}</div>
          <h2 style="font-size: 1.3rem; margin:0;">${data.name || 'Unnamed'}</h2>
          <span style="font-size: 0.8rem; color: var(--neutral-gray); margin-top: 4px;">ID: <strong>${data.chickenCode}</strong></span>
          <div style="margin-top: 14px; display:flex; gap:6px; justify-content:center;"><span class="chk-status-pill ${healthBadgeClass}">Healthy</span><span class="chk-status-pill ${classificationBadgeClass}">${statusUI}</span></div>
        </div>
        <div class="form-section-card" style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:8px; padding:20px; display:flex; flex-direction:column; gap:10px;">
          <button class="btn btn-outline" id="btn-detail-edit-action" style="width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px;"><i class="fa-solid fa-pen-to-square"></i> Edit Record</button>
          <button class="btn btn-outline" id="btn-detail-delete-action" style="width: 100%; color: #DC2626; border-color: rgba(220,38,38,0.2); display: flex; align-items: center; justify-content: center; gap: 8px;"><i class="fa-solid fa-trash-can"></i> Delete Chicken</button>
        </div>
      </div>
    `;

    document.getElementById("btn-detail-edit-action").onclick = () => {
      const bird = {
        id: data.chickenCode, dbId: data.id, name: data.name,
        gender: data.gender === "MALE" ? "Rooster" : "Hen",
        category: CATS_REV[data.category] || "Other", breed: BREEDS_REV[data.breed] || "Other",
        dob: data.dateOfBirth, weight: data.weight || 0.0, health: "Healthy",
        status: STATUS_REV[data.status] || "Laying", source: origin, band: data.color || "",
        notes: notes, ageText
      };
      openFormWorkspace(bird);
    };

    document.getElementById("btn-detail-delete-action").onclick = () => {
      if (confirm(`Are you sure you want to permanently delete chicken ${data.chickenCode}?`)) {
        Api.delete(`chickens/${data.id}`)
          .then(() => {
            showSuccessToast(`Chicken ${data.chickenCode} deleted successfully.`);
            switchView(listWorkspace);
            loadChickensList();
          })
          .catch(e => console.error(e));
      }
    };

    const btnEditLnk = document.getElementById("btn-detail-edit-lnk");
    if (btnEditLnk) btnEditLnk.onclick = () => document.getElementById("btn-detail-edit-action").click();

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
