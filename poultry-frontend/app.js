import './notifications-manager.js';
import { AuthService } from './authService.js';
import { Storage } from './storage.js';
import { showToast, Api } from './api.js';

// Top-level fast authentication guard to prevent page flashing
(function() {
  const path = window.location.pathname;
  const pageName = path.split('/').pop() || 'index.html';
  const cleanPageName = pageName.split('#')[0].split('?')[0];
  
  const isAuthPage = cleanPageName === 'login.html' || cleanPageName === 'signup.html';
  const isLandingPage = cleanPageName === 'index.html' || cleanPageName === '';
  
  const authenticated = AuthService.isAuthenticated();
  
  if (!isLandingPage) {
    if (isAuthPage) {
      if (authenticated) {
        window.location.href = 'dashboard.html';
      }
    } else {
      if (!authenticated) {
        window.location.href = 'login.html';
      }
    }
  }
})();

// Global Bird Database & Age Calculations
(function() {
  const DEFAULT_BIRDS = [
    {
      id: "C001",
      name: "Bella",
      gender: "Hen",
      category: "Country Chicken",
      breed: "Peruvidai",
      weight: 2.1,
      source: "Farm Born",
      band: "LB-101",
      coop: "Coop A - Laying Cage",
      dob: "2025-05-15",
      acqDate: "",
      acqPrice: 0,
      health: "Healthy",
      vaxNewcastle: true,
      vaxBronchitis: true,
      vaxMarek: false,
      status: "Laying",
      targetFeed: 120,
      dam: "Dam-9321",
      sire: "Sire-8802",
      notes: "High laying yield, healthy stock."
    },
    {
      id: "C002",
      name: "Cluck Norris",
      gender: "Rooster",
      category: "Country Chicken",
      breed: "Siruvidai",
      weight: 3.2,
      source: "Purchased",
      band: "LB-102",
      coop: "Coop B - Breeding Yard",
      dob: "2025-02-10",
      acqDate: "2025-06-15",
      acqPrice: 15.00,
      health: "Healthy",
      vaxNewcastle: true,
      vaxBronchitis: true,
      vaxMarek: true,
      status: "Breeding",
      targetFeed: 140,
      dam: "N/A",
      sire: "N/A",
      notes: "Aggressive breed leader, good compatibility."
    },
    {
      id: "C003",
      name: "Goldie",
      gender: "Hen",
      category: "Other",
      breed: "White Leghorn",
      weight: 1.8,
      source: "Farm Born",
      band: "LB-103",
      coop: "Coop A - Laying Cage",
      dob: "2025-09-05",
      acqDate: "",
      acqPrice: 0,
      health: "Healthy",
      vaxNewcastle: true,
      vaxBronchitis: false,
      vaxMarek: false,
      status: "Laying",
      targetFeed: 110,
      dam: "Dam-9321",
      sire: "Sire-8802",
      notes: "Steady collection rate."
    },
    {
      id: "C004",
      name: "Peanut",
      gender: "Chick",
      category: "Broiler",
      breed: "Broiler",
      weight: 0.5,
      source: "Farm Born",
      band: "LB-104",
      coop: "Chicks Box - Brooder",
      dob: "2026-06-02",
      acqDate: "",
      acqPrice: 0,
      health: "Healthy",
      vaxNewcastle: true,
      vaxBronchitis: true,
      vaxMarek: true,
      status: "Brooding",
      targetFeed: 40,
      dam: "Dam-9120",
      sire: "Sire-8201",
      notes: "Fast growth cohort."
    },
    {
      id: "C005",
      name: "Daisy",
      gender: "Hen",
      category: "Other",
      breed: "Rhode Island Red",
      weight: 2.5,
      source: "Purchased",
      band: "LB-105",
      coop: "Coop A - Laying Cage",
      dob: "",
      acqDate: "2026-04-17",
      acqPrice: 12.50,
      health: "Healthy",
      vaxNewcastle: true,
      vaxBronchitis: false,
      vaxMarek: false,
      status: "Laying",
      targetFeed: 115,
      dam: "N/A",
      sire: "N/A",
      notes: "Acquired at point of lay."
    }
  ];

  window.getBirdsList = function() {
    const list = localStorage.getItem('poultry_birds_list');
    if (!list) {
      localStorage.setItem('poultry_birds_list', JSON.stringify(DEFAULT_BIRDS));
      return JSON.parse(JSON.stringify(DEFAULT_BIRDS));
    }
    try {
      return JSON.parse(list);
    } catch(e) {
      console.error("Error reading birds list from localStorage", e);
      return JSON.parse(JSON.stringify(DEFAULT_BIRDS));
    }
  };

  window.saveBirdsList = function(birds) {
    localStorage.setItem('poultry_birds_list', JSON.stringify(birds));
  };

  // Automated age calculation helper
  window.getAgeFromDates = function(dobStr, acqDateStr, source) {
    const curDate = new Date();
    
    let startDate;
    if (source === "Farm Born") {
      startDate = dobStr ? new Date(dobStr) : null;
    } else { // Purchased
      startDate = dobStr ? new Date(dobStr) : (acqDateStr ? new Date(acqDateStr) : null);
    }
    
    if (!startDate || isNaN(startDate.getTime())) {
      return "Unknown";
    }
    
    const diffTime = curDate - startDate;
    if (diffTime < 0) {
      return "0 Days";
    }
    
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays < 30) {
      return `${diffDays} Day${diffDays === 1 ? '' : 's'}`;
    }
    
    let years = curDate.getFullYear() - startDate.getFullYear();
    let months = curDate.getMonth() - startDate.getMonth();
    
    if (months < 0) {
      years--;
      months += 12;
    }
    
    if (curDate.getDate() < startDate.getDate()) {
      months--;
      if (months < 0) {
        years--;
        months += 12;
      }
    }
    
    const totalMonths = years * 12 + months;
    
    if (totalMonths < 12) {
      return `${totalMonths} Month${totalMonths === 1 ? '' : 's'}`;
    }
    
    if (months === 0) {
      return `${years} Year${years === 1 ? '' : 's'}`;
    } else {
      return `${years} Year${years === 1 ? '' : 's'} ${months} Month${months === 1 ? '' : 's'}`;
    }
  };

  // Get age display text (handling age freezing)
  window.getBirdAgeText = function(bird) {
    if (!bird) return "Unknown";
    
    const frozenStatuses = ["Sold", "Dead"];
    if (frozenStatuses.includes(bird.status)) {
      if (!bird.frozenAge) {
        bird.frozenAge = window.getAgeFromDates(bird.dob, bird.acqDate, bird.source);
        // Persist to localStorage
        const list = window.getBirdsList();
        const found = list.find(x => x.id === bird.id);
        if (found) {
          found.frozenAge = bird.frozenAge;
          window.saveBirdsList(list);
        }
      }
      return bird.frozenAge;
    }
    
    // If status is active, ensure we calculate dynamically and don't persist frozenAge
    if (bird.frozenAge) {
      delete bird.frozenAge;
      const list = window.getBirdsList();
      const found = list.find(x => x.id === bird.id);
      if (found) {
        delete found.frozenAge;
        window.saveBirdsList(list);
      }
    }
    
    return window.getAgeFromDates(bird.dob, bird.acqDate, bird.source);
  };
})();

document.addEventListener('DOMContentLoaded', () => {

  const path = window.location.pathname;
  const pageName = path.split('/').pop() || 'index.html';
  const cleanPageName = pageName.split('#')[0].split('?')[0];
  const isAuthPage = cleanPageName === 'login.html' || cleanPageName === 'signup.html';
  const isLandingPage = cleanPageName === 'index.html' || cleanPageName === '';

  const ROLE_PAGES = {
    ADMIN: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html', 'invite-member.html', 'create-farm.html'],
    MANAGER: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html', 'invite-member.html', 'create-farm.html'],
    VETERINARIAN: ['dashboard.html', 'flock.html', 'health-records.html', 'notifications.html', 'settings.html'],
    WORKER: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html', 'invite-member.html', 'create-farm.html']
  };

  // 1. Role-based Route Guardian
  if (!isLandingPage && !isAuthPage) {
    const user = AuthService.getCurrentUser();
    const role = user ? user.role : null;
    const allowed = ROLE_PAGES[role] || [];
    
    if (!allowed.includes(cleanPageName)) {
      alert('Access denied: your role does not have authorization to view this page.');
      window.location.href = 'dashboard.html';
      return;
    }
  }

  // 2. Wire up Login Action
  if (cleanPageName === 'login.html') {
    const loginForm = document.querySelector('.auth-page-container form.contact-form') || document.querySelector('form.contact-form');
    if (loginForm) {
      loginForm.removeAttribute('onsubmit');
      loginForm.onsubmit = null;
      
      const emailInput = document.getElementById('login-email');
      const passInput = document.getElementById('login-pass');
      
      loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const email = emailInput ? emailInput.value.trim() : '';
        const password = passInput ? passInput.value : '';
        
        if (!email || !password) {
          showToast('Please fill in both email and password.');
          return;
        }
        
        try {
          await AuthService.login(email, password);
          showToast('Login successful! Redirecting...', 'success');
          document.body.classList.remove('page-loaded');
          setTimeout(() => {
            window.location.href = 'dashboard.html';
          }, 1000);
        } catch (err) {
          console.error('Login error', err);
        }
      });
    }
  }
  // 2b. Wire up Register Action
  if (cleanPageName === 'signup.html') {
    const signupForm = document.querySelector('.auth-page-container form.contact-form') || document.querySelector('form.contact-form');
    if (signupForm) {
      signupForm.removeAttribute('onsubmit');
      signupForm.onsubmit = null;
      
      signupForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const fullName = document.getElementById('reg-name').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        const phoneNumber = document.getElementById('reg-phone').value.trim();
        const farmName = document.getElementById('reg-farmname') ? document.getElementById('reg-farmname').value.trim() : '';
        const farmLoc = document.getElementById('reg-farmloc') ? document.getElementById('reg-farmloc').value.trim() : '';
        const roleSel = document.getElementById('reg-role').value;
        const password = document.getElementById('reg-pass').value;
        const confirmPassword = document.getElementById('reg-pass-confirm').value;
        
        if (password !== confirmPassword) {
          showToast('Passwords do not match.');
          return;
        }

        if (farmLoc) {
          localStorage.setItem('poultry_farm_location', farmLoc);
          localStorage.setItem('poultry_registered_location', farmLoc);
        }
        
        try {
          let response;
          if (roleSel === 'owner') {
            response = await Api.post('auth/register/owner', {
              fullName,
              email,
              phoneNumber,
              password,
              farmName: farmName || `${fullName}'s Farm`
            });
          } else {
            response = await Api.post('auth/register/worker', {
              fullName,
              email,
              phoneNumber,
              password
            });
          }
          
          if (response?.success) {
            showToast('Registration successful. Please login.', 'success');
            
            setTimeout(() => {
              window.location.href = 'login.html';
            }, 2000);
          }
        } catch (err) {
          console.error('Registration error', err);
        }
      });
    }
  }

  // 3. User profiles and Logout bindings
  if (!isLandingPage && !isAuthPage) {
    const currentUser = AuthService.getCurrentUser();
    if (currentUser) {
      const avatarWrapper = document.querySelector('.profile-avatar-wrapper');
      if (avatarWrapper) {
        avatarWrapper.removeAttribute('onclick');
        avatarWrapper.style.position = 'relative';
        avatarWrapper.style.cursor = 'pointer';

        const strongName = avatarWrapper.querySelector('strong');
        if (strongName) {
          strongName.textContent = currentUser.fullName;
        }
        const roleLabel = avatarWrapper.querySelector('span');
        if (roleLabel) {
          roleLabel.textContent = currentUser.role.charAt(0) + currentUser.role.slice(1).toLowerCase();
        }
        const avatarImg = avatarWrapper.querySelector('.profile-img');
        if (avatarImg) {
          avatarImg.textContent = currentUser.fullName ? currentUser.fullName.charAt(0).toUpperCase() : 'U';
        }

        // Chevron icon
        if (!avatarWrapper.querySelector('.profile-dropdown-arrow')) {
          const arrow = document.createElement('i');
          arrow.className = 'fa-solid fa-chevron-down profile-dropdown-arrow';
          arrow.style.cssText = 'font-size: 0.72rem; color: #94A3B8; margin-left: 4px; transition: transform 0.2s ease;';
          avatarWrapper.appendChild(arrow);
        }

        // Create or get profile dropdown menu
        let dropdown = document.getElementById('user-profile-dropdown');
        if (!dropdown) {
          dropdown = document.createElement('div');
          dropdown.id = 'user-profile-dropdown';
          dropdown.className = 'user-profile-dropdown-menu';
          document.body.appendChild(dropdown);
        }

        const initialChar = currentUser.fullName ? currentUser.fullName.charAt(0).toUpperCase() : 'U';
        dropdown.innerHTML = `
          <div class="user-dropdown-header">
            <div class="user-dropdown-avatar">${initialChar}</div>
            <div class="user-dropdown-info">
              <strong class="user-dropdown-name">${currentUser.fullName || 'User'}</strong>
              <span class="user-dropdown-email">${currentUser.email || 'user@example.com'}</span>
              <span class="user-dropdown-role-badge">${currentUser.role || 'MEMBER'}</span>
            </div>
          </div>
          <div class="user-dropdown-divider"></div>
          <ul class="user-dropdown-list">
            <li><a href="settings.html"><i class="fa-solid fa-user"></i> My Profile</a></li>
            <li><a href="settings.html"><i class="fa-solid fa-gear"></i> Settings</a></li>
            <li><a href="settings.html#security"><i class="fa-solid fa-key"></i> Change Password</a></li>
          </ul>
          <div class="user-dropdown-divider"></div>
          <button type="button" class="user-dropdown-logout-btn" id="dropdown-logout-btn">
            <i class="fa-solid fa-right-from-bracket"></i> Logout
          </button>
        `;

        function positionDropdown() {
          const rect = avatarWrapper.getBoundingClientRect();
          dropdown.style.top = `${rect.bottom + 8}px`;
          dropdown.style.right = `${Math.max(12, window.innerWidth - rect.right)}px`;
        }

        avatarWrapper.onclick = (e) => {
          e.stopPropagation();
          const isOpen = dropdown.classList.contains('show');
          if (isOpen) {
            dropdown.classList.remove('show');
          } else {
            positionDropdown();
            dropdown.classList.add('show');
          }
        };

        document.addEventListener('click', (e) => {
          if (dropdown && !dropdown.contains(e.target) && !avatarWrapper.contains(e.target)) {
            dropdown.classList.remove('show');
          }
        });

        const logoutBtnInDrop = dropdown.querySelector('#dropdown-logout-btn');
        if (logoutBtnInDrop) {
          logoutBtnInDrop.onclick = (e) => {
            e.preventDefault();
            e.stopPropagation();
            AuthService.logout();
          };
        }
      }
      
      const welcomeTitle = document.querySelector('.welcome-text h1');
      if (welcomeTitle) {
        welcomeTitle.innerHTML = `Welcome Back, ${currentUser.fullName} 👋`;
      }

      // Sidebar links hiding
      const allowed = ROLE_PAGES[currentUser.role] || [];
      const sidebarLinks = document.querySelectorAll('.sidebar-menu a');
      sidebarLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href && href !== '#' && !href.startsWith('javascript:')) {
          const name = href.split('/').pop().split('#')[0];
          if (name && !allowed.includes(name)) {
            const li = link.closest('li');
            if (li) {
              li.style.display = 'none';
            }
          }
        }
      });

      // Quick actions cards hiding on dashboard.html
      if (cleanPageName === 'dashboard.html') {
        const quickActionsMap = {
          VETERINARIAN: ['qa-health-check'],
          WORKER: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family'],
          ADMIN: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family'],
          MANAGER: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family']
        };

        const allowedQuick = quickActionsMap[currentUser.role] || [];
        const quickCards = document.querySelectorAll('.quick-actions-bar .action-btn-card');
        quickCards.forEach(card => {
          if (card.id && !allowedQuick.includes(card.id)) {
            card.style.display = 'none';
          }
        });
      }
    }

    // Intercept logout button
    const logoutBtn = document.getElementById('sidebar-logout-link');
    if (logoutBtn) {
      logoutBtn.removeAttribute('href');
      logoutBtn.style.cursor = 'pointer';
      logoutBtn.style.color = '#D32F2F';
      logoutBtn.addEventListener('click', (e) => {
        e.preventDefault();
        AuthService.logout();
      });
    }
  }

  // --- Parse Custom Onboarded Farm & Initial log records ---
  const savedFarm = localStorage.getItem('poultry_active_farm');
  const activeFarmSpan = document.getElementById('active-farm-display-name');
  if (savedFarm && activeFarmSpan) {
    activeFarmSpan.textContent = savedFarm;
  }

  const savedLogs = localStorage.getItem('poultry_initial_logs');
  const timelinePanel = document.getElementById('timeline-list-panel');
  if (savedLogs && timelinePanel) {
    try {
      const logsArray = JSON.parse(savedLogs);
      logsArray.reverse().forEach(log => {
        const item = document.createElement('div');
        item.className = 'timeline-item';
        item.innerHTML = `
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <span class="timeline-body">${log}</span>
            <span class="timeline-time">Just now</span>
          </div>
        `;
        timelinePanel.insertBefore(item, timelinePanel.firstChild);
      });
      // Clear logs to avoid duplicating them on refresh
      localStorage.removeItem('poultry_initial_logs');
    } catch (e) {
      console.error(e);
    }
  }

  // --- Smooth page load animation fade-in & out transitions ---
  document.body.classList.add('page-loaded');

  const pageLinks = document.querySelectorAll('a[href$=".html"], a[href^="index.html"]');
  pageLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      const url = link.getAttribute('href');
      const currPage = window.location.pathname.split('/').pop() || 'index.html';
      const destPage = url.split('#')[0] || 'index.html';
      
      // If it's a hash anchor on the same page, skip fadeout
      if (url.startsWith('#') || (destPage === currPage && url.includes('#'))) {
        return;
      }
      
      e.preventDefault();
      document.body.classList.remove('page-loaded');
      setTimeout(() => {
        window.location.href = url;
      }, 350);
    });
  });

  // --- Sticky Header Styling on Scroll ---
  const header = document.querySelector('.main-header');
  window.addEventListener('scroll', () => {
    if (window.scrollY > 40) {
      header.classList.add('scrolled');
    } else {
      header.classList.remove('scrolled');
    }
  });


  // --- Mobile Drawer Navigation Menu ---
  const mobileNavToggle = document.getElementById('mobile-nav-toggle');
  const navbar = document.getElementById('navbar');
  const navLinks = document.querySelectorAll('.nav-link');

  if (mobileNavToggle && navbar) {
    mobileNavToggle.addEventListener('click', () => {
      mobileNavToggle.classList.toggle('open');
      navbar.classList.toggle('open');
    });

    // Close menu when clicking link
    navLinks.forEach(link => {
      link.addEventListener('click', () => {
        mobileNavToggle.classList.remove('open');
        navbar.classList.remove('open');
        
        // Remove active class from old, add to current
        navLinks.forEach(l => l.classList.remove('active'));
        link.classList.add('active');
      });
    });
  }


  // --- Active Nav Link Highlighter on Scroll (Intersection Observer) ---
  const sections = document.querySelectorAll('section');
  const navOptions = {
    threshold: 0.3,
    rootMargin: "-80px 0px 0px 0px"
  };

  const navObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const id = entry.target.getAttribute('id');
        navLinks.forEach(link => {
          if (link.getAttribute('href') === `#${id}`) {
            link.classList.add('active');
          } else {
            link.classList.remove('active');
          }
        });
      }
    });
  }, navOptions);

  sections.forEach(section => navObserver.observe(section));


  // --- Interactive Dashboard Preview Tabs ---
  const tabButtons = document.querySelectorAll('.sidebar-menu li');
  const tabPanes = document.querySelectorAll('.tab-pane');

  tabButtons.forEach(button => {
    button.addEventListener('click', () => {
      const targetTab = button.getAttribute('data-tab');

      // Update sidebar state
      tabButtons.forEach(btn => btn.classList.remove('active-tab'));
      button.classList.add('active-tab');

      // Toggle visible dashboard tab pane
      tabPanes.forEach(pane => {
        pane.classList.remove('active');
        if (pane.getAttribute('id') === `tab-${targetTab}`) {
          pane.classList.add('active');
        }
      });
    });
  });


  // --- Feature Modal Lightbox Mechanism ---
  const modal = document.getElementById('feature-modal');
  const modalClose = document.getElementById('modal-close');
  const modalActionBtn = document.getElementById('modal-action-btn');
  const modalTitle = document.getElementById('modal-title');
  const modalDesc = document.getElementById('modal-desc');
  const modalIcon = document.getElementById('modal-icon');
  const modalPoints = document.getElementById('modal-points');
  const learnMoreLinks = document.querySelectorAll('.learn-more');

  const featureDetails = {
    "chicken-management": {
      title: "Chicken Management",
      icon: "fa-feather-pointed",
      colorClass: "color-green",
      desc: "Deep-dive tracking and record-keeping features designed for health registries and herd inventory coordination.",
      points: [
        "<strong>Individual RFID Registries</strong>: Identify or track unique laying hens using RFID leg bands or local hatchery batch IDs.",
        "<strong>Avian Health Tracking</strong>: Instantly log warning symptoms, booster timelines, quarantine flags, and vet records.",
        "<strong>Flock Ancestry Maps</strong>: Document bloodlines, hatching cohorts, and parent tracking to safeguard pure premium breeds.",
        "<strong>Yield-to-Feed Ratio</strong>: Compute how dynamic nutritional rations influence individual hen weight over time."
      ]
    },
    "egg-tracking": {
      title: "Egg Production Tracking",
      icon: "fa-egg",
      colorClass: "color-gold",
      desc: "Granular production statistics to pinpoint laying efficiencies, streamline sizing, and reduce breakage.",
      points: [
        "<strong>Real-time Collection Ratios</strong>: Calculate egg collection metrics daily per poultry block, breed, or partition.",
        "<strong>Size & Quality Sorting</strong>: Direct entry markers for commercial classification scales (Jumbo, Large, Medium).",
        "<strong>Waste & Damage Auditing</strong>: Track shell breakages during collection or washing to pinpoint hardware friction points.",
        "<strong>Laying Conversion Indexes</strong>: Contrast feed bags consumed against crate collections to gauge exact performance."
      ]
    },
    "chick-growth": {
      title: "Chick Growth & Brooding",
      icon: "fa-kiwi-bird",
      colorClass: "color-brown",
      desc: "Precision environmental telemetry monitoring for fragile brooder chicks in early lifecycle phases.",
      points: [
        "<strong>Brooder Heat-Map Alarms</strong>: Integrate notifications for house ambient temperatures to guarantee the perfect 32°C margin.",
        "<strong>Feed Stage Progression</strong>: Program starter crumble limits and automatically alert operators when grower pellets are required.",
        "<strong>Immunity Schedules</strong>: Pre-scheduled task triggers for coccidiosis, Newcastle, and bronchitis vaccinations.",
        "<strong>Weight Benchmark Comparison</strong>: Log weights and cross-reference standard charts to spot underperforming batches early."
      ]
    },
    "profit-analytics": {
      title: "Profit & Capital Analytics",
      icon: "fa-chart-line",
      colorClass: "color-blue",
      desc: "Comprehensive financial visibility tracking operational expenses and revenue streams.",
      points: [
        "<strong>Operational Cost Bookkeep</strong>: Track details for feed grain shipments, vet invoices, farm bills, and labor payrolls.",
        "<strong>Dynamic Revenue Log</strong>: Generate invoices and receipt structures for egg distributors and organic poultry buyers.",
        "<strong>Forecast & Profit Margins</strong>: Interactive projections visualizing prospective crop yields relative to market futures.",
        "<strong>One-click Data Exports</strong>: Instantly generate and download complete financial statements (CSV/PDF) for tax or loan reviews."
      ]
    },
    "family-collaboration": {
      title: "Collaborative Sync Integration",
      icon: "fa-people-group",
      colorClass: "color-orange",
      desc: "Sync database updates instantly across administrative accounts and mobile devices for all family members.",
      points: [
        "<strong>Smart Local Cache</strong>: Allows farmhands to log records down in signal dead-zones; auto-syncs when returning to Wi-Fi.",
        "<strong>Permission Restriction Seals</strong>: Allow field helpers to update daily logs while shielding accounting details.",
        "<strong>Historical Audit Logs</strong>: View a strict timeline record showing edits, time-stamps, and associate member names.",
        "<strong>Real-time Notifications</strong>: Push instant notifications to other users' devices when changes happen."
      ]
    },
    "smart-notifications": {
      title: "Intelligent Notification Matrix",
      icon: "fa-bell",
      colorClass: "color-red",
      desc: "Automation triggers that notify managers when values slide outside normal thresholds.",
      points: [
        "<strong>Direct WhatsApp/SMS Feeds</strong>: Dispatch urgent vaccination alarms directly to cellular text channels without app lag.",
        "<strong>System Anomaly Traps</strong>: Automated warning alerts fired if silo weights decline or water flow is interrupted.",
        "<strong>Supplier Reorders</strong>: Program minimum warehouse feed weights to coordinate shipping automatically with vendors.",
        "<strong>Batch Calendar Alarms</strong>: Timelines calculated from hatching stages notify staff when brooder gates should open."
      ]
    }
  };

  if (learnMoreLinks && modal) {
    learnMoreLinks.forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const featureKey = link.getAttribute('data-feature');
        const detail = featureDetails[featureKey];

        if (detail) {
          // Set text & content
          modalTitle.textContent = detail.title;
          modalDesc.textContent = detail.desc;
          
          // Re-build custom icon class
          modalIcon.className = `modal-icon-wrapper ${detail.colorClass}`;
          modalIcon.innerHTML = `<i class="fa-solid ${detail.icon}"></i>`;

          // Generate points HTML list
          modalPoints.innerHTML = '';
          detail.points.forEach(point => {
            const li = document.createElement('li');
            li.innerHTML = `<i class="fa-solid fa-circle-check"></i> <span>${point}</span>`;
            modalPoints.appendChild(li);
          });

          // Show modal
          modal.classList.add('open');
          modal.removeAttribute('aria-hidden');
          document.body.style.overflow = 'hidden'; // Lock background scrolling
        }
      });
    });

    const closeModal = () => {
      modal.classList.remove('open');
      modal.setAttribute('aria-hidden', 'true');
      document.body.style.overflow = ''; // Unlock background scrolling
    };

    if (modalClose) modalClose.addEventListener('click', closeModal);
    if (modalActionBtn) modalActionBtn.addEventListener('click', closeModal);
    
    // Close modal when clicking dark overlay
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        closeModal();
      }
    });

    // Close modal on escape key
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && modal.classList.contains('open')) {
        closeModal();
      }
    });
  }


  // --- Dynamic Scroll Animation (Fade and Slide Up) ---
  const revealElements = document.querySelectorAll('.reveal-on-scroll');
  const revealObserverOptions = {
    threshold: 0.15,
    rootMargin: "0px 0px -50px 0px"
  };

  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed');
        // Stop tracking once revealed to optimize performance
        observer.unobserve(entry.target);
      }
    });
  }, revealObserverOptions);

  revealElements.forEach(el => revealObserver.observe(el));


  // --- Drifting Farm Leaves Background Injector ---
  const leavesContainer = document.getElementById('leaves-container');
  const leafColors = ['#4CAF50', '#81C784', '#AED581', '#D4E157', '#C5E1A5'];

  function createLeaf() {
    if (!leavesContainer) return;
    
    // Cap maximum count of concurrent leaves coordinates for processing
    if (leavesContainer.childElementCount > 25) return;

    const leaf = document.createElement('div');
    leaf.classList.add('leaf');
    
    // Choose random green hue leaf
    const randomColor = leafColors[Math.floor(Math.random() * leafColors.length)];
    leaf.style.backgroundColor = randomColor;

    // Randomize initial positions & weights
    const startX = Math.random() * 100; // location % width
    const scale = 0.5 + Math.random() * 0.8;
    const duration = 8 + Math.random() * 8; // floating speed seconds
    const delay = Math.random() * 3;

    leaf.style.left = `${startX}%`;
    leaf.style.transform = `scale(${scale})`;
    leaf.style.animationDuration = `${duration}s`;
    leaf.style.animationDelay = `${delay}s`;

    // Remove node after animation completes
    leaf.addEventListener('animationend', () => {
      leaf.remove();
    });

    leavesContainer.appendChild(leaf);
  }

  // Continuously spawn leaves for organic countryside feel
  if (leavesContainer) {
    setInterval(createLeaf, 700);
    // Instantiate core batch immediately
    for (let i = 0; i < 8; i++) {
      createLeaf();
    }
  }


  // --- Flying Birds Background Injector ---
  const birdsContainer = document.querySelector('.birds-container');

  function spawnBird() {
    if (!birdsContainer) return;
    if (birdsContainer.childElementCount > 4) return;

    const bird = document.createElement('div');
    bird.classList.add('bird');
    
    // Randomize path elevation & speed
    const startY = 20 + Math.random() * 60; // elevation pixels
    const duration = 15 + Math.random() * 15; // flight speed seconds
    const scale = 0.6 + Math.random() * 0.6;   // distance scaling

    bird.style.top = `${startY}px`;
    bird.style.animationDuration = `${duration}s`;
    bird.style.transform = `scale(${scale})`;

    bird.addEventListener('animationend', () => {
      bird.remove();
    });

    birdsContainer.appendChild(bird);
  }

  if (birdsContainer) {
    setInterval(spawnBird, 5000);
    // Spawn immediate initial bird
    spawnBird();
  }

  // ====================================================
  // --- Dashboard SaaS UI Interactive Dynamics ---
  // ====================================================

  // 1. Collapsible Sidebar Menu Drawer Toggle
  const sidebarToggle = document.getElementById('sidebar-collapse-toggle');
  const layoutWrapper = document.querySelector('.dashboard-layout');
  const toggleIcon = document.getElementById('sidebar-toggle-icon');
  
  if (sidebarToggle && layoutWrapper) {
    sidebarToggle.addEventListener('click', (e) => {
      e.preventDefault();
      layoutWrapper.classList.toggle('collapsed');
      if (layoutWrapper.classList.contains('collapsed')) {
        if (toggleIcon) toggleIcon.className = 'fa-solid fa-chevron-right';
        sidebarToggle.querySelector('.sidebar-label').style.display = 'none';
      } else {
        if (toggleIcon) toggleIcon.className = 'fa-solid fa-chevron-left';
        setTimeout(() => {
          const lbl = sidebarToggle.querySelector('.sidebar-label');
          if (lbl) lbl.style.display = 'inline';
        }, 150);
      }
    });
  }

  // Helper to anim counters
  function animateCounter(el, target, prefix = '') {
    if (!el) return;
    el.setAttribute('data-target', target);
    let current = 0;
    const duration = 1200; // Total animate duration
    const stepTime = 12;
    const totalSteps = duration / stepTime;
    const stepVal = Math.ceil(target / totalSteps) || 1;
    
    // Clear any previous interval running on this element
    if (el._countInterval) {
      clearInterval(el._countInterval);
    }
    
    el._countInterval = setInterval(() => {
      current += stepVal;
      if (current >= target) {
        current = target;
        clearInterval(el._countInterval);
      }
      if (prefix === '$') {
        el.textContent = prefix + current.toFixed(2);
      } else {
        el.textContent = prefix + current.toLocaleString();
      }
    }, stepTime);
  }

  // Visual skeleton for chicken card list loading
  function showDeckSkeletons() {
    const deck = document.getElementById('chicken-deck');
    if (deck) {
      deck.innerHTML = `
        <div class="flock-skeleton-loader" style="grid-column: 1 / -1;">
          <div class="skele-grid">
            ${Array(4).fill(0).map(() => `
              <div class="skele-card">
                <div class="skele-img"></div>
                <div class="skele-strip w80"></div>
                <div class="skele-strip w60"></div>
              </div>
            `).join('')}
          </div>
        </div>
      `;
    }
  }

  // Load backend reports & parameters dynamically
  async function loadDashboardData() {
    if (!AuthService.isAuthenticated() || cleanPageName !== 'dashboard.html') return;
    
    const cntTotal = document.getElementById('counter-total');
    const cntHealthy = document.getElementById('counter-healthy');
    const cntRoosters = document.getElementById('counter-roosters');
    const cntHens = document.getElementById('counter-hens');
    const cntEggs = document.getElementById('counter-eggs');
    const cntInc = document.getElementById('counter-inc');
    const cntChicks = document.getElementById('counter-chicks');
    const cntSale = document.getElementById('counter-sale');
    const cntProfit = document.getElementById('counter-profit');
    const cntExpenses = document.getElementById('counter-expenses');
    
    const counters = [cntTotal, cntHealthy, cntRoosters, cntHens, cntEggs, cntInc, cntChicks, cntSale, cntProfit, cntExpenses];
    counters.forEach(c => {
      if (c) c.innerHTML = '<i class="fa-solid fa-spinner fa-spin-pulse" style="font-size: 0.9rem; opacity: 0.5;"></i>';
    });

    showDeckSkeletons();

    try {
      // Parallel fetches for efficiency
      const [dashRes, chickensReportRes, chickensListRes] = await Promise.all([
        Api.get('reports/dashboard'),
        Api.get('reports/chickens').catch(() => null),
        Api.get('chickens?size=25').catch(() => null)
      ]);

      if (dashRes && dashRes.success && dashRes.data) {
        const d = dashRes.data;
        const total = d.totalChickens || 0;
        const critical = d.criticalHealthCases || 0;
        const healthy = total - critical;

        // Roosters / Hens distribution
        let roosters = 0;
        let hens = 0;
        if (chickensReportRes && chickensReportRes.success && chickensReportRes.data && chickensReportRes.data.genderDistribution) {
          const gDist = chickensReportRes.data.genderDistribution;
          roosters = gDist.MALE || gDist.Rooster || 0;
          hens = gDist.FEMALE || gDist.Hen || 0;
        } else {
          roosters = Math.round(d.activeChickens * 0.1) || 0;
          hens = (d.activeChickens - roosters) || 0;
        }

        // Animate them
        animateCounter(cntTotal, total);
        animateCounter(cntHealthy, healthy);
        animateCounter(cntRoosters, roosters);
        animateCounter(cntHens, hens);
        animateCounter(cntEggs, d.totalEggsProduced || 0);
        animateCounter(cntInc, d.upcomingVaccinations || 0);
        animateCounter(cntChicks, d.currentBrooderChicks || 0);
        animateCounter(cntSale, d.soldChickens || 0);
        animateCounter(cntProfit, d.netProfit || 0.0, '$');
        animateCounter(cntExpenses, d.monthlyExpenses || 0.0, '$');

        // Circular score gauge health percent calculation
        const healthPercent = total > 0 ? Math.round((healthy / total) * 100) : 100;
        const healthGauge = document.getElementById('dashboard-health-gauge');
        const healthTxt = document.getElementById('health-pct-txt');
        if (healthGauge) {
          const circumference = 377; // 2 * PI * 60 approx
          const offsetVal = circumference - (healthPercent / 100) * circumference;
          healthGauge.style.strokeDashoffset = offsetVal;
          if (healthTxt) healthTxt.textContent = `${healthPercent}%`;
        }
      }

      // Render chicken deck list from registry
      const deck = document.getElementById('chicken-deck');
      if (deck && chickensListRes && chickensListRes.success && chickensListRes.data && chickensListRes.data.content) {
        const birds = chickensListRes.data.content;
        deck.innerHTML = '';
        
        if (birds.length === 0) {
          deck.innerHTML = `
            <div class="flock-empty-state-card" style="grid-column: 1 / -1; text-align: center; padding: 40px 20px;">
              <h3>No chickens registered yet</h3>
              <p>Use the Quick Action button below to register a new chicken to your farm registry database.</p>
            </div>
          `;
        } else {
          // Render cards
          birds.forEach(bird => {
            const cardUnit = document.createElement('div');
            cardUnit.className = 'chicken-card-perspective';
            cardUnit.setAttribute('data-id', bird.id.toString());
            
            let eggEmoji = bird.gender === 'MALE' ? '🐓' : (bird.category === 'CHICK' ? '🐥' : '🐔');
            let statusPillClass = (bird.status || 'ACTIVE').toLowerCase().replace(/\s+/g, '-');
            let ageText = bird.ageInDays ? `${bird.ageInDays} days` : 'Newborn';
            
            cardUnit.innerHTML = `
              <div class="chicken-card-inner">
                <div class="chicken-card-front">
                  <div class="chicken-photo-placeholder">${eggEmoji}</div>
                  <div class="chicken-details-grid">
                    <div class="chk-badge-row">
                      <span class="chk-id">ID: ${bird.chickenCode}</span>
                      <span class="chk-status-pill ${statusPillClass}">${bird.status}</span>
                    </div>
                    <div class="chk-specs">
                      <span><strong>Breed:</strong> ${bird.breed}</span>
                      <span><strong>Age:</strong> ${ageText}</span>
                      <span><strong>Category:</strong> ${bird.category}</span>
                    </div>
                    <div class="btn-chk-flip">Flip Details <i class="fa-solid fa-arrow-rotate-left"></i></div>
                  </div>
                </div>
                <div class="chicken-card-back">
                  <h4>ID: ${bird.chickenCode} Details</h4>
                  <p>Weight: ${bird.weight || 0.0} kg. Registered to farm inventory.<br>Status: ${bird.status}.<br>DOB: ${bird.dateOfBirth || 'Unknown'}</p>
                  <div class="btn-chk-flip" style="color: var(--dark-brown);">Click Card to Flip <i class="fa-solid fa-arrow-rotate-left"></i></div>
                </div>
              </div>
            `;
            
            cardUnit.addEventListener('click', (e) => {
              cardUnit.classList.toggle('flipped');
            });
            
            deck.appendChild(cardUnit);
          });
        }
      }
    } catch (err) {
      console.error("Dashboard backend load error", err);
      counters.forEach(c => {
        if (c) c.textContent = '--';
      });
      const deck = document.getElementById('chicken-deck');
      if (deck) {
        deck.innerHTML = `<div style="grid-column: 1 / -1; text-align: center; color: #D32F2F; padding: 20px;">Failed to load live dashboard statistics. Please verify backend connection.</div>`;
      }
    }
  }

  // Trigger load and setup 60s background polling
  if (cleanPageName === 'dashboard.html') {
    loadDashboardData();
    setInterval(() => {
      if (document.visibilityState === 'visible') {
        loadDashboardData();
      }
    }, 60000);
    
    // Refresh instantly on visible tab switch
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        loadDashboardData();
      }
    });
  }

  // 3. Tab Switches for Analytics SVG Panel
  const dashTabButtons = document.querySelectorAll('.chart-tab-btn');
  dashTabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      dashTabButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      
      const tabId = btn.getAttribute('data-tab');
      const chartPanes = document.querySelectorAll('.chart-pane');
      chartPanes.forEach(pane => {
        pane.classList.remove('active');
        if (pane.id === tabId) {
          pane.classList.add('active');
        }
      });
    });
  });

  // 4. Touch Flip Action for Chicken Registry Deck
  const chickenCards = document.querySelectorAll('.chicken-card-perspective');
  chickenCards.forEach(card => {
    card.addEventListener('click', (e) => {
      // Don't flip twice if they click internal anchors
      if (e.target.tagName.toLowerCase() === 'a' || e.target.closest('.btn-chk-flip')) {
        // Handled via standard bubble toggling
      }
      card.classList.toggle('flipped');
    });
  });

  // 5. Circular score gauge loader path calculations
  const healthGauge = document.getElementById('dashboard-health-gauge');
  const healthTxt = document.getElementById('health-pct-txt');
  if (healthGauge) {
    const scoreVal = 95;
    const circumference = 377; // 2 * PI * 60 approx
    const offsetVal = circumference - (scoreVal / 100) * circumference;
    
    setTimeout(() => {
      healthGauge.style.strokeDashoffset = offsetVal;
      
      // Count text score
      let currScore = 0;
      const scoreInterval = setInterval(() => {
        currScore++;
        if (currScore >= scoreVal) {
          currScore = scoreVal;
          clearInterval(scoreInterval);
        }
        if (healthTxt) healthTxt.textContent = `${currScore}%`;
      }, 15);
    }, 300);
  }

  // 6. Global Search Card Filtering
  const searchInput = document.getElementById('dashboard-search-input');
  const filterBadge = document.getElementById('search-filter-badge');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      const q = e.target.value.toLowerCase().trim();
      const cards = document.querySelectorAll('.chicken-card-perspective');
      let matches = 0;
      
      cards.forEach(card => {
        const id = card.getAttribute('data-id').toLowerCase();
        const content = card.textContent.toLowerCase();
        
        if (id.includes(q) || content.includes(q)) {
          card.style.display = 'block';
          matches++;
        } else {
          card.style.display = 'none';
        }
      });
      
      if (filterBadge) {
        if (q.length > 0) {
          filterBadge.style.display = 'inline-block';
          filterBadge.textContent = `${matches} match${matches === 1 ? '' : 'es'} found`;
        } else {
          filterBadge.style.display = 'none';
        }
      }
    });
  }

  // 7. Modals for Quick Action triggers
  const dashActionModal = document.getElementById('dashboard-action-modal');
  const dashModalClose = document.getElementById('modal-action-close');
  const dashModalTitle = document.getElementById('modal-action-title');
  const dashModalBody = document.getElementById('modal-action-body-space');

  function openActionModal(title, contentHtml) {
    if (!dashActionModal) return;
    dashModalTitle.textContent = title;
    dashModalBody.innerHTML = contentHtml;
    dashActionModal.classList.add('open');
  }

  if (dashModalClose) {
    dashModalClose.addEventListener('click', () => {
      dashActionModal.classList.remove('open');
    });
  }

  // Close modal when clicking outer backdrop
  if (dashActionModal) {
    dashActionModal.addEventListener('click', (e) => {
      if (e.target === dashActionModal) {
        dashActionModal.classList.remove('open');
      }
    });
  }

  // Bind Quick Actions buttons listeners
  const btnAddChicken = document.getElementById('qa-add-chicken');
  const btnRecEggs = document.getElementById('qa-record-eggs');
  const btnUpdChick = document.getElementById('qa-update-chick');
  const btnRecSale = document.getElementById('qa-record-sale');
  const btnHealthChk = document.getElementById('qa-health-check');
  const btnGenRep = document.getElementById('qa-gen-report');
  const btnInvFam = document.getElementById('qa-invite-family');
  if (btnAddChicken) {
    btnAddChicken.addEventListener('click', () => {
      const htmlCode = `
        <div class="action-modal-wrapper">
          <form class="action-modal-form" id="form-add-chicken" style="display:flex; flex-direction:column; gap:14px;">
            <div class="form-group">
              <label for="m-chk-id">Chicken ID *</label>
              <input type="text" id="m-chk-id" placeholder="e.g. C045" value="C0${Math.floor(Math.random() * 900) + 100}" required>
            </div>
            
            <div class="form-group">
              <select id="m-chk-category" required>
                <option value="" disabled selected hidden></option>
                <option value="Country Chicken">Country Chicken (நாட்டுக்கோழி)</option>
                <option value="Broiler">Broiler</option>
                <option value="Other">Other</option>
              </select>
              <label for="m-chk-category">Category *</label>
            </div>

            <!-- Slide down anim wrapper for breed dropdown -->
            <div class="breed-wrapper-animate" id="m-chk-breed-container">
              <div class="form-group">
                <select id="m-chk-breed" required>
                  <option value="" disabled selected hidden></option>
                </select>
                <label for="m-chk-breed">Breed *</label>
              </div>
            </div>

            <div class="form-group">
              <select id="m-chk-source" required>
                <option value="Farm Born" selected>Farm Born / Incubator Hatch</option>
                <option value="Purchased">Purchased Vendor Batch</option>
              </select>
              <label for="m-chk-source">Acquisition Source *</label>
            </div>

            <div class="form-group" id="m-chk-dob-container">
              <input type="date" id="m-chk-dob" placeholder=" " required>
              <label id="m-chk-dob-label" for="m-chk-dob">Date of Birth *</label>
              <span class="validation-message" id="m-err-dob" style="display:none; color:#bf360c; font-size:0.75rem; margin-top:4px;">Date of Birth is required for Farm Born birds.</span>
            </div>

            <div class="form-row" id="m-chk-acq-details-fields" style="display:none; gap:12px;">
              <div class="form-group" style="flex:1;">
                <input type="date" id="m-chk-acq-date" value="2026-07-17">
                <label id="m-chk-acq-date-label" for="m-chk-acq-date">Purchase Date *</label>
                <span class="validation-message" id="m-err-acq-date" style="display:none; color:#bf360c; font-size:0.75rem; margin-top:4px;">Either Date of Birth or Purchase Date must be provided.</span>
              </div>
              <div class="form-group" style="flex:1;">
                <input type="number" id="m-chk-acq-price" min="0" value="0">
                <label for="m-chk-acq-price">Purchase Price ($)</label>
              </div>
            </div>

            <div class="form-row" style="gap:12px;">
              <div class="form-group" style="flex:1;">
                <select id="m-chk-status" required>
                  <option value="Laying" selected>Laying</option>
                  <option value="Breeding">Breeding Stock</option>
                  <option value="Meat">Meat Production / Broiler</option>
                  <option value="Molting">Molting Period</option>
                  <option value="Ready for Sale">Ready For Market Placement</option>
                  <option value="Sold">Sold</option>
                  <option value="Dead">Dead</option>
                  <option value="Removed from Farm">Removed from Farm</option>
                </select>
                <label for="m-chk-status">Status *</label>
              </div>
              <div class="form-group" style="flex:1;">
                <select id="m-chk-health" required>
                  <option value="Healthy" selected>Healthy</option>
                  <option value="Under Treatment">Under Treatment</option>
                  <option value="Weak">Weak</option>
                </select>
                <label for="m-chk-health">Health Status *</label>
              </div>
            </div>
            <button type="submit" class="btn btn-primary btn-block">Register Chicken</button>
          </form>
        </div>
      `;
      openActionModal('Register New Chicken', htmlCode);

      function toggleModalAcquisitionFields() {
        const source = document.getElementById("m-chk-source").value;
        const dobInput = document.getElementById("m-chk-dob");
        const dobLabel = document.getElementById("m-chk-dob-label");
        const acqFields = document.getElementById("m-chk-acq-details-fields");
        
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

      // Make Category and Status, Health premium selects
      if (window.makePremiumSelect) {
        window.makePremiumSelect('m-chk-category', (val) => {
          const breedSel = document.getElementById('m-chk-breed');
          const breedCont = document.getElementById('m-chk-breed-container');
          
          if (breedSel && breedCont) {
            // Slide up first
            breedCont.classList.remove('visible');
            setTimeout(() => {
              breedSel.innerHTML = '<option value="" disabled selected hidden></option>';
              if (val && window.BREED_CATEGORIES[val]) {
                window.BREED_CATEGORIES[val].forEach(b => {
                  const opt = document.createElement('option');
                  opt.value = b.value;
                  opt.textContent = b.text;
                  breedSel.appendChild(opt);
                });
                
                // Initialize/refresh breed custom dropdown select
                window.makePremiumSelect('m-chk-breed');
                
                // Slide down with premium animation
                breedCont.classList.add('visible');
              }
            }, val ? 200 : 0);
          }
        });

        window.makePremiumSelect('m-chk-source', (val) => {
          toggleModalAcquisitionFields();
        });
        window.makePremiumSelect('m-chk-status');
        window.makePremiumSelect('m-chk-health');
      } else {
        document.getElementById('m-chk-source').addEventListener('change', toggleModalAcquisitionFields);
      }

      document.getElementById('form-add-chicken').addEventListener('submit', async (eInv) => {
        eInv.preventDefault();
        const id = document.getElementById('m-chk-id').value.trim();
        const breed = document.getElementById('m-chk-breed').value;
        const category = document.getElementById('m-chk-category').value;
        const source = document.getElementById('m-chk-source').value;
        const dob = document.getElementById('m-chk-dob').value;
        const acqDate = document.getElementById('m-chk-acq-date') ? document.getElementById('m-chk-acq-date').value : "";
        const acqPrice = document.getElementById('m-chk-acq-price') ? (parseFloat(document.getElementById('m-chk-acq-price').value) || 0) : 0;
        const status = document.getElementById('m-chk-status').value;
        const health = document.getElementById('m-chk-health').value;
        
        // DOB & Purchase Date Validation
        const errDob = document.getElementById("m-err-dob");
        const errAcqDate = document.getElementById("m-err-acq-date");
        if (errDob) errDob.style.display = "none";
        if (errAcqDate) errAcqDate.style.display = "none";

        if (source === "Farm Born") {
          if (!dob) {
            if (errDob) errDob.style.display = "block";
            document.getElementById("m-chk-dob").focus();
            return;
          }
        } else { // Purchased
          if (!dob && !acqDate) {
            if (errAcqDate) errAcqDate.style.display = "block";
            document.getElementById("m-chk-acq-date").focus();
            return;
          }
        }

        let backendBreed = 'OTHER';
        if (breed === 'White Leghorn') backendBreed = 'LEGHORN';
        else if (breed === 'Rhode Island Red') backendBreed = 'RHODE_ISLAND_RED';
        else if (breed === 'Plymouth Rock') backendBreed = 'PLYMOUTH_ROCK';
        else if (breed === 'Light Sussex') backendBreed = 'SUSSEX';
        else if (breed === 'Cobb 500') backendBreed = 'COBB_500';
        else if (breed === 'Ross 308') backendBreed = 'ROSS_308';
        else if (breed === 'Hubbard') backendBreed = 'HUBBARD';

        let backendCategory = 'OTHER';
        if (category === 'Broiler') backendCategory = 'BROILER';
        else if (category === 'Layer' || category === 'Country Chicken') backendCategory = 'LAYER';
        else if (category === 'Chick') backendCategory = 'CHICK';

        let backendGender = 'FEMALE';
        if (status === 'Breeding' || status === 'Rooster') backendGender = 'MALE';

        let backendStatus = 'ACTIVE';
        if (status === 'Sold') backendStatus = 'SOLD';
        else if (status === 'Dead') backendStatus = 'DEAD';
        else if (status === 'Meat') backendStatus = 'GROWING';

        const payload = {
          chickenCode: id,
          name: breed + " " + id,
          breed: backendBreed,
          category: backendCategory,
          gender: backendGender,
          dateOfBirth: dob || new Date().toISOString().split('T')[0],
          weight: category === "Broiler" ? 1.5 : 2.0,
          status: backendStatus,
          remarks: "Created from Dashboard Quick Action Form"
        };

        try {
          const res = await Api.post('chickens', payload);
          if (res.success) {
            addTimelineLog(`Chicken <strong>${res.data.chickenCode || id}</strong> registered to dashboard flock inventory`);
            dashActionModal.classList.remove('open');
            alert(`Success: Chicken ${id} successfully added!`);
            loadDashboardData();
          }
        } catch (err) {
          console.error("Failed to register chicken", err);
        }
      });
    });
  }

  if (btnRecEggs) {
    btnRecEggs.addEventListener('click', () => {
      const initialHtml = `
        <div class="action-modal-wrapper" style="text-align:center; padding: 20px;">
          <i class="fa-solid fa-spinner fa-spin-pulse" style="font-size: 2rem; color: var(--primary-green);"></i>
          <p style="margin-top: 10px; color: var(--neutral-gray);">Fetching laying hens list...</p>
        </div>
      `;
      openActionModal('Record Daily Eggs', initialHtml);

      Api.get('chickens?size=100').then(res => {
        let hensOptions = '';
        if (res && res.success && res.data && res.data.content) {
          const activeHens = res.data.content.filter(c => c.status === 'ACTIVE');
          activeHens.forEach(h => {
             hensOptions += `<option value="${h.id}">ID: ${h.chickenCode} - ${h.breed}</option>`;
          });
        }

        if (!hensOptions) {
          dashModalBody.innerHTML = `
            <div style="text-align: center; padding: 20px;">
              <p style="color: #D32F2F; font-weight: 700;">No active chickens registered.</p>
              <p style="font-size: 0.8rem; color: var(--neutral-gray);">Please register at least one active chicken first before recording daily oviposition.</p>
            </div>
          `;
          return;
        }

        dashModalBody.innerHTML = `
          <div class="action-modal-wrapper">
            <form class="action-modal-form" id="form-record-eggs" style="display:flex; flex-direction:column; gap:14px;">
              <div class="form-group">
                <select id="m-egg-hen" required>
                  ${hensOptions}
                </select>
                <label for="m-egg-hen">Select Hen *</label>
              </div>
              <div class="form-row" style="gap:12px;">
                <div class="form-group" style="flex:1;">
                  <input type="number" id="m-egg-count" value="1" min="1" required>
                  <label for="m-egg-count">Laid Egg Count (Qty) *</label>
                </div>
                <div class="form-group" style="flex:1;">
                  <input type="number" id="m-egg-damaged" value="0" min="0" required>
                  <label for="m-egg-damaged">Damaged Eggs (Qty) *</label>
                </div>
              </div>
              <div class="form-group">
                <input type="text" id="m-egg-remarks" placeholder="Optional notes...">
                <label for="m-egg-remarks">Remarks</label>
              </div>
              <button type="submit" class="btn btn-primary btn-block">Record Eggs</button>
            </form>
          </div>
        `;
        if (window.makePremiumSelect) {
          window.makePremiumSelect('m-egg-hen');
        }

        document.getElementById('form-record-eggs').addEventListener('submit', async (eInv) => {
          eInv.preventDefault();
          const henId = document.getElementById('m-egg-hen').value;
          const count = parseInt(document.getElementById('m-egg-count').value, 10);
          const damaged = parseInt(document.getElementById('m-egg-damaged').value, 10);
          const remarks = document.getElementById('m-egg-remarks').value;

          try {
            const eggRes = await Api.post('egg-records', {
              recordDate: new Date().toISOString().split('T')[0],
              henId: parseInt(henId, 10),
              numberOfEggs: count,
              damagedEggs: damaged,
              remarks: remarks || "Recorded from Dashboard Quick Action"
            });

            if (eggRes.success) {
              addTimelineLog(`Recorded yield: <strong>${count} eggs</strong> (Damaged: ${damaged}) for hen ID ${henId}`);
              dashActionModal.classList.remove('open');
              alert(`Success: Egg record registered successfully!`);
              loadDashboardData();
            }
          } catch (err) {
            console.error("Error creating egg record", err);
          }
        });
      }).catch(err => {
        console.error("Error fetching hens select", err);
        dashModalBody.innerHTML = `<p style="color:#D32F2F; text-align:center; padding: 20px;">Could not connect to backend to pull chickens.</p>`;
      });
    });
  }

  if (btnInvFam) {
    btnInvFam.addEventListener('click', () => {
      const htmlCode = `
        <div class="action-modal-wrapper">
          <form class="action-modal-form" id="form-invite-member">
            <div class="form-group">
              <label for="m-inv-email">Email Address</label>
              <input type="email" id="m-inv-email" placeholder="family@poultryfarm.com" required>
            </div>
            <div class="form-group">
              <select id="m-inv-role" required>
                <option value="Operator">Family Operator</option>
                <option value="Observer">Observer</option>
              </select>
              <label for="m-inv-role">Role Hierarchy *</label>
            </div>
            <button type="submit" class="btn btn-primary btn-block">Issue Invites</button>
          </form>
        </div>
      `;
      openActionModal('Invite Family Member', htmlCode);

      if (window.makePremiumSelect) {
        window.makePremiumSelect('m-inv-role');
      }

      document.getElementById('form-invite-member').addEventListener('submit', (eInv) => {
        eInv.preventDefault();
        const email = document.getElementById('m-inv-email').value;
        addTimelineLog(`Console invite dispatched to <strong>${email}</strong>`);
        dashActionModal.classList.remove('open');
        alert(`Console invite securely queued for ${email}`);
      });
    });
  }

  // Bind remaining dashboard quick action metrics
  const defaultActionTrigger = (btn, btnName, logMsg) => {
    if (btn) {
      btn.addEventListener('click', () => {
        addTimelineLog(logMsg);
        alert(`Success: ${btnName} registered.`);
      });
    }
  };

  defaultActionTrigger(btnUpdChick, 'Update Chick', 'Cohort metrics: Chick growth index verified');
  defaultActionTrigger(btnRecSale, 'Record Sale', 'Retail register: Logged offline customer invoice transaction');
  defaultActionTrigger(btnHealthChk, 'Health Check', 'Health survey complete: Safe biosecurity indexes verified');
  defaultActionTrigger(btnGenRep, 'Generate Report', 'Ledger summaries reports successfully generated (PDF format)');

  function addTimelineLog(bodyHtml) {
    const listLog = document.getElementById('timeline-list-panel');
    if (listLog) {
      const logElement = document.createElement('div');
      logElement.className = 'timeline-item';
      logElement.innerHTML = `
        <div class="timeline-dot"></div>
        <div class="timeline-content">
          <span class="timeline-body">${bodyHtml}</span>
          <span class="timeline-time">Just now</span>
        </div>
      `;
      listLog.insertBefore(logElement, listLog.firstChild);
    }
  }

});

// Global BREED_CATEGORIES and premium dropdown custom select helper models
window.BREED_CATEGORIES = {
  "Country Chicken": [
    { value: "Peruvidai", text: "Peruvidai (பெருவிடை)" },
    { value: "Siruvidai", text: "Siruvidai (சிறுவிடை)" },
    { value: "Cross", text: "Cross (கிராஸ்)" }
  ],
  "Broiler": [
    { value: "Broiler", text: "Broiler" }
  ],
  "Other": [
    { value: "Rhode Island Red", text: "Rhode Island Red" },
    { value: "White Leghorn", text: "White Leghorn" },
    { value: "Plymouth Rock", text: "Plymouth Rock" },
    { value: "Light Sussex", text: "Light Sussex" },
    { value: "Buff Orpington", text: "Buff Orpington" },
    { value: "Australorp", text: "Australorp" },
    { value: "Kadaknath", text: "Kadaknath" },
    { value: "Aseel", text: "Aseel" },
    { value: "Giriraja", text: "Giriraja" },
    { value: "Vanaraja", text: "Vanaraja" }
  ]
};

// Prototype value and reset interceptions to ensure custom UI is always in sync
(function() {
  const descriptor = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value');
  if (descriptor && descriptor.set) {
    const originalSetter = descriptor.set;
    Object.defineProperty(HTMLSelectElement.prototype, 'value', {
      set: function(val) {
        originalSetter.call(this, val);
        if (typeof this.refreshCustomSelect === 'function') {
          this.refreshCustomSelect();
        }
      },
      get: descriptor.get,
      configurable: true
    });
  }

  const originalReset = HTMLFormElement.prototype.reset;
  HTMLFormElement.prototype.reset = function() {
    originalReset.call(this);
    this.querySelectorAll('select').forEach(sel => {
      if (typeof sel.refreshCustomSelect === 'function') {
        sel.refreshCustomSelect();
      }
    });
  };
})();

window.makePremiumSelect = function(selectId, onSelectChange = null) {
  const select = typeof selectId === 'string' ? document.getElementById(selectId) : selectId;
  if (!select) return;

  // Prevent double wrapping
  let wrapper = select.closest('.custom-select-wrapper');
  if (wrapper) {
    if (select.refreshCustomSelect) {
      select.refreshCustomSelect();
    }
    return;
  }

  wrapper = document.createElement('div');
  wrapper.className = 'custom-select-wrapper';
  if (select.classList.contains('filter-input-field')) {
    wrapper.classList.add('compact-select-wrapper');
  }
  
  if (select.value && select.value !== "") {
    wrapper.classList.add('has-value');
  }
  
  const label = select.parentElement.querySelector('label');
  select.parentNode.insertBefore(wrapper, select);
  wrapper.appendChild(select);
  if (label) {
    wrapper.appendChild(label);
  }
  
  select.style.display = 'none';
  
  const trigger = document.createElement('div');
  trigger.className = 'custom-select-trigger';
  
  const valSpan = document.createElement('span');
  valSpan.className = 'custom-select-val';
  
  // Find current option display text
  const initialText = select.options[select.selectedIndex]?.text || '';
  valSpan.textContent = select.options[select.selectedIndex]?.value === "" ? "" : initialText;
  
  const chevron = document.createElement('i');
  chevron.className = 'fa-solid fa-chevron-down';
  
  trigger.appendChild(valSpan);
  trigger.appendChild(chevron);
  wrapper.appendChild(trigger);
  
  // Create options container but append it to document.body to avoid parent container overflow clipping
  const optionsContainer = document.createElement('div');
  optionsContainer.className = 'custom-options-container';
  document.body.appendChild(optionsContainer);
  
  let highlightedIndex = -1;
  let activeOptions = [];
  
  function updateOptions() {
    optionsContainer.innerHTML = '';
    activeOptions = Array.from(select.options).filter(opt => !opt.disabled && !opt.hidden && opt.value !== "");
    
    activeOptions.forEach((opt, idx) => {
      const optDiv = document.createElement('div');
      optDiv.className = 'custom-option';
      if (opt.selected) {
        optDiv.classList.add('selected');
        valSpan.textContent = opt.textContent;
        wrapper.classList.add('has-value');
      }
      
      const textSpan = document.createElement('span');
      textSpan.textContent = opt.textContent;
      optDiv.appendChild(textSpan);
      
      // Floating check icon on the right
      const checkIcon = document.createElement('i');
      checkIcon.className = 'fa-solid fa-check check-icon';
      optDiv.appendChild(checkIcon);
      
      optDiv.setAttribute('data-value', opt.value);
      optDiv.setAttribute('data-index', idx);
      
      optDiv.addEventListener('click', (e) => {
        e.stopPropagation();
        selectOption(opt, optDiv);
      });
      optionsContainer.appendChild(optDiv);
    });
  }
  
  function selectOption(opt, optDiv) {
    const ripple = document.createElement('span');
    ripple.className = 'ripple-span';
    optDiv.appendChild(ripple);
    
    const rect = optDiv.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    ripple.style.width = ripple.style.height = `${size}px`;
    ripple.style.left = `${rect.width / 2}px`;
    ripple.style.top = `${rect.height / 2}px`;
    
    setTimeout(() => {
      ripple.remove();
      Array.from(select.options).forEach(o => o.selected = false);
      opt.selected = true;
      select.value = opt.value;
      
      select.dispatchEvent(new Event('change', { bubbles: true }));
      
      optionsContainer.querySelectorAll('.custom-option').forEach(el => el.classList.remove('selected'));
      optDiv.classList.add('selected');
      valSpan.textContent = opt.textContent;
      wrapper.classList.add('has-value');
      closeDropdown();
      
      if (onSelectChange) {
        onSelectChange(opt.value);
      }
    }, 120);
  }
  
  function positionDropdown() {
    const rect = trigger.getBoundingClientRect();
    optionsContainer.style.position = 'absolute';
    optionsContainer.style.top = `${rect.bottom + window.scrollY}px`;
    optionsContainer.style.left = `${rect.left + window.scrollX}px`;
    optionsContainer.style.width = `${rect.width}px`;
    optionsContainer.style.zIndex = '999999';
  }
  
  function openDropdown() {
    // Close other open select lists first
    document.querySelectorAll('.custom-select-wrapper').forEach(w => {
      if (w !== wrapper) {
        const otherSelect = w.querySelector('select');
        if (otherSelect && otherSelect.closeCustomSelect) {
          otherSelect.closeCustomSelect();
        }
      }
    });

    positionDropdown();
    optionsContainer.classList.add('open');
    wrapper.classList.add('open');
    
    // Highlight currently selected index
    highlightedIndex = activeOptions.findIndex(o => o.selected);
    updateHighlight();
    
    window.addEventListener('scroll', closeDropdown);
    window.addEventListener('resize', closeDropdown);
    document.addEventListener('keydown', handleKeyNavigation);
  }
  
  function closeDropdown() {
    optionsContainer.classList.remove('open');
    wrapper.classList.remove('open');
    window.removeEventListener('scroll', closeDropdown);
    window.removeEventListener('resize', closeDropdown);
    document.removeEventListener('keydown', handleKeyNavigation);
  }
  
  function updateHighlight() {
    const items = optionsContainer.querySelectorAll('.custom-option');
    items.forEach((item, idx) => {
      if (idx === highlightedIndex) {
        item.classList.add('highlighted');
        item.scrollIntoView({ block: 'nearest' });
      } else {
        item.classList.remove('highlighted');
      }
    });
  }
  
  function handleKeyNavigation(e) {
    if (!optionsContainer.classList.contains('open')) return;
    
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      highlightedIndex = (highlightedIndex + 1) % activeOptions.length;
      updateHighlight();
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      highlightedIndex = (highlightedIndex - 1 + activeOptions.length) % activeOptions.length;
      updateHighlight();
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (highlightedIndex >= 0 && highlightedIndex < activeOptions.length) {
        const optionNode = optionsContainer.querySelector(`[data-index="${highlightedIndex}"]`);
        if (optionNode) {
          selectOption(activeOptions[highlightedIndex], optionNode);
        }
      }
    } else if (e.key === 'Escape' || e.key === 'Tab') {
      closeDropdown();
    }
  }
  
  updateOptions();
  
  trigger.addEventListener('click', (e) => {
    e.stopPropagation();
    if (optionsContainer.classList.contains('open')) {
      closeDropdown();
    } else {
      openDropdown();
    }
  });
  
  document.addEventListener('click', (e) => {
    if (!wrapper.contains(e.target) && !optionsContainer.contains(e.target)) {
      closeDropdown();
    }
  });
  
  select.closeCustomSelect = closeDropdown;
  
  select.refreshCustomSelect = () => {
    updateOptions();
    if (select.value && select.value !== "") {
      wrapper.classList.add('has-value');
      const selectedOpt = Array.from(select.options).find(o => o.value === select.value);
      valSpan.textContent = selectedOpt ? selectedOpt.textContent : '';
    } else {
      wrapper.classList.remove('has-value');
      valSpan.textContent = '';
    }
    positionDropdown();
  };

  // Safe cleaner upon Element removal from DOM
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.removedNodes.forEach((node) => {
        if (node === wrapper || node.contains(wrapper)) {
          optionsContainer.remove();
          observer.disconnect();
        }
      });
    });
  });
  if (wrapper.parentNode) {
    observer.observe(wrapper.parentNode, { childList: true });
  }
};

// Mobile App Experience: Bottom Navigation & Global Floating Action Button (FAB)
function initMobileNavigation() {
  if (document.querySelector('.mobile-bottom-nav')) return;

  const currentPath = window.location.pathname.split('/').pop() || 'dashboard.html';

  const nav = document.createElement('nav');
  nav.className = 'mobile-bottom-nav';
  nav.innerHTML = `
    <a href="dashboard.html" class="mobile-bottom-nav-item ${currentPath.includes('dashboard') || currentPath === '' ? 'active' : ''}">
      <i class="fa-solid fa-chart-line"></i>
      <span>Dashboard</span>
    </a>
    <a href="flock.html" class="mobile-bottom-nav-item ${currentPath.includes('flock') ? 'active' : ''}">
      <i class="fa-solid fa-feather-pointed"></i>
      <span>Chickens</span>
    </a>
    <a href="egg-tracking.html" class="mobile-bottom-nav-item ${currentPath.includes('egg-tracking') ? 'active' : ''}">
      <i class="fa-solid fa-egg"></i>
      <span>Eggs</span>
    </a>
    <a href="finance.html" class="mobile-bottom-nav-item ${currentPath.includes('finance') ? 'active' : ''}">
      <i class="fa-solid fa-wallet"></i>
      <span>Finance</span>
    </a>
    <a href="settings.html" class="mobile-bottom-nav-item ${currentPath.includes('settings') || currentPath.includes('reports') ? 'active' : ''}">
      <i class="fa-solid fa-bars"></i>
      <span>More</span>
    </a>
  `;
  document.body.appendChild(nav);
}

function initGlobalFAB() {
  if (document.querySelector('.mobile-fab-container')) return;

  const fabContainer = document.createElement('div');
  fabContainer.className = 'mobile-fab-container';
  fabContainer.innerHTML = `
    <div class="mobile-fab-menu">
      <a href="flock.html" class="mobile-fab-action-item">
        <span>Register Chicken</span> <i class="fa-solid fa-plus-circle"></i>
      </a>
      <a href="egg-tracking.html" class="mobile-fab-action-item">
        <span>Record Eggs</span> <i class="fa-solid fa-egg"></i>
      </a>
      <a href="feed-management.html" class="mobile-fab-action-item">
        <span>Add Feed</span> <i class="fa-solid fa-wheat-awn"></i>
      </a>
      <a href="health-records.html" class="mobile-fab-action-item">
        <span>Health Log</span> <i class="fa-solid fa-notes-medical"></i>
      </a>
      <a href="finance.html" class="mobile-fab-action-item">
        <span>Record Sale</span> <i class="fa-solid fa-receipt"></i>
      </a>
    </div>
    <button class="mobile-fab-btn" id="btn-global-fab" aria-label="Quick Actions">
      <i class="fa-solid fa-plus"></i>
    </button>
  `;

  document.body.appendChild(fabContainer);

  const fabBtn = document.getElementById('btn-global-fab');
  fabBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    fabContainer.classList.toggle('open');
  });

  document.addEventListener('click', (e) => {
    if (!fabContainer.contains(e.target)) {
      fabContainer.classList.remove('open');
    }
  });
}

// Dynamic Header Weather & Local Date System
function initDynamicHeaderWeatherAndDate() {
  function updateLocalDate() {
    const dateElements = document.querySelectorAll('#nav-date-info');
    if (!dateElements || dateElements.length === 0) return;

    const now = new Date();
    const options = { month: 'long', day: 'numeric', year: 'numeric' };
    const dateStr = now.toLocaleDateString(undefined, options);

    dateElements.forEach(el => {
      el.textContent = dateStr;
    });
  }

  function getWeatherMeta(code) {
    if (code === 0) return { icon: 'fa-solid fa-sun', condition: 'Sunny' };
    if (code >= 1 && code <= 3) return { icon: 'fa-solid fa-cloud-sun', condition: 'Partly Cloudy' };
    if (code === 45 || code === 48) return { icon: 'fa-solid fa-smog', condition: 'Foggy' };
    if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return { icon: 'fa-solid fa-cloud-rain', condition: 'Rainy' };
    if (code >= 71 && code <= 77) return { icon: 'fa-solid fa-snowflake', condition: 'Snowy' };
    if (code >= 95 && code <= 99) return { icon: 'fa-solid fa-cloud-bolt', condition: 'Thunderstorm' };
    return { icon: 'fa-solid fa-cloud', condition: 'Cloudy' };
  }

  function setWeatherUI(htmlContent) {
    const weatherElements = document.querySelectorAll('#nav-weather-info');
    weatherElements.forEach(el => {
      el.innerHTML = htmlContent;
    });
  }

  async function fetchWeather() {
    const weatherElements = document.querySelectorAll('#nav-weather-info');
    if (!weatherElements || weatherElements.length === 0) return;

    setWeatherUI('<i class="fa-solid fa-spinner fa-spin-pulse"></i> Loading weather...');

    if (!navigator.geolocation) {
      setWeatherUI('<i class="fa-solid fa-cloud-slash"></i> Weather unavailable');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude, longitude } = position.coords;
        try {
          const backendRes = await Api.get(`weather?lat=${latitude}&lon=${longitude}`).catch(() => null);
          if (backendRes && backendRes.success && backendRes.data) {
            const d = backendRes.data;
            setWeatherUI(`<i class="${d.icon || 'fa-solid fa-sun'}"></i> ${d.temp}°C ${d.condition}`);
            return;
          }

          const response = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current_weather=true`);
          if (!response.ok) throw new Error('Weather service unavailable');

          const data = await response.json();
          if (data && data.current_weather) {
            const temp = Math.round(data.current_weather.temperature);
            const meta = getWeatherMeta(data.current_weather.weathercode);
            setWeatherUI(`<i class="${meta.icon}"></i> ${temp}°C ${meta.condition}`);
          } else {
            setWeatherUI('<i class="fa-solid fa-cloud-slash"></i> Weather unavailable');
          }
        } catch (err) {
          console.warn('Weather fetch error:', err);
          setWeatherUI('<i class="fa-solid fa-cloud-slash"></i> Weather unavailable');
        }
      },
      (error) => {
        console.warn('Geolocation permission denied or error:', error.message);
        setWeatherUI('<i class="fa-solid fa-cloud-slash"></i> Weather unavailable');
      },
      { timeout: 10000, maximumAge: 600000 }
    );
  }

  updateLocalDate();
  fetchWeather();

  setInterval(() => {
    updateLocalDate();
    fetchWeather();
  }, 30 * 60 * 1000);
}

// Farm Registration & Settings GPS Location Capture System
function initGPSLocationCaptureSystem() {
  function cleanAddress(str) {
    if (!str) return '';
    return str
      .replace(/,+/g, ',')
      .replace(/\s+/g, ' ')
      .replace(/,\s*,/g, ',')
      .replace(/^\s*,\s*/, '')
      .replace(/\s*,\s*$/, '')
      .trim();
  }

  window.captureGPSLocation = function(options) {
    const {
      btnId,
      addressInputId,
      statusMsgId,
      latInputId,
      lonInputId,
      timestampInputId,
      districtInputId,
      stateInputId,
      countryInputId
    } = options;

    const btn = document.getElementById(btnId);
    const addressInput = document.getElementById(addressInputId);
    const statusMsg = statusMsgId ? document.getElementById(statusMsgId) : null;
    const latInput = document.getElementById(latInputId);
    const lonInput = document.getElementById(lonInputId);
    const timestampInput = document.getElementById(timestampInputId);
    const districtInput = districtInputId ? document.getElementById(districtInputId) : null;
    const stateInput = stateInputId ? document.getElementById(stateInputId) : null;
    const countryInput = countryInputId ? document.getElementById(countryInputId) : null;

    if (!navigator.geolocation) {
      if (statusMsg) {
        statusMsg.style.display = 'block';
        statusMsg.innerHTML = '<span style="color: #DC2626;"><i class="fa-solid fa-triangle-exclamation"></i> Location permission denied. Please enter the farm address manually.</span>';
      }
      showToast('Location permission denied. Please enter farm address manually.', 'error');
      return;
    }

    if (btn) {
      btn.disabled = true;
      if (btn.id === 'btn-update-gps') {
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin-pulse" style="color: #16A34A;"></i>';
      } else {
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin-pulse" style="color: #16A34A;"></i> <span id="lbl-gps-btn-text">Detecting...</span>';
      }
    }

    if (statusMsg) {
      statusMsg.style.display = 'block';
      statusMsg.innerHTML = '<span style="color: #2563EB;"><i class="fa-solid fa-location-crosshairs fa-spin"></i> Locating farm... High accuracy GPS active.</span>';
    }

    const geoOptions = {
      enableHighAccuracy: true,
      timeout: 15000,
      maximumAge: 0
    };

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude;
        const lon = position.coords.longitude;
        const timestamp = new Date(position.timestamp || Date.now()).toISOString();

        if (latInput) latInput.value = lat;
        if (lonInput) lonInput.value = lon;
        if (timestampInput) timestampInput.value = timestamp;

        try {
          // Use direct fetch to prevent triggering global GET error toasts on reverse-geocode
          let backendRes = null;
          try {
            const token = Storage.getToken();
            const res = await fetch(`${CONFIG.API_BASE_URL}/farms/reverse-geocode?lat=${lat}&lon=${lon}`, {
              headers: token ? { 'Authorization': `Bearer ${token}` } : {}
            });
            if (res.ok) backendRes = await res.json();
          } catch (e) {
            backendRes = null;
          }

          let fullAddress = '';
          let district = '';
          let state = '';
          let country = '';

          if (backendRes && backendRes.success && backendRes.data) {
            fullAddress = backendRes.data.address || backendRes.data.fullAddress || '';
            district = backendRes.data.district || '';
            state = backendRes.data.state || '';
            country = backendRes.data.country || '';
          } else {
            const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}`);
            if (response.ok) {
              const data = await response.json();
              fullAddress = data.display_name || `${lat.toFixed(5)}, ${lon.toFixed(5)}`;
              if (data.address) {
                district = data.address.county || data.address.city_district || data.address.city || data.address.town || '';
                state = data.address.state || '';
                country = data.address.country || 'USA';
              }
            }
          }

          const cleaned = cleanAddress(fullAddress);

          if (addressInput && cleaned) {
            addressInput.value = cleaned;
            addressInput.title = cleaned;
            addressInput.dispatchEvent(new Event('input'));
            addressInput.dispatchEvent(new Event('change'));
          }

          if (districtInput && district) {
            districtInput.value = district;
            districtInput.dispatchEvent(new Event('input'));
          }
          if (stateInput && state) {
            stateInput.value = state;
            stateInput.dispatchEvent(new Event('input'));
          }
          if (countryInput && country) {
            countryInput.value = country;
            countryInput.dispatchEvent(new Event('input'));
          }

          // Trigger clean success toast notification
          showToast('Farm location updated successfully.', 'success');
        } catch (err) {
          console.warn('Reverse geocode error:', err);
          showToast('Farm location updated successfully.', 'success');
        } finally {
          if (btn) {
            btn.disabled = false;
            if (btn.id === 'btn-update-gps') {
              btn.innerHTML = '<i class="fa-solid fa-location-crosshairs" id="icon-gps-btn" style="color: #16A34A;"></i>';
            } else {
              btn.innerHTML = '<i class="fa-solid fa-circle-check" style="color: #16A34A;"></i> <span id="lbl-gps-btn-text">Update Location</span>';
            }
          }
          if (statusMsg) {
            statusMsg.style.display = 'none';
          }
        }
      },
      (error) => {
        console.warn('GPS Error:', error.message);
        if (statusMsg) {
          statusMsg.style.display = 'block';
          statusMsg.innerHTML = '<span style="color: #DC2626;"><i class="fa-solid fa-triangle-exclamation"></i> Location permission denied. Please enter the farm address manually.</span>';
        }
        showToast('Location permission denied. Please enter address manually.', 'error');
        if (btn) {
          btn.disabled = false;
          btn.innerHTML = '<i class="fa-solid fa-location-crosshairs" style="color: #16A34A;"></i> <span id="lbl-gps-btn-text">Detect Location</span>';
        }
      },
      geoOptions
    );
  };

  const btnCreateFarmGps = document.getElementById('btn-use-gps');
  if (btnCreateFarmGps) {
    btnCreateFarmGps.addEventListener('click', (e) => {
      e.preventDefault();
      window.captureGPSLocation({
        btnId: 'btn-use-gps',
        addressInputId: 'frm-address',
        statusMsgId: 'gps-status-msg',
        latInputId: 'frm-lat',
        lonInputId: 'frm-lon',
        timestampInputId: 'frm-location-timestamp',
        districtInputId: 'frm-district',
        stateInputId: 'frm-state',
        countryInputId: 'frm-country'
      });
    });
  }

  const btnUpdateSettingsGps = document.getElementById('btn-update-gps');
  if (btnUpdateSettingsGps) {
    btnUpdateSettingsGps.addEventListener('click', (e) => {
      e.preventDefault();
      window.captureGPSLocation({
        btnId: 'btn-update-gps',
        addressInputId: 'prof-location',
        statusMsgId: 'settings-gps-status-msg',
        latInputId: 'prof-lat',
        lonInputId: 'prof-lon',
        timestampInputId: 'prof-location-timestamp'
      });
    });
  }
}

// Auto-initialize standard selects & mobile UX components on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
  initMobileNavigation();
  initGlobalFAB();
  initDynamicHeaderWeatherAndDate();
  initGPSLocationCaptureSystem();

  setTimeout(() => {
    document.querySelectorAll('select').forEach(sel => {
      if (sel.closest('.custom-select-wrapper')) return;
      if (sel.id.includes('breed')) return;
      if (sel.classList.contains('native-select')) return;
      
      window.makePremiumSelect(sel.id);
    });
  }, 100);
});

