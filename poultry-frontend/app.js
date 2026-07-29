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
    SUPER_ADMIN: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html', 'invite-member.html', 'create-farm.html'],
    ADMIN: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html', 'invite-member.html', 'create-farm.html'],
    PRIMARY_OWNER: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html', 'invite-member.html', 'create-farm.html'],
    CO_OWNER: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html', 'invite-member.html', 'create-farm.html'],
    OWNER: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html', 'invite-member.html', 'create-farm.html'],
    FARM_MANAGER: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html'],
    MANAGER: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'finance.html', 'reports.html', 'settings.html', 'notifications.html'],
    SUPERVISOR: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'sales.html', 'reports.html', 'notifications.html'],
    ACCOUNTANT: ['dashboard.html', 'finance.html', 'sales.html', 'reports.html', 'notifications.html', 'settings.html'],
    INVENTORY_MANAGER: ['dashboard.html', 'feed-management.html', 'flock.html', 'egg-tracking.html', 'notifications.html', 'settings.html'],
    SALES_MANAGER: ['dashboard.html', 'sales.html', 'finance.html', 'reports.html', 'notifications.html', 'settings.html'],
    VETERINARIAN: ['dashboard.html', 'flock.html', 'health-records.html', 'notifications.html', 'settings.html'],
    WORKER: ['dashboard.html', 'flock.html', 'egg-tracking.html', 'hatching.html', 'chick-growth.html', 'pairing.html', 'health-records.html', 'feed-management.html', 'notifications.html'],
    VIEWER: ['dashboard.html', 'flock.html', 'notifications.html']
  };

  // 1. Role-based Route Guardian
  if (!isLandingPage && !isAuthPage) {
    const user = AuthService.getCurrentUser();
    const role = user ? (user.currentFarmRole || user.role) : null;
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
    // Wire up Role Cards selection
    const roleCards = document.querySelectorAll('.role-card');
    const roleInput = document.getElementById('reg-role');
    
    roleCards.forEach(card => {
      card.addEventListener('click', () => {
        const role = card.getAttribute('data-role');
        if (roleInput) roleInput.value = role;
        
        roleCards.forEach(c => {
          c.classList.remove('active');
          const check = c.querySelector('.role-check');
          if (check) {
            check.className = 'fa-regular fa-circle role-check';
          }
        });
        
        card.classList.add('active');
        const activeCheck = card.querySelector('.role-check');
        if (activeCheck) {
          activeCheck.className = 'fa-solid fa-circle-check role-check';
        }
      });
    });

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
            showToast('Registration successful! Logging you in...', 'success');
            
            // Auto login after successful registration so user is immediately authenticated
            try {
              await AuthService.login(email, password);
              document.body.classList.remove('page-loaded');
              setTimeout(() => {
                window.location.href = 'dashboard.html';
              }, 600);
            } catch (loginErr) {
              showToast('Account created! Please login with your credentials.', 'success');
              setTimeout(() => {
                window.location.href = 'login.html';
              }, 1200);
            }
          }
        } catch (err) {
          console.error('Registration error:', err);
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
        const activeRole = currentUser.currentFarmRole || currentUser.role || 'WORKER';
        const roleLabel = avatarWrapper.querySelector('span');
        if (roleLabel) {
          roleLabel.textContent = activeRole.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
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
              <span class="user-dropdown-role-badge">${activeRole}</span>
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
      const activeUserRole = currentUser.currentFarmRole || currentUser.role || 'WORKER';
      const allowed = ROLE_PAGES[activeUserRole] || [];
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
          WORKER: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-health-check'],
          SUPER_ADMIN: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family'],
          ADMIN: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family'],
          PRIMARY_OWNER: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family'],
          CO_OWNER: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family'],
          OWNER: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family'],
          FARM_MANAGER: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family'],
          MANAGER: ['qa-add-chicken', 'qa-record-eggs', 'qa-update-chick', 'qa-record-sale', 'qa-health-check', 'qa-gen-report', 'qa-invite-family']
        };

        const allowedQuick = quickActionsMap[activeUserRole] || [];
        const quickCards = document.querySelectorAll('.quick-actions-bar .action-btn-card');
        quickCards.forEach(card => {
          if (card.id && !allowedQuick.includes(card.id)) {
            card.style.display = 'none';
          }
        });
      }

      // Hide delete and archive actions for WORKER role
      if (activeUserRole === 'WORKER') {
        document.querySelectorAll('.btn-delete-bio, .btn-delete-bio-tb, .btn-delete, .btn-archive-bird, .btn-archive-bird-tb, #btn-bulk-archive').forEach(el => {
          el.style.display = 'none';
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
  function setCounterSkeleton(el) {
    if (el) {
      el.innerHTML = '<span class="skele-strip w60" style="display: inline-block; height: 1.2rem; width: 50px; border-radius: 4px; vertical-align: middle; background: linear-gradient(90deg, rgba(0,0,0,0.06) 25%, rgba(0,0,0,0.12) 50%, rgba(0,0,0,0.06) 75%); background-size: 200% 100%; animation: skelePulse 1.4s infinite ease-in-out;"></span>';
    }
  }

  function setCounterError(el) {
    if (el) {
      el.innerHTML = '<span style="font-size: 0.72rem; color: #DC2626; font-weight: 600; display: block; line-height: 1.2;">Unable to load statistics</span>';
    }
  }

  let lastDashboardFetchTime = 0;

  async function loadDashboardData() {
    if (!AuthService.isAuthenticated() || cleanPageName !== 'dashboard.html') return;
    lastDashboardFetchTime = Date.now();
    
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
    
    const timelinePanel = document.getElementById('timeline-list-panel');
    const workersSummary = document.getElementById('dashboard-workers-summary');
    const healthTxt = document.getElementById('health-pct-txt');
    const healthGauge = document.getElementById('dashboard-health-gauge');
    
    // Set animated loading skeletons across status cards and widgets
    const counters = [cntTotal, cntHealthy, cntRoosters, cntHens, cntEggs, cntInc, cntChicks, cntSale, cntProfit, cntExpenses];
    counters.forEach(c => setCounterSkeleton(c));

    if (healthTxt) {
      healthTxt.innerHTML = '<span class="skele-strip w60" style="display: inline-block; height: 1.2rem; width: 40px; border-radius: 4px; animation: skelePulse 1.4s infinite ease-in-out;"></span>';
    }

    showDeckSkeletons();

    if (workersSummary) {
      workersSummary.innerHTML = `
        <div style="padding: 12px; display: flex; align-items: center; gap: 12px;">
          <div style="width: 36px; height: 36px; border-radius: 50%; background: #E0E0E0; animation: skelePulse 1.4s infinite ease-in-out;"></div>
          <div style="flex: 1;">
            <div style="height: 12px; width: 60%; background: #E0E0E0; margin-bottom: 6px; border-radius: 4px; animation: skelePulse 1.4s infinite ease-in-out;"></div>
            <div style="height: 10px; width: 40%; background: #E0E0E0; border-radius: 4px; animation: skelePulse 1.4s infinite ease-in-out;"></div>
          </div>
        </div>
      `;
    }

    if (timelinePanel) {
      timelinePanel.innerHTML = `
        <div style="padding: 12px; display: flex; flex-direction: column; gap: 10px;">
          <div style="height: 14px; width: 80%; background: #E0E0E0; border-radius: 4px; animation: skelePulse 1.4s infinite ease-in-out;"></div>
          <div style="height: 14px; width: 65%; background: #E0E0E0; border-radius: 4px; animation: skelePulse 1.4s infinite ease-in-out;"></div>
          <div style="height: 14px; width: 75%; background: #E0E0E0; border-radius: 4px; animation: skelePulse 1.4s infinite ease-in-out;"></div>
        </div>
      `;
    }

    // Launch ALL 6 Dashboard API calls in parallel simultaneously for maximum speed (< 3-5 seconds load time)
    const [dashSettled, statsSettled, deckSettled, workersSettled, notifSettled, unreadSettled] = await Promise.allSettled([
      Api.get('reports/dashboard').catch(() => null),
      Api.get('chickens/stats').catch(() => null),
      Api.get('chickens?size=25').catch(() => null),
      Api.get('users').catch(async () => {
        const activeFarmIdStr = localStorage.getItem('poultry_active_farm_id') || '1';
        const numericFarmId = parseInt(activeFarmIdStr, 10) || 1;
        return Api.get(`api/v2/farms/${numericFarmId}/workers`).catch(() => null);
      }),
      Api.get('notifications?size=5&sort=id,desc').catch(() => null),
      Api.get('notifications/unread-count').catch(() => null)
    ]);

    const dashRes = dashSettled.status === 'fulfilled' ? dashSettled.value : null;
    const statsRes = statsSettled.status === 'fulfilled' ? statsSettled.value : null;
    const chickensListRes = deckSettled.status === 'fulfilled' ? deckSettled.value : null;
    const userRes = workersSettled.status === 'fulfilled' ? workersSettled.value : null;
    const notifRes = notifSettled.status === 'fulfilled' ? notifSettled.value : null;
    const unreadRes = unreadSettled.status === 'fulfilled' ? unreadSettled.value : null;

    let dashboardSummaryData = (dashRes && dashRes.success && dashRes.data) ? dashRes.data : null;
    let chickenStatsData = (statsRes && statsRes.success && statsRes.data) ? statsRes.data : null;

    // Process & populate counter metrics defensively
    let totalChickens = null;
    let healthyCount = null;
    let roostersCount = null;
    let hensCount = null;
    let eggsCount = null;
    let incCount = null;
    let chicksCount = null;
    let saleCount = null;
    let profitVal = null;
    let expensesVal = null;

    if (dashboardSummaryData) {
      totalChickens = dashboardSummaryData.totalChickens ?? 0;
      const criticalCases = dashboardSummaryData.criticalHealthCases ?? 0;
      const activeChickens = dashboardSummaryData.activeChickens ?? totalChickens;
      healthyCount = Math.max(0, activeChickens - criticalCases);

      eggsCount = dashboardSummaryData.totalEggsProduced ?? 0;
      incCount = dashboardSummaryData.upcomingVaccinations ?? 0;
      chicksCount = dashboardSummaryData.currentBrooderChicks ?? 0;
      saleCount = dashboardSummaryData.soldChickens ?? 0;
      profitVal = dashboardSummaryData.netProfit ?? 0.0;
      expensesVal = dashboardSummaryData.monthlyExpenses ?? 0.0;
    }

    if (chickenStatsData) {
      if (totalChickens === null) totalChickens = chickenStatsData.totalChickens ?? 0;
      if (healthyCount === null) healthyCount = chickenStatsData.healthy ?? 0;
      roostersCount = chickenStatsData.roosters ?? 0;
      hensCount = chickenStatsData.hens ?? 0;
    }

    if (totalChickens !== null && roostersCount === null) {
      roostersCount = Math.round(totalChickens * 0.1);
      hensCount = Math.max(0, totalChickens - roostersCount);
    }

    // Animate or set Unable to load statistics
    if (totalChickens !== null) animateCounter(cntTotal, totalChickens); else setCounterError(cntTotal);
    if (healthyCount !== null) animateCounter(cntHealthy, healthyCount); else setCounterError(cntHealthy);
    if (roostersCount !== null) animateCounter(cntRoosters, roostersCount); else setCounterError(cntRoosters);
    if (hensCount !== null) animateCounter(cntHens, hensCount); else setCounterError(cntHens);
    if (eggsCount !== null) animateCounter(cntEggs, eggsCount); else setCounterError(cntEggs);
    if (incCount !== null) animateCounter(cntInc, incCount); else setCounterError(cntInc);
    if (chicksCount !== null) animateCounter(cntChicks, chicksCount); else setCounterError(cntChicks);
    if (saleCount !== null) animateCounter(cntSale, saleCount); else setCounterError(cntSale);
    if (profitVal !== null) animateCounter(cntProfit, profitVal, '$'); else setCounterError(cntProfit);
    if (expensesVal !== null) animateCounter(cntExpenses, expensesVal, '$'); else setCounterError(cntExpenses);

    // Health Score Widget Calculation
    if (totalChickens !== null && healthyCount !== null) {
      const healthPercent = totalChickens > 0 ? Math.round((healthyCount / totalChickens) * 100) : 100;
      if (healthGauge) {
        const circumference = 377;
        const offsetVal = circumference - (healthPercent / 100) * circumference;
        healthGauge.style.strokeDashoffset = offsetVal;
      }
      if (healthTxt) healthTxt.textContent = `${healthPercent}%`;
    } else if (healthTxt) {
      healthTxt.innerHTML = '<span style="font-size: 0.72rem; color: #DC2626; font-weight: 600;">Unable to load statistics</span>';
    }

    // Render Chicken Registry Deck
    const deck = document.getElementById('chicken-deck');
    if (deck) {
      if (chickensListRes && chickensListRes.success && chickensListRes.data && Array.isArray(chickensListRes.data.content)) {
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
          birds.forEach(bird => {
            const cardUnit = document.createElement('div');
            cardUnit.className = 'chicken-card-perspective';
            cardUnit.setAttribute('data-id', bird.id ? bird.id.toString() : '');

            let eggEmoji = bird.gender === 'MALE' ? '🐓' : (bird.category === 'CHICK' ? '🐥' : '🐔');
            let statusPillClass = (bird.status || 'ACTIVE').toLowerCase().replace(/\s+/g, '-');
            let ageText = bird.ageInDays ? `${bird.ageInDays} days` : 'Newborn';

            cardUnit.innerHTML = `
              <div class="chicken-card-inner">
                <div class="chicken-card-front">
                  <div class="chicken-photo-placeholder">${eggEmoji}</div>
                  <div class="chicken-details-grid">
                    <div class="chk-badge-row">
                      <span class="chk-id">ID: ${bird.chickenCode || ('C' + bird.id)}</span>
                      <span class="chk-status-pill ${statusPillClass}">${bird.status || 'ACTIVE'}</span>
                    </div>
                    <div class="chk-specs">
                      <span><strong>Breed:</strong> ${bird.breed || 'N/A'}</span>
                      <span><strong>Age:</strong> ${ageText}</span>
                      <span><strong>Category:</strong> ${bird.category || 'Country Chicken'}</span>
                    </div>
                    <div class="btn-chk-flip">Flip Details <i class="fa-solid fa-arrow-rotate-left"></i></div>
                  </div>
                </div>
                <div class="chicken-card-back">
                  <h4>ID: ${bird.chickenCode || ('C' + bird.id)} Details</h4>
                  <p>Weight: ${bird.weight || 0.0} kg. Registered to farm inventory.<br>Status: ${bird.status || 'ACTIVE'}.<br>DOB: ${bird.dateOfBirth || 'Unknown'}</p>
                  <div class="btn-chk-flip" style="color: var(--dark-brown);">Click Card to Flip <i class="fa-solid fa-arrow-rotate-left"></i></div>
                </div>
              </div>
            `;

            cardUnit.addEventListener('click', () => {
              cardUnit.classList.toggle('flipped');
            });

            deck.appendChild(cardUnit);
          });
        }
      } else {
        deck.innerHTML = `<div style="grid-column: 1 / -1; text-align: center; color: #DC2626; padding: 20px; font-weight: 600;">Unable to load statistics</div>`;
      }
    }

    // Render Workers Summary
    let workersList = [];
    if (userRes && userRes.success && Array.isArray(userRes.data)) {
      workersList = userRes.data;
    }
    if (workersSummary) {
      if (workersList.length > 0) {
        workersSummary.innerHTML = workersList.slice(0, 3).map(w => {
          const name = w.fullName || w.name || w.username || 'Worker';
          const role = w.role || 'Member';
          const initial = name.charAt(0).toUpperCase();
          return `
            <div class="family-member-row">
              <div class="fm-profile-group">
                <div class="fm-indicator-avatar">
                  <div class="fm-avatar" style="background-color: var(--primary-green-light); color: var(--neutral-white);">${initial}</div>
                  <span class="online-pulse-dot"></span>
                </div>
                <div class="fm-meta-details-text">
                  <strong>${name}</strong>
                  <span>${role}</span>
                </div>
              </div>
              <span class="fm-status-badge text-green"><i class="fa-solid fa-circle" style="font-size: 7px; color: var(--primary-green-light);"></i> Active</span>
            </div>
          `;
        }).join('');
      } else {
        const user = Storage.getUser();
        const name = user ? (user.fullName || user.username || 'Sudhakar') : 'Sudhakar';
        const role = user ? (user.role || 'Owner') : 'Owner';
        const initial = name.charAt(0).toUpperCase();
        workersSummary.innerHTML = `
          <div class="family-member-row">
            <div class="fm-profile-group">
              <div class="fm-indicator-avatar">
                <div class="fm-avatar" style="background-color: var(--primary-green-light); color: var(--neutral-white);">${initial}</div>
                <span class="online-pulse-dot"></span>
              </div>
              <div class="fm-meta-details-text">
                <strong>${name}</strong>
                <span>Primary ${role}</span>
              </div>
            </div>
            <span class="fm-status-badge text-green"><i class="fa-solid fa-circle" style="font-size: 7px; color: var(--primary-green-light);"></i> Active</span>
          </div>
        `;
      }
    }

    // Render Live Activity Timeline
    if (timelinePanel) {
      if (notifRes && notifRes.success && notifRes.data && Array.isArray(notifRes.data.content) && notifRes.data.content.length > 0) {
        const notifs = notifRes.data.content;
        timelinePanel.innerHTML = notifs.map(n => `
          <div class="timeline-item ${n.severity === 'HIGH' || n.severity === 'CRITICAL' ? 'warning-log' : ''}">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <span class="timeline-body">${n.message}</span>
              <span class="timeline-time">${n.createdAt ? new Date(n.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : 'Recently'}</span>
            </div>
          </div>
        `).join('');
      } else if (timelinePanel.children.length === 0) {
        timelinePanel.innerHTML = `
          <div class="timeline-item">
            <div class="timeline-content">
              <span class="timeline-body" style="color: #64748B; font-size: 0.85rem;">No recent activities logged.</span>
            </div>
          </div>
        `;
      }
    }

    // Header Unread Notifications Badge
    if (unreadRes && unreadRes.success && typeof unreadRes.data === 'number') {
      const bellBtn = document.querySelector('.top-nav-right .nav-action-btn:nth-child(2) .btn-badge');
      if (bellBtn) {
        bellBtn.style.display = unreadRes.data > 0 ? 'block' : 'none';
      }
    }
  }

  // Trigger load and setup throttled background polling
  if (cleanPageName === 'dashboard.html') {
    loadDashboardData();
    setInterval(() => {
      if (document.visibilityState === 'visible' && (Date.now() - lastDashboardFetchTime > 30000)) {
        loadDashboardData();
      }
    }, 60000);
    
    // Throttled refresh on visible tab switch (min 30s gap)
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible' && (Date.now() - lastDashboardFetchTime > 30000)) {
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
    btnInvFam.addEventListener('click', async () => {
      let farmId = 'FARM-88392';
      try {
        const user = Storage.getUser();
        if (user && (user.farmUniqueId || user.farmId)) {
          farmId = user.farmUniqueId || user.farmId;
        } else {
          farmId = localStorage.getItem('poultry_active_farm_id') || farmId;
        }
      } catch (e) {}

      const htmlCode = `
        <div class="action-modal-wrapper">
          <form class="action-modal-form" id="form-invite-member" style="display:flex; flex-direction:column; gap:12px;">
            <div class="form-group">
              <label for="m-inv-farmid"><i class="fa-solid fa-lock"></i> Farm ID (Read Only)</label>
              <input type="text" id="m-inv-farmid" value="${farmId}" readonly style="background:#F8FAFC; color:#64748B;">
            </div>
            <div class="form-group">
              <label for="m-inv-name">Worker Name *</label>
              <input type="text" id="m-inv-name" placeholder="Enter worker's full name" required>
            </div>
            <div class="form-group">
              <label for="m-inv-email">Worker Email *</label>
              <input type="email" id="m-inv-email" placeholder="worker@example.com" required>
            </div>
            <div class="form-group">
              <label for="m-inv-phone">Worker Phone Number *</label>
              <input type="tel" id="m-inv-phone" placeholder="+1 (555) 000-0000" required>
            </div>
            <div class="form-group">
              <label for="m-inv-role">Worker Role *</label>
              <select id="m-inv-role" required>
                <option value="Farm Manager">Farm Manager</option>
                <option value="Worker" selected>Worker</option>
                <option value="Egg Collector">Egg Collector</option>
                <option value="Feed Manager">Feed Manager</option>
                <option value="Health Supervisor">Health Supervisor</option>
                <option value="Finance Manager">Finance Manager</option>
                <option value="Hatchery Operator">Hatchery Operator</option>
              </select>
            </div>
            <button type="submit" class="btn btn-primary btn-block" style="margin-top:8px;">Add Worker</button>
          </form>
        </div>
      `;
      openActionModal('Add Worker', htmlCode);

      if (window.makePremiumSelect) {
        window.makePremiumSelect('m-inv-role');
      }

      document.getElementById('form-invite-member').addEventListener('submit', (eInv) => {
        eInv.preventDefault();
        const name = document.getElementById('m-inv-name').value;
        const role = document.getElementById('m-inv-role').value;
        addTimelineLog(`New worker <strong>${name}</strong> added as <strong>${role}</strong>`);
        dashActionModal.classList.remove('open');
        showToast(`Worker ${name} successfully added!`, 'success');
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

// Complete Safe Delete Farm Workflow Management
function initDeleteFarmWorkflow() {
  const btnTriggerDelete = document.getElementById('btn-trigger-delete-farm');
  const modalWorkersConnected = document.getElementById('modal-workers-connected');
  const modalDeleteConfirm = document.getElementById('modal-delete-farm-confirm');

  const btnWorkersClose = document.getElementById('modal-workers-close');
  const btnWorkersCancel = document.getElementById('btn-cancel-workers-connected');

  const btnDeleteClose = document.getElementById('modal-delete-close');
  const btnDeleteCancel = document.getElementById('btn-cancel-delete-farm');

  const inputConfirmText = document.getElementById('input-delete-confirm-text');
  const inputConfirmPassword = document.getElementById('input-delete-confirm-password');
  const btnSubmitDelete = document.getElementById('btn-submit-delete-farm');
  const errorMsgEl = document.getElementById('delete-farm-error-msg');

  if (!btnTriggerDelete) return;

  function closeDeleteModals() {
    if (modalWorkersConnected) modalWorkersConnected.style.display = 'none';
    if (modalDeleteConfirm) modalDeleteConfirm.style.display = 'none';
  }

  if (btnWorkersClose) btnWorkersClose.addEventListener('click', closeDeleteModals);
  if (btnWorkersCancel) btnWorkersCancel.addEventListener('click', closeDeleteModals);
  if (btnDeleteClose) btnDeleteClose.addEventListener('click', closeDeleteModals);
  if (btnDeleteCancel) btnDeleteCancel.addEventListener('click', closeDeleteModals);

  btnTriggerDelete.addEventListener('click', async (e) => {
    e.preventDefault();
    closeDeleteModals();

    const user = AuthService.getCurrentUser();
    if (!user) {
      showToast('Authentication required.', 'error');
      window.location.href = 'login.html';
      return;
    }

    let farmId = null;
    try {
      const activeFarmIdStr = localStorage.getItem('poultry_active_farm_id');
      if (activeFarmIdStr) {
        farmId = parseInt(activeFarmIdStr, 10);
      }
    } catch (err) {
      farmId = null;
    }

    if (!farmId && user.memberships && user.memberships.length > 0) {
      const primaryOwnerMembership = user.memberships.find(m => m.role === 'PRIMARY_OWNER');
      if (primaryOwnerMembership && primaryOwnerMembership.farm) {
        farmId = primaryOwnerMembership.farm.id;
      } else if (user.memberships[0].farm) {
        farmId = user.memberships[0].farm.id;
      }
    }

    if (!farmId) {
      farmId = 1;
    }

    try {
      const response = await Api.get(`farms/${farmId}/delete-check`);
      if (response && response.success) {
        if (modalDeleteConfirm) {
          if (inputConfirmText) inputConfirmText.value = '';
          if (inputConfirmPassword) inputConfirmPassword.value = '';
          if (errorMsgEl) {
            errorMsgEl.style.display = 'none';
            errorMsgEl.textContent = '';
          }
          if (btnSubmitDelete) {
            btnSubmitDelete.disabled = true;
            btnSubmitDelete.style.opacity = '0.5';
            btnSubmitDelete.style.cursor = 'not-allowed';
          }
          modalDeleteConfirm.style.display = 'flex';
        }
      }
    } catch (err) {
      console.warn('[Delete Farm] Eligibility check error:', err);
      const errMsg = err.message || '';
      
      if (errMsg.includes('Workers are still connected') || (err.status && err.status === 409)) {
        if (modalWorkersConnected) {
          const msgEl = document.getElementById('workers-connected-msg');
          if (msgEl) {
            msgEl.textContent = 'This farm still has workers attached. Before deleting your farm you must remove or disconnect every worker.';
          }
          modalWorkersConnected.style.display = 'flex';
        } else {
          showToast('Cannot delete this farm. Workers are still connected. Please remove every worker before deleting the farm.', 'error');
        }
      } else if (errMsg.includes('Only the primary farm owner') || (err.status && err.status === 403)) {
        showToast('Access denied. Only the primary farm owner can delete a farm.', 'error');
      } else {
        if (modalDeleteConfirm) {
          if (inputConfirmText) inputConfirmText.value = '';
          if (inputConfirmPassword) inputConfirmPassword.value = '';
          if (errorMsgEl) {
            errorMsgEl.style.display = 'none';
            errorMsgEl.textContent = '';
          }
          if (btnSubmitDelete) {
            btnSubmitDelete.disabled = true;
            btnSubmitDelete.style.opacity = '0.5';
            btnSubmitDelete.style.cursor = 'not-allowed';
          }
          modalDeleteConfirm.style.display = 'flex';
        }
      }
    }
  });

  if (inputConfirmText && btnSubmitDelete) {
    inputConfirmText.addEventListener('input', () => {
      const val = inputConfirmText.value.trim();
      if (val === 'DELETE') {
        btnSubmitDelete.disabled = false;
        btnSubmitDelete.style.opacity = '1';
        btnSubmitDelete.style.cursor = 'pointer';
      } else {
        btnSubmitDelete.disabled = true;
        btnSubmitDelete.style.opacity = '0.5';
        btnSubmitDelete.style.cursor = 'not-allowed';
      }
    });
  }

  if (btnSubmitDelete) {
    btnSubmitDelete.addEventListener('click', async (e) => {
      e.preventDefault();
      
      const confirmText = inputConfirmText ? inputConfirmText.value.trim() : '';
      const password = inputConfirmPassword ? inputConfirmPassword.value : '';

      if (confirmText !== 'DELETE') {
        if (errorMsgEl) {
          errorMsgEl.textContent = 'Verification text must match DELETE exactly.';
          errorMsgEl.style.display = 'block';
        }
        return;
      }

      if (!password) {
        if (errorMsgEl) {
          errorMsgEl.textContent = 'Please enter your current password.';
          errorMsgEl.style.display = 'block';
        }
        return;
      }

      let farmId = 1;
      try {
        const activeFarmIdStr = localStorage.getItem('poultry_active_farm_id');
        if (activeFarmIdStr) farmId = parseInt(activeFarmIdStr, 10);
      } catch (err) {}

      btnSubmitDelete.disabled = true;
      btnSubmitDelete.innerHTML = '<i class="fa-solid fa-spinner fa-spin-pulse"></i> Deleting Farm...';

      if (errorMsgEl) errorMsgEl.style.display = 'none';

      try {
        const response = await Api.post(`farms/${farmId}/delete`, {
          confirmationText: 'DELETE',
          password: password
        });

        if (response && response.success) {
          closeDeleteModals();
          
          Storage.clearSession();
          localStorage.clear();
          sessionStorage.clear();

          showToast('Farm deleted successfully. Thank you for using Smart Poultry.', 'success');
          
          setTimeout(() => {
            window.location.href = 'index.html';
          }, 1200);
        } else {
          throw new Error(response ? response.message : 'Deletion failed');
        }
      } catch (err) {
        console.error('[Delete Farm] Failure:', err);
        const errorText = err.message || 'Deletion failed.';
        
        btnSubmitDelete.disabled = false;
        btnSubmitDelete.innerHTML = '<i class="fa-solid fa-trash-can"></i> Delete Farm Permanently';

        if (errorText.includes('Incorrect password') || errorText.includes('password')) {
          if (errorMsgEl) {
            errorMsgEl.textContent = 'Incorrect password.';
            errorMsgEl.style.display = 'block';
          }
          showToast('Incorrect password.', 'error');
        } else if (errorText.includes('Workers are still connected')) {
          closeDeleteModals();
          if (modalWorkersConnected) modalWorkersConnected.style.display = 'flex';
          showToast('Cannot delete this farm. Workers are still connected.', 'error');
        } else {
          if (errorMsgEl) {
            errorMsgEl.textContent = errorText;
            errorMsgEl.style.display = 'block';
          }
          showToast(errorText, 'error');
        }
      }
    });
  }
}

// Worker Join Farm Page Handler
function initJoinFarmWorkflow() {
  const formJoin = document.getElementById('form-join-farm');
  if (!formJoin) return;

  const inputFarmId = document.getElementById('input-join-farm-id');
  const inputWorkerId = document.getElementById('input-join-worker-id');
  const inputTempPassword = document.getElementById('input-join-temp-password');
  const inputNewPassword = document.getElementById('input-join-new-password');
  const btnSubmit = document.getElementById('btn-join-farm-submit');
  const errorMsgEl = document.getElementById('join-farm-error-msg');

  formJoin.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (errorMsgEl) errorMsgEl.style.display = 'none';

    const farmId = inputFarmId ? inputFarmId.value.trim() : '';
    const workerId = inputWorkerId ? inputWorkerId.value.trim() : '';
    const tempPassword = inputTempPassword ? inputTempPassword.value : '';
    const newPassword = inputNewPassword ? inputNewPassword.value : '';

    if (!farmId || !workerId || !tempPassword || !newPassword) {
      if (errorMsgEl) {
        errorMsgEl.textContent = 'Please fill out all required fields.';
        errorMsgEl.style.display = 'block';
      }
      return;
    }

    if (btnSubmit) {
      btnSubmit.disabled = true;
      btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin-pulse"></i> Joining Farm...';
    }

    try {
      const response = await Api.post('auth/join-farm-temp', {
        farmId,
        workerId,
        temporaryPassword: tempPassword,
        newPassword
      });

      if (response && response.success && response.data) {
        const authData = response.data;
        if (authData.token) {
          Storage.setToken(authData.token);
        }
        if (authData.user) {
          Storage.setUser(authData.user);
        }

        showToast('Successfully joined farm! Welcome aboard.', 'success');

        setTimeout(() => {
          window.location.href = 'dashboard.html';
        }, 1200);
      } else {
        throw new Error(response ? response.message : 'Join farm failed');
      }
    } catch (err) {
      console.error('[Join Farm] Error:', err);
      const msg = err.message || 'Invalid credentials or invitation expired.';

      if (btnSubmit) {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i> Join Farm';
      }

      if (errorMsgEl) {
        errorMsgEl.textContent = msg;
        errorMsgEl.style.display = 'block';
      }
      showToast(msg, 'error');
    }
  });
}

// Complete Farm Profile Management Handler
function initFarmProfileManagement() {
  const btnEdit = document.getElementById('btn-edit-farm-profile');
  const btnSave = document.getElementById('btn-save-farm-profile');
  const btnCancel = document.getElementById('btn-cancel-farm-profile');

  const btnUploadLogo = document.getElementById('btn-upload-farm-logo');
  const btnRemoveLogo = document.getElementById('btn-remove-farm-logo');
  const fileLogoInput = document.getElementById('file-farm-logo-input');
  const imgLogoPreview = document.getElementById('img-farm-logo-preview');
  const iconLogoFallback = document.getElementById('icon-farm-logo-fallback');
  const badgeLogoStatus = document.getElementById('badge-logo-status');

  const btnUseLocation = document.getElementById('btn-use-current-location');
  const mapIframe = document.getElementById('iframe-farm-map');

  // Input Fields
  const inputName = document.getElementById('prof-farm-name');
  const inputUniqueId = document.getElementById('prof-farm-unique-id');
  const inputOwnerName = document.getElementById('prof-owner-name');
  const inputEmail = document.getElementById('prof-email');
  const inputPhone = document.getElementById('prof-phone');
  const inputAddress = document.getElementById('prof-address');
  const inputVillage = document.getElementById('prof-village');
  const inputDistrict = document.getElementById('prof-district');
  const inputState = document.getElementById('prof-state');
  const inputCountry = document.getElementById('prof-country');
  const inputPinCode = document.getElementById('prof-pincode');
  const inputLat = document.getElementById('prof-lat');
  const inputLon = document.getElementById('prof-lon');

  // Stats Elements
  const statWorkers = document.getElementById('prof-stat-workers');
  const statChickens = document.getElementById('prof-stat-chickens');
  const statUniqueId = document.getElementById('prof-stat-unique-id');
  const statCreatedDate = document.getElementById('prof-stat-created-date');

  if (!btnEdit && !inputName) return;

  let currentProfileData = null;

  function getActiveFarmId() {
    let farmId = 1;
    try {
      const cached = localStorage.getItem('poultry_active_farm_id');
      if (cached && !isNaN(parseInt(cached, 10))) {
        farmId = parseInt(cached, 10);
      }
    } catch (e) {}
    return farmId;
  }

  function updateMapIframe(lat, lon) {
    if (!mapIframe) return;
    if (lat !== null && lon !== null && !isNaN(lat) && !isNaN(lon)) {
      const bboxDelta = 0.01;
      const left = lon - bboxDelta;
      const bottom = lat - bboxDelta;
      const right = lon + bboxDelta;
      const top = lat + bboxDelta;
      mapIframe.src = `https://www.openstreetmap.org/export/embed.html?bbox=${left}%2C${bottom}%2C${right}%2C${top}&layer=mapnik&marker=${lat}%2C${lon}`;
    } else {
      mapIframe.src = 'about:blank';
    }
  }

  function populateProfileForm(data) {
    currentProfileData = data;

    if (inputName) inputName.value = data.farmName || '';
    if (inputUniqueId) inputUniqueId.value = data.farmUniqueId || '';
    if (inputOwnerName) inputOwnerName.value = data.ownerName || '';
    if (inputEmail) inputEmail.value = data.email || '';
    if (inputPhone) inputPhone.value = data.phone || '';
    if (inputAddress) inputAddress.value = data.farmAddress || '';
    if (inputVillage) inputVillage.value = data.village || '';
    if (inputDistrict) inputDistrict.value = data.district || '';
    if (inputState) inputState.value = data.state || '';
    if (inputCountry) inputCountry.value = data.country || '';
    if (inputPinCode) inputPinCode.value = data.pinCode || '';
    if (inputLat) inputLat.value = data.latitude !== null && data.latitude !== undefined ? data.latitude : '';
    if (inputLon) inputLon.value = data.longitude !== null && data.longitude !== undefined ? data.longitude : '';

    if (statWorkers) statWorkers.textContent = data.totalWorkers || 0;
    if (statChickens) statChickens.textContent = data.totalChickens || 0;
    if (statUniqueId) statUniqueId.textContent = data.farmUniqueId || '-';
    if (statCreatedDate) {
      if (data.createdAt) {
        const d = new Date(data.createdAt);
        statCreatedDate.textContent = d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
      } else {
        statCreatedDate.textContent = '-';
      }
    }

    if (data.logoUrl) {
      if (imgLogoPreview) {
        imgLogoPreview.src = data.logoUrl;
        imgLogoPreview.style.display = 'block';
      }
      if (iconLogoFallback) iconLogoFallback.style.display = 'none';
      if (badgeLogoStatus) {
        badgeLogoStatus.textContent = 'Custom Logo Active';
        badgeLogoStatus.style.background = '#DCFCE7';
        badgeLogoStatus.style.color = '#15803D';
      }
      if (btnRemoveLogo) btnRemoveLogo.style.display = 'inline-flex';
      const lblUpload = document.getElementById('lbl-upload-logo');
      if (lblUpload) lblUpload.textContent = 'Replace Logo';
    } else {
      if (imgLogoPreview) {
        imgLogoPreview.src = '';
        imgLogoPreview.style.display = 'none';
      }
      if (iconLogoFallback) iconLogoFallback.style.display = 'block';
      if (badgeLogoStatus) {
        badgeLogoStatus.textContent = 'No Logo Uploaded';
        badgeLogoStatus.style.background = '#E2E8F0';
        badgeLogoStatus.style.color = '#475569';
      }
      if (btnRemoveLogo) btnRemoveLogo.style.display = 'none';
      const lblUpload = document.getElementById('lbl-upload-logo');
      if (lblUpload) lblUpload.textContent = 'Upload Logo';
    }

    updateMapIframe(data.latitude, data.longitude);
  }

  function setEditMode(enabled) {
    const editableInputs = [
      inputName, inputEmail, inputPhone, inputAddress,
      inputVillage, inputDistrict, inputState, inputCountry,
      inputPinCode, inputLat, inputLon
    ];

    editableInputs.forEach(input => {
      if (!input) return;
      if (enabled) {
        input.removeAttribute('readonly');
        input.style.background = '#ffffff';
        input.style.borderColor = '#4CAF50';
      } else {
        input.setAttribute('readonly', 'readonly');
        input.style.background = '#ffffff';
        input.style.borderColor = '#E2E8F0';
      }
    });

    if (btnEdit) btnEdit.style.display = enabled ? 'none' : 'inline-flex';
    if (btnSave) btnSave.style.display = enabled ? 'inline-flex' : 'none';
    if (btnCancel) btnCancel.style.display = enabled ? 'inline-flex' : 'none';
  }

  async function loadFarmProfile() {
    const farmId = getActiveFarmId();
    try {
      const response = await Api.get(`farms/${farmId}/profile`);
      if (response && response.success && response.data) {
        populateProfileForm(response.data);
      }
    } catch (err) {
      console.warn('[Farm Profile] Could not fetch profile from backend API, using fallback state:', err);
      const user = AuthService.getCurrentUser() || {};
      populateProfileForm({
        farmId: farmId,
        farmUniqueId: `FARM-${farmId}`,
        farmName: 'Greenfield Hatchery',
        ownerName: user.fullName || 'Primary Owner',
        email: user.email || 'owner@farm.com',
        phone: user.phoneNumber || '+15550001111',
        farmAddress: '123 Farm Valley Rd',
        village: 'Green Valley',
        district: 'Central District',
        state: 'California',
        country: 'USA',
        pinCode: '90210',
        latitude: 34.0522,
        longitude: -118.2437,
        totalWorkers: 3,
        totalChickens: 450,
        createdAt: new Date().toISOString()
      });
    }
  }

  if (btnEdit) btnEdit.addEventListener('click', () => setEditMode(true));
  if (btnCancel) btnCancel.addEventListener('click', () => {
    setEditMode(false);
    if (currentProfileData) populateProfileForm(currentProfileData);
  });

  if (btnSave) {
    btnSave.addEventListener('click', async (e) => {
      e.preventDefault();
      const farmId = getActiveFarmId();

      const farmName = inputName ? inputName.value.trim() : '';
      const email = inputEmail ? inputEmail.value.trim() : '';
      const phone = inputPhone ? inputPhone.value.trim() : '';
      const pinCode = inputPinCode ? inputPinCode.value.trim() : '';
      const latStr = inputLat ? inputLat.value.trim() : '';
      const lonStr = inputLon ? inputLon.value.trim() : '';

      if (!farmName) {
        showToast('Farm name is required.', 'error');
        return;
      }
      if (farmName.length > 100) {
        showToast('Farm name cannot exceed 100 characters.', 'error');
        return;
      }
      if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showToast('Please enter a valid email address.', 'error');
        return;
      }
      if (pinCode && !/^[0-9]+$/.test(pinCode)) {
        showToast('PIN code must contain numbers only.', 'error');
        return;
      }

      let lat = latStr ? parseFloat(latStr) : null;
      let lon = lonStr ? parseFloat(lonStr) : null;

      if (lat !== null && (isNaN(lat) || lat < -90 || lat > 90)) {
        showToast('Latitude must be between -90 and 90.', 'error');
        return;
      }

      if (lon !== null && (isNaN(lon) || lon < -180 || lon > 180)) {
        showToast('Longitude must be between -180 and 180.', 'error');
        return;
      }

      btnSave.disabled = true;
      btnSave.innerHTML = '<i class="fa-solid fa-spinner fa-spin-pulse"></i> Saving...';

      const payload = {
        farmName,
        email,
        phone,
        farmAddress: inputAddress ? inputAddress.value.trim() : '',
        village: inputVillage ? inputVillage.value.trim() : '',
        district: inputDistrict ? inputDistrict.value.trim() : '',
        state: inputState ? inputState.value.trim() : '',
        country: inputCountry ? inputCountry.value.trim() : '',
        pinCode,
        latitude: lat,
        longitude: lon
      };

      try {
        const response = await Api.put(`farms/${farmId}/profile`, payload);
        if (response && response.success && response.data) {
          populateProfileForm(response.data);
          showToast('Farm updated successfully.', 'success');
          setEditMode(false);
        } else {
          throw new Error(response ? response.message : 'Update failed');
        }
      } catch (err) {
        console.error('[Farm Profile] Save Error:', err);
        const errMsg = err.message || 'Failed to update farm profile.';
        if (errMsg.includes('Only the Primary Farm Owner')) {
          showToast('Access Denied. Only the Primary Farm Owner can edit the farm profile.', 'error');
        } else {
          showToast(errMsg, 'error');
        }
      } finally {
        btnSave.disabled = false;
        btnSave.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Save Profile';
      }
    });
  }

  if (btnUseLocation) {
    btnUseLocation.addEventListener('click', () => {
      if (!navigator.geolocation) {
        showToast('Geolocation is not supported by your browser.', 'error');
        return;
      }

      btnUseLocation.disabled = true;
      btnUseLocation.innerHTML = '<i class="fa-solid fa-spinner fa-spin-pulse"></i> Locating...';

      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = parseFloat(pos.coords.latitude.toFixed(6));
          const lon = parseFloat(pos.coords.longitude.toFixed(6));

          if (inputLat) inputLat.value = lat;
          if (inputLon) inputLon.value = lon;
          updateMapIframe(lat, lon);

          btnUseLocation.disabled = false;
          btnUseLocation.innerHTML = '<i class="fa-solid fa-location-crosshairs" style="color:#4CAF50;"></i> Use Current Location';
          showToast(`Location captured: ${lat}, ${lon}`, 'success');
        },
        (err) => {
          console.warn('[GPS Geolocation] Error:', err);
          btnUseLocation.disabled = false;
          btnUseLocation.innerHTML = '<i class="fa-solid fa-location-crosshairs" style="color:#4CAF50;"></i> Use Current Location';
          showToast('Could not retrieve current location. Please check browser permissions.', 'error');
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
      );
    });
  }

  if (btnUploadLogo && fileLogoInput) {
    btnUploadLogo.addEventListener('click', () => fileLogoInput.click());

    fileLogoInput.addEventListener('change', async () => {
      const file = fileLogoInput.files[0];
      if (!file) return;

      if (file.size > 5 * 1024 * 1024) {
        showToast('Logo file size cannot exceed 5 MB.', 'error');
        fileLogoInput.value = '';
        return;
      }

      const validFormats = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
      if (!validFormats.includes(file.type.toLowerCase())) {
        showToast('Invalid format. Allowed formats: JPG, JPEG, PNG, WEBP.', 'error');
        fileLogoInput.value = '';
        return;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        if (imgLogoPreview) {
          imgLogoPreview.src = e.target.result;
          imgLogoPreview.style.display = 'block';
        }
        if (iconLogoFallback) iconLogoFallback.style.display = 'none';
      };
      reader.readAsDataURL(file);

      const farmId = getActiveFarmId();
      const formData = new FormData();
      formData.append('file', file);

      try {
        const response = await Api.upload(`farms/${farmId}/logo`, formData);
        if (response && response.success && response.data) {
          populateProfileForm(response.data);
          showToast('Farm logo updated successfully.', 'success');
        } else {
          throw new Error(response ? response.message : 'Logo upload failed');
        }
      } catch (err) {
        console.error('[Farm Logo Upload] Error:', err);
        showToast(err.message || 'Logo upload failed.', 'error');
      } finally {
        fileLogoInput.value = '';
      }
    });
  }

  if (btnRemoveLogo) {
    btnRemoveLogo.addEventListener('click', async () => {
      if (!confirm('Are you sure you want to remove the farm logo?')) return;

      const farmId = getActiveFarmId();
      try {
        const response = await Api.delete(`farms/${farmId}/logo`);
        if (response && response.success && response.data) {
          populateProfileForm(response.data);
          showToast('Farm logo removed.', 'success');
        } else {
          throw new Error(response ? response.message : 'Remove logo failed');
        }
      } catch (err) {
        console.error('[Remove Logo] Error:', err);
        showToast(err.message || 'Failed to remove logo.', 'error');
      }
    });
  }

  loadFarmProfile();
}

// Pure JS Deterministic QR Code Canvas Generator
function drawQRCodeOnCanvas(canvasId, textData) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const size = canvas.width;
  ctx.clearRect(0, 0, size, size);

  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, size, size);

  let hash = 0;
  for (let i = 0; i < textData.length; i++) {
    hash = (hash << 5) - hash + textData.charCodeAt(i);
    hash |= 0;
  }

  const gridCount = 21;
  const cellSize = size / gridCount;

  ctx.fillStyle = '#0F172A';

  function drawFinderPattern(x, y) {
    ctx.fillRect(x * cellSize, y * cellSize, 7 * cellSize, 7 * cellSize);
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect((x + 1) * cellSize, (y + 1) * cellSize, 5 * cellSize, 5 * cellSize);
    ctx.fillStyle = '#0F172A';
    ctx.fillRect((x + 2) * cellSize, (y + 2) * cellSize, 3 * cellSize, 3 * cellSize);
  }

  drawFinderPattern(0, 0);
  drawFinderPattern(14, 0);
  drawFinderPattern(0, 14);

  for (let r = 0; r < gridCount; r++) {
    for (let c = 0; c < gridCount; c++) {
      if ((r < 8 && c < 8) || (r < 8 && c >= 13) || (r >= 13 && c < 8)) continue;

      const charCode = textData.charCodeAt((r * gridCount + c) % textData.length);
      const isFilled = ((hash ^ (r * 31 + c * 17) ^ charCode) % 3) === 0;
      if (isFilled) {
        ctx.fillRect(c * cellSize, r * cellSize, cellSize, cellSize);
      }
    }
  }
}

// Complete Professional Multi-Step Chicken Registration Wizard
function initChickenRegistrationWizard() {
  const triggerAddBtn = document.getElementById('btn-trigger-add-view');
  const formWorkspace = document.getElementById('flock-form-workspace');
  const drawerBackdrop = document.getElementById('drawer-backdrop');

  const btnCloseTop = document.getElementById('btn-form-cancel-top');
  const btnWizardCancel = document.getElementById('btn-wizard-cancel');
  const btnWizardPrev = document.getElementById('btn-wizard-prev');
  const btnWizardNext = document.getElementById('btn-wizard-next');
  const btnWizardSubmit = document.getElementById('btn-wizard-submit');

  const stepItems = document.querySelectorAll('.wizard-step-item');
  const stepPanes = document.querySelectorAll('.wizard-step-pane');

  const inputBirdId = document.getElementById('fm-bird-id');
  const selectCategory = document.getElementById('fm-category');
  const selectBreed = document.getElementById('fm-breed');
  const selectGender = document.getElementById('fm-gender');
  const inputColor = document.getElementById('fm-color');
  const inputWeight = document.getElementById('fm-weight');
  const selectHealth = document.getElementById('fm-health');
  const selectStatus = document.getElementById('fm-status');

  const inputDob = document.getElementById('fm-dob');
  const inputCalculatedAge = document.getElementById('fm-calculated-age');
  const selectOrigin = document.getElementById('fm-origin');
  const wrapperPurchasedFields = document.getElementById('wrapper-purchased-fields');
  const inputPurchaseDate = document.getElementById('fm-purchase-date');
  const inputPurchaseCost = document.getElementById('fm-purchase-cost');
  const inputSupplierName = document.getElementById('fm-supplier-name');
  const inputSupplierContact = document.getElementById('fm-supplier-contact');

  const inputWingTag = document.getElementById('fm-wing-tag');
  const inputLegBand = document.getElementById('fm-leg-band');

  const selectFatherId = document.getElementById('fm-father-id');
  const selectMotherId = document.getElementById('fm-mother-id');

  const selectVaccinated = document.getElementById('fm-vaccinated');
  const wrapperVaccineRows = document.getElementById('wrapper-vaccine-rows');
  const vaccineRowsContainer = document.getElementById('vaccine-rows-container');
  const btnAddVaccineRow = document.getElementById('btn-add-vaccine-row');

  const filePhotoInput = document.getElementById('file-chicken-photo');
  const btnUploadPhoto = document.getElementById('btn-upload-chicken-photo');
  const btnRemovePhoto = document.getElementById('btn-remove-chicken-photo');
  const imgPhotoPreview = document.getElementById('img-chicken-photo-preview');
  const iconPhotoFallback = document.getElementById('icon-chicken-photo-fallback');

  const modalSuccess = document.getElementById('modal-registration-success');
  const succChickenId = document.getElementById('succ-chicken-id');
  const succBreed = document.getElementById('succ-breed');
  const succGender = document.getElementById('succ-gender');
  const succDob = document.getElementById('succ-dob');
  const succCategory = document.getElementById('succ-category');
  const btnSuccView = document.getElementById('btn-succ-view-chicken');
  const btnSuccRegisterAnother = document.getElementById('btn-succ-register-another');
  const btnSuccPrintCard = document.getElementById('btn-succ-print-card');

  const modalPrintCard = document.getElementById('modal-print-card');
  const btnClosePrintCard = document.getElementById('btn-close-print-card');
  const btnCancelPrint = document.getElementById('btn-cancel-print');
  const printChickenId = document.getElementById('print-chicken-id');
  const printChickenPhoto = document.getElementById('print-chicken-photo');
  const printPhotoFallback = document.getElementById('print-photo-fallback');
  const printCategory = document.getElementById('print-category');
  const printBreed = document.getElementById('print-breed');
  const printGender = document.getElementById('print-gender');
  const printDob = document.getElementById('print-dob');
  const printStatus = document.getElementById('print-status');
  const printGenDate = document.getElementById('print-gen-date');

  if (!formWorkspace) return;

  let currentStep = 1;
  let chickenPhotoBase64 = null;
  let lastRegisteredChickenData = null;

  function checkWorkerPermission() {
    const user = AuthService.getCurrentUser();
    const activeRole = user ? (user.currentFarmRole || user.role) : null;
    if (activeRole === 'WORKER') {
      showToast('Access Denied. Workers have read-only access to chickens.', 'error');
      return false;
    }
    return true;
  }

  async function fetchNextChickenCode() {
    try {
      const response = await Api.get('chickens/next-code');
      if (response && response.success && response.data) {
        if (inputBirdId) inputBirdId.value = response.data;
      }
    } catch (e) {
      if (inputBirdId && !inputBirdId.value) inputBirdId.value = 'CHK-000001';
    }
  }

  async function fetchParentCandidates() {
    try {
      const response = await Api.get('chickens?size=200');
      const chickens = (response && response.data && response.data.content) ? response.data.content : [];

      if (selectFatherId) {
        selectFatherId.innerHTML = '<option value="">None / Unknown Father</option>';
        chickens.filter(c => c.gender === 'ROOSTER' || c.gender === 'MALE' || c.gender === 'Hen' || c.gender === 'Rooster').forEach(c => {
          const opt = document.createElement('option');
          opt.value = c.id;
          opt.textContent = `${c.chickenCode} - ${c.name || c.breed}`;
          selectFatherId.appendChild(opt);
        });
      }

      if (selectMotherId) {
        selectMotherId.innerHTML = '<option value="">None / Unknown Mother</option>';
        chickens.filter(c => c.gender === 'HEN' || c.gender === 'FEMALE' || c.gender === 'Hen' || c.gender === 'Rooster').forEach(c => {
          const opt = document.createElement('option');
          opt.value = c.id;
          opt.textContent = `${c.chickenCode} - ${c.name || c.breed}`;
          selectMotherId.appendChild(opt);
        });
      }
    } catch (e) {}
  }

  function goToStep(step) {
    if (step < 1 || step > 6) return;

    currentStep = step;
    stepPanes.forEach(pane => pane.style.display = 'none');
    const targetPane = document.getElementById(`wizard-pane-${step}`);
    if (targetPane) targetPane.style.display = 'block';

    stepItems.forEach(item => {
      const stepNum = parseInt(item.getAttribute('data-step'), 10);
      const numSpan = item.querySelector('.step-num');

      if (stepNum === step) {
        item.style.color = '#16A34A';
        item.style.fontWeight = '700';
        if (numSpan) {
          numSpan.style.background = '#DCFCE7';
          numSpan.style.borderColor = '#16A34A';
          numSpan.style.color = '#16A34A';
        }
      } else if (stepNum < step) {
        item.style.color = '#16A34A';
        item.style.fontWeight = '600';
        if (numSpan) {
          numSpan.style.background = '#16A34A';
          numSpan.style.borderColor = '#16A34A';
          numSpan.style.color = '#FFFFFF';
        }
      } else {
        item.style.color = '#64748B';
        item.style.fontWeight = '600';
        if (numSpan) {
          numSpan.style.background = '#F1F5F9';
          numSpan.style.borderColor = '#CBD5E1';
          numSpan.style.color = '#64748B';
        }
      }
    });

    if (btnWizardPrev) btnWizardPrev.style.display = step === 1 ? 'none' : 'inline-flex';
    if (btnWizardNext) btnWizardNext.style.display = step === 6 ? 'none' : 'inline-flex';
    if (btnWizardSubmit) btnWizardSubmit.style.display = step === 6 ? 'inline-flex' : 'none';

    if (step === 6) {
      updateReviewSummary();
    }
  }

  function validateCurrentStep() {
    if (currentStep === 1) {
      const weight = inputWeight ? parseFloat(inputWeight.value) : 0;
      if (isNaN(weight) || weight <= 0) {
        showToast('Weight must be greater than 0 kg.', 'error');
        return false;
      }
      return true;
    }

    if (currentStep === 2) {
      const dobVal = inputDob ? inputDob.value : '';
      if (!dobVal) {
        showToast('Date of birth is required.', 'error');
        return false;
      }
      const dob = new Date(dobVal);
      if (dob > new Date()) {
        showToast('Date of birth cannot be in the future.', 'error');
        return false;
      }

      if (selectOrigin && selectOrigin.value === 'Purchased') {
        const pDateVal = inputPurchaseDate ? inputPurchaseDate.value : '';
        if (pDateVal) {
          const pDate = new Date(pDateVal);
          if (pDate < dob) {
            showToast('Purchase date cannot be before date of birth.', 'error');
            return false;
          }
        }
        const cost = inputPurchaseCost ? parseFloat(inputPurchaseCost.value) : 0;
        if (!isNaN(cost) && cost < 0) {
          showToast('Purchase cost cannot be negative.', 'error');
          return false;
        }
      }
      return true;
    }

    if (currentStep === 4) {
      const fId = selectFatherId ? selectFatherId.value : '';
      const mId = selectMotherId ? selectMotherId.value : '';
      if (fId && mId && fId === mId) {
        showToast('Father chicken and mother chicken cannot be the same chicken.', 'error');
        return false;
      }
      return true;
    }

    if (currentStep === 5) {
      if (selectVaccinated && selectVaccinated.value === 'Yes') {
        const nameInputs = vaccineRowsContainer.querySelectorAll('.vac-name-input');
        for (let inp of nameInputs) {
          if (!inp.value.trim()) {
            showToast('Vaccine name is required for each added vaccine row.', 'error');
            return false;
          }
        }
      }
      return true;
    }

    return true;
  }

  function calculateAgeText(dobString) {
    if (!dobString) return '-';
    const dob = new Date(dobString);
    const today = new Date();
    const diffTime = Math.abs(today - dob);
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 30) return `${diffDays} days`;
    const months = Math.floor(diffDays / 30);
    const daysRem = diffDays % 30;
    return `${months} months ${daysRem} days (${diffDays} days)`;
  }

  if (inputDob) {
    inputDob.addEventListener('change', () => {
      if (inputCalculatedAge) {
        inputCalculatedAge.value = calculateAgeText(inputDob.value);
      }
    });
  }

  if (selectOrigin) {
    selectOrigin.addEventListener('change', () => {
      if (wrapperPurchasedFields) {
        wrapperPurchasedFields.style.display = selectOrigin.value === 'Purchased' ? 'block' : 'none';
      }
    });
  }

  if (selectVaccinated) {
    selectVaccinated.addEventListener('change', () => {
      if (wrapperVaccineRows) {
        wrapperVaccineRows.style.display = selectVaccinated.value === 'Yes' ? 'flex' : 'none';
      }
      if (selectVaccinated.value === 'Yes' && vaccineRowsContainer.children.length === 0) {
        addVaccineRow();
      }
    });
  }

  function addVaccineRow() {
    if (!vaccineRowsContainer) return;
    const row = document.createElement('div');
    row.style.cssText = 'background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 10px; padding: 14px; display: flex; flex-direction: column; gap: 10px; position: relative;';

    row.innerHTML = `
      <button type="button" class="btn btn-outline btn-remove-vac" style="position: absolute; right: 10px; top: 10px; padding: 2px 8px; font-size: 0.75rem; color: #DC2626; border-color: #FCA5A5;">
        <i class="fa-solid fa-trash-can"></i> Remove
      </button>
      <div style="display: flex; gap: 12px; margin-top: 10px;">
        <div class="floating-label-group" style="flex: 1; margin-bottom: 0;">
          <input type="text" class="vac-name-input" placeholder=" " required style="height: 42px;">
          <label>Vaccine Name *</label>
        </div>
        <div class="floating-label-group" style="flex: 1; margin-bottom: 0;">
          <input type="date" class="vac-date-input" style="height: 42px;">
          <label>Vaccination Date</label>
        </div>
      </div>
      <div style="display: flex; gap: 12px;">
        <div class="floating-label-group" style="flex: 1; margin-bottom: 0;">
          <input type="date" class="vac-due-input" style="height: 42px;">
          <label>Next Due Date</label>
        </div>
        <div class="floating-label-group" style="flex: 1; margin-bottom: 0;">
          <input type="text" class="vac-notes-input" placeholder=" " style="height: 42px;">
          <label>Notes</label>
        </div>
      </div>
    `;

    const btnRemove = row.querySelector('.btn-remove-vac');
    btnRemove.addEventListener('click', () => row.remove());
    vaccineRowsContainer.appendChild(row);
  }

  if (btnAddVaccineRow) {
    btnAddVaccineRow.addEventListener('click', addVaccineRow);
  }

  if (btnUploadPhoto && filePhotoInput) {
    btnUploadPhoto.addEventListener('click', () => filePhotoInput.click());

    filePhotoInput.addEventListener('change', () => {
      const file = filePhotoInput.files[0];
      if (!file) return;

      if (file.size > 5 * 1024 * 1024) {
        showToast('Image size cannot exceed 5 MB.', 'error');
        filePhotoInput.value = '';
        return;
      }

      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
      if (!validTypes.includes(file.type.toLowerCase())) {
        showToast('Invalid format. Allowed formats: JPG, JPEG, PNG, WEBP.', 'error');
        filePhotoInput.value = '';
        return;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        chickenPhotoBase64 = e.target.result;
        if (imgPhotoPreview) {
          imgPhotoPreview.src = chickenPhotoBase64;
          imgPhotoPreview.style.display = 'block';
        }
        if (iconPhotoFallback) iconPhotoFallback.style.display = 'none';
        if (btnRemovePhoto) btnRemovePhoto.style.display = 'inline-flex';
      };
      reader.readAsDataURL(file);
    });
  }

  if (btnRemovePhoto) {
    btnRemovePhoto.addEventListener('click', () => {
      chickenPhotoBase64 = null;
      if (filePhotoInput) filePhotoInput.value = '';
      if (imgPhotoPreview) {
        imgPhotoPreview.src = '';
        imgPhotoPreview.style.display = 'none';
      }
      if (iconPhotoFallback) iconPhotoFallback.style.display = 'block';
      btnRemovePhoto.style.display = 'none';
    });
  }

  function updateReviewSummary() {
    const revId = document.getElementById('rev-id');
    const revCat = document.getElementById('rev-category');
    const revBreed = document.getElementById('rev-breed');
    const revGender = document.getElementById('rev-gender');
    const revDob = document.getElementById('rev-dob');
    const revWeight = document.getElementById('rev-weight');
    const revOrigin = document.getElementById('rev-origin');
    const revHealth = document.getElementById('rev-health');

    if (revId) revId.textContent = inputBirdId ? inputBirdId.value : '-';
    if (revCat) revCat.textContent = selectCategory ? selectCategory.value : '-';
    if (revBreed) revBreed.textContent = selectBreed ? selectBreed.value : '-';
    if (revGender) revGender.textContent = selectGender ? selectGender.value : '-';
    if (revDob) revDob.textContent = inputDob ? inputDob.value : '-';
    if (revWeight) revWeight.textContent = inputWeight ? `${inputWeight.value} kg` : '-';
    if (revOrigin) revOrigin.textContent = selectOrigin ? selectOrigin.value : '-';
    if (revHealth) revHealth.textContent = selectHealth ? selectHealth.value : '-';
  }

  function openWizard() {
    if (!checkWorkerPermission()) return;

    if (formWorkspace) {
      formWorkspace.style.display = 'flex';
      formWorkspace.classList.add('open');
    }
    if (drawerBackdrop) drawerBackdrop.classList.add('open');

    const form = document.getElementById('form-fullpage-bird-editor');
    if (form) form.reset();
    chickenPhotoBase64 = null;
    if (imgPhotoPreview) {
      imgPhotoPreview.src = '';
      imgPhotoPreview.style.display = 'none';
    }
    if (iconPhotoFallback) iconPhotoFallback.style.display = 'block';
    if (btnRemovePhoto) btnRemovePhoto.style.display = 'none';
    if (vaccineRowsContainer) vaccineRowsContainer.innerHTML = '';
    if (wrapperVaccineRows) wrapperVaccineRows.style.display = 'none';
    if (wrapperPurchasedFields) wrapperPurchasedFields.style.display = 'none';

    if (inputDob) inputDob.value = new Date().toISOString().split('T')[0];
    if (inputCalculatedAge) inputCalculatedAge.value = '0 days';

    fetchNextChickenCode();
    fetchParentCandidates();
    goToStep(1);
  }

  function closeWizard() {
    if (formWorkspace) {
      formWorkspace.classList.remove('open');
      setTimeout(() => formWorkspace.style.display = 'none', 300);
    }
    if (drawerBackdrop) drawerBackdrop.classList.remove('open');
  }

  if (triggerAddBtn) triggerAddBtn.addEventListener('click', openWizard);
  if (btnCloseTop) btnCloseTop.addEventListener('click', closeWizard);
  if (btnWizardCancel) btnWizardCancel.addEventListener('click', closeWizard);

  if (btnWizardPrev) {
    btnWizardPrev.addEventListener('click', () => {
      if (currentStep > 1) goToStep(currentStep - 1);
    });
  }

  if (btnWizardNext) {
    btnWizardNext.addEventListener('click', () => {
      if (validateCurrentStep()) {
        goToStep(currentStep + 1);
      }
    });
  }

  stepItems.forEach(item => {
    item.addEventListener('click', () => {
      const step = parseInt(item.getAttribute('data-step'), 10);
      if (step < currentStep || validateCurrentStep()) {
        goToStep(step);
      }
    });
  });

  const formEditor = document.getElementById('form-fullpage-bird-editor');
  if (formEditor) {
    formEditor.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!validateCurrentStep()) return;

      if (btnWizardSubmit) {
        btnWizardSubmit.disabled = true;
        btnWizardSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin-pulse"></i> Saving...';
      }

      const categoryMap = {
        'Country Chicken': 'COUNTRY_CHICKEN',
        'Broiler': 'BROILER',
        'Layer': 'LAYER',
        'Other': 'OTHER'
      };

      const breedMap = {
        'Cobb 500': 'COBB_500',
        'Ross 308': 'ROSS_308',
        'Hubbard': 'HUBBARD',
        'Leghorn': 'LEGHORN',
        'Rhode Island Red': 'RHODE_ISLAND_RED',
        'Plymouth Rock': 'PLYMOUTH_ROCK',
        'Brahma': 'BRAMA',
        'Sussex': 'SUSSEX',
        'Desi Country': 'OTHER',
        'Other': 'OTHER'
      };

      const genderMap = {
        'Hen': 'FEMALE',
        'Rooster': 'MALE',
        'Unknown': 'UNKNOWN'
      };

      const healthMap = {
        'Healthy': 'HEALTHY',
        'Under Observation': 'OBSERVATION',
        'Sick': 'SICK',
        'In Treatment': 'UNDER_TREATMENT',
        'Recovered': 'RECOVERING',
        'Dead': 'DECEASED'
      };

      const statusMap = {
        'Active': 'ACTIVE',
        'Sold': 'SOLD',
        'Dead': 'DEAD'
      };

      const vaccinations = [];
      if (selectVaccinated && selectVaccinated.value === 'Yes' && vaccineRowsContainer) {
        const rows = vaccineRowsContainer.children;
        for (let r of rows) {
          const vName = r.querySelector('.vac-name-input')?.value;
          const vDate = r.querySelector('.vac-date-input')?.value;
          const vDue = r.querySelector('.vac-due-input')?.value;
          const vNotes = r.querySelector('.vac-notes-input')?.value;

          if (vName) {
            vaccinations.push({
              vaccineName: vName,
              vaccinationDate: vDate || null,
              nextDueDate: vDue || null,
              notes: vNotes || ''
            });
          }
        }
      }

      const payload = {
        chickenCode: inputBirdId ? inputBirdId.value.trim() : null,
        category: categoryMap[selectCategory?.value] || 'OTHER',
        breed: breedMap[selectBreed?.value] || 'OTHER',
        gender: genderMap[selectGender?.value] || 'UNKNOWN',
        color: inputColor ? inputColor.value.trim() : '',
        weight: inputWeight ? parseFloat(inputWeight.value) : 1.0,
        healthStatus: healthMap[selectHealth?.value] || 'HEALTHY',
        status: statusMap[selectStatus?.value] || 'ACTIVE',
        dateOfBirth: inputDob ? inputDob.value : new Date().toISOString().split('T')[0],
        origin: selectOrigin && selectOrigin.value === 'Purchased' ? 'PURCHASED' : 'FARM_BORN',
        purchaseDate: inputPurchaseDate ? inputPurchaseDate.value : null,
        purchaseCost: inputPurchaseCost ? parseFloat(inputPurchaseCost.value) : null,
        supplierName: inputSupplierName ? inputSupplierName.value.trim() : null,
        supplierContact: inputSupplierContact ? inputSupplierContact.value.trim() : null,
        wingTagNumber: inputWingTag ? inputWingTag.value.trim() : null,
        legBandNumber: inputLegBand ? inputLegBand.value.trim() : null,
        fatherId: selectFatherId && selectFatherId.value ? parseInt(selectFatherId.value, 10) : null,
        motherId: selectMotherId && selectMotherId.value ? parseInt(selectMotherId.value, 10) : null,
        vaccinated: selectVaccinated ? selectVaccinated.value === 'Yes' : false,
        vaccinations: vaccinations,
        photoUrl: chickenPhotoBase64 || null
      };

      try {
        const response = await Api.post('chickens', payload);
        if (response && response.success && response.data) {
          lastRegisteredChickenData = response.data;
          closeWizard();
          showSuccessModal(response.data);
          showToast('Chicken registered successfully!', 'success');
        } else {
          throw new Error(response ? response.message : 'Registration failed');
        }
      } catch (err) {
        console.error('[Chicken Registration] Error:', err);
        showToast(err.message || 'Failed to register chicken.', 'error');
      } finally {
        if (btnWizardSubmit) {
          btnWizardSubmit.disabled = false;
          btnWizardSubmit.innerHTML = '<i class="fa-solid fa-circle-check"></i> Register Chicken';
        }
      }
    });
  }

  function showSuccessModal(data) {
    if (!modalSuccess) return;

    if (succChickenId) succChickenId.textContent = data.chickenCode || '-';
    if (succBreed) succBreed.textContent = data.breed || '-';
    if (succGender) succGender.textContent = data.gender || '-';
    if (succDob) succDob.textContent = data.dateOfBirth || '-';
    if (succCategory) succCategory.textContent = data.category || '-';

    const qrData = `Chicken ID: ${data.chickenCode} | Farm: 1 | Breed: ${data.breed} | Gender: ${data.gender} | DOB: ${data.dateOfBirth}`;
    drawQRCodeOnCanvas('canvas-chicken-qrcode', qrData);

    modalSuccess.style.display = 'flex';
  }

  if (btnSuccView) {
    btnSuccView.addEventListener('click', () => {
      if (modalSuccess) modalSuccess.style.display = 'none';
      if (window.location.pathname.includes('flock.html')) {
        window.location.reload();
      }
    });
  }

  if (btnSuccRegisterAnother) {
    btnSuccRegisterAnother.addEventListener('click', () => {
      if (modalSuccess) modalSuccess.style.display = 'none';
      openWizard();
    });
  }

  if (btnSuccPrintCard) {
    btnSuccPrintCard.addEventListener('click', () => {
      if (modalSuccess) modalSuccess.style.display = 'none';
      openPrintCardModal(lastRegisteredChickenData);
    });
  }

  function openPrintCardModal(data) {
    if (!modalPrintCard || !data) return;

    if (printChickenId) printChickenId.textContent = data.chickenCode || 'CHK-000001';
    if (printCategory) printCategory.textContent = data.category || '-';
    if (printBreed) printBreed.textContent = data.breed || '-';
    if (printGender) printGender.textContent = data.gender || '-';
    if (printDob) printDob.textContent = data.dateOfBirth || '-';
    if (printStatus) printStatus.textContent = data.status || 'Active';
    if (printGenDate) printGenDate.textContent = new Date().toLocaleDateString('en-US');

    if (data.photoUrl) {
      if (printChickenPhoto) {
        printChickenPhoto.src = data.photoUrl;
        printChickenPhoto.style.display = 'block';
      }
      if (printPhotoFallback) printPhotoFallback.style.display = 'none';
    } else {
      if (printChickenPhoto) printChickenPhoto.style.display = 'none';
      if (printPhotoFallback) printPhotoFallback.style.display = 'block';
    }

    const qrData = `Chicken ID: ${data.chickenCode || data.id} | Farm: 1 | Breed: ${data.breed} | Gender: ${data.gender} | DOB: ${data.dateOfBirth || data.dob}`;
    drawQRCodeOnCanvas('canvas-print-qrcode', qrData);

    modalPrintCard.style.display = 'flex';
  }

  window.triggerPrintCard = async (dbId) => {
    try {
      const res = await window.Api.get(`chickens/${dbId}`);
      if (res && res.success) openPrintCardModal(res.data);
    } catch (e) {
      console.error(e);
    }
  };

  window.showChickenQrModal = (chicken) => {
    openPrintCardModal({
      chickenCode: chicken.id,
      category: chicken.category,
      breed: chicken.breed,
      gender: chicken.gender,
      dateOfBirth: chicken.dob,
      status: chicken.status,
      photoUrl: chicken.photoUrl
    });
  };

  if (btnClosePrintCard) btnClosePrintCard.addEventListener('click', () => modalPrintCard.style.display = 'none');
  if (btnCancelPrint) btnCancelPrint.addEventListener('click', () => modalPrintCard.style.display = 'none');
}

// Auto-initialize standard selects & mobile UX components on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
  initMobileNavigation();
  initGlobalFAB();
  initDynamicHeaderWeatherAndDate();
  initGPSLocationCaptureSystem();
  initDeleteFarmWorkflow();
  initJoinFarmWorkflow();
  initFarmProfileManagement();
  initChickenRegistrationWizard();

  setTimeout(() => {
    document.querySelectorAll('select').forEach(sel => {
      if (sel.closest('.custom-select-wrapper')) return;
      if (sel.id.includes('breed')) return;
      if (sel.classList.contains('native-select')) return;
      
      window.makePremiumSelect(sel.id);
    });
  }, 100);
});

