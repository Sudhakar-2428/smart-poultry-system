import { Api } from './api.js';
import { AuthService } from './authService.js';

// Centralized Notification Manager for Smart Poultry Management System
(function() {
  // Helper to generate relative dates for demo data consistency
  function getRelativeDate(daysAgo, hoursAgo = 0) {
    const d = new Date();
    d.setDate(d.getDate() - daysAgo);
    d.setHours(d.getHours() - hoursAgo);
    return d.toISOString();
  }

  const NotificationManager = {
    getNotifications: function() {
      let list = localStorage.getItem('poultry_notifications');
      if (!list) {
        list = JSON.stringify(this.generateDemoNotifications());
        localStorage.setItem('poultry_notifications', list);
      }
      try {
        return JSON.parse(list);
      } catch(e) {
        console.error("Error reading notifications from localStorage", e);
        return [];
      }
    },

    saveNotifications: function(list) {
      localStorage.setItem('poultry_notifications', JSON.stringify(list));
      window.dispatchEvent(new CustomEvent('poultry_notifications_updated'));
    },

    syncWithBackend: async function() {
      if (!AuthService.isAuthenticated()) return;
      try {
        const response = await Api.get('notifications?page=0&size=100&sort=id,desc');
        if (response && response.success && response.data && response.data.content) {
          const backendList = response.data.content.map(item => {
            let frontType = 'Information';
            if (item.notificationType === 'SUCCESS') frontType = 'Success';
            else if (item.notificationType === 'WARNING') frontType = 'Warning';
            else if (item.notificationType === 'CRITICAL') frontType = 'Critical';
            else if (item.notificationType === 'REMINDER') frontType = 'Reminder';

            let frontPriority = 'Low';
            if (item.severity === 'MEDIUM') frontPriority = 'Medium';
            else if (item.severity === 'HIGH') frontPriority = 'High';
            else if (item.severity === 'CRITICAL') frontPriority = 'Critical';

            let frontModule = 'Chicken Management';
            const m = item.sourceModule;
            if (m === 'HEALTH') frontModule = 'Health Records';
            else if (m === 'FEED') frontModule = 'Feed Management';
            else if (m === 'SALES') frontModule = 'Sales Management';
            else if (m === 'FINANCE') frontModule = 'Finance & Ledger';
            else if (m === 'CHICKEN') frontModule = 'Chicken Management';
            else if (m === 'EGG') frontModule = 'Egg Tracking';
            else if (m === 'BROOD') frontModule = 'Brooding';
            else if (m === 'INCUBATION') frontModule = 'Incubation';
            else if (m === 'HATCH') frontModule = 'Hatch Results';
            else if (m === 'GROWTH') frontModule = 'Chick Growth';
            else if (m === 'PAIRING') frontModule = 'Pairing Management';
            else if (m === 'REPORTS') frontModule = 'Reports';
            else if (m === 'SETTINGS') frontModule = 'Farm Settings';

            let frontStatus = 'Unread';
            if (item.isArchived) frontStatus = 'Archived';
            else if (item.isRead) frontStatus = 'Read';

            return {
              id: item.id.toString(),
              title: item.title,
              description: item.message,
              module: frontModule,
              type: frontType,
              priority: frontPriority,
              status: frontStatus,
              timestamp: item.createdAt || new Date().toISOString(),
              referenceId: item.referenceId ? item.referenceId.toString() : ''
            };
          });

          const currentList = this.getNotifications();
          const demoList = currentList.filter(n => n.id.startsWith('NOTF-DEMO-'));
          
          const merged = [...backendList];
          demoList.forEach(demo => {
            if (!merged.some(m => m.id === demo.id)) {
              merged.push(demo);
            }
          });

          this.saveNotifications(merged);
        }
      } catch (err) {
        console.error("Failed to sync notifications with backend", err);
      }
    },

    add: function(title, description, module, type, priority, referenceId = "") {
      const list = this.getNotifications();
      const newNotif = {
        id: 'NOTF-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
        title: title,
        description: description,
        module: module,
        type: type, // 'Information', 'Success', 'Warning', 'Critical', 'Reminder'
        priority: priority, // 'Low', 'Medium', 'High', 'Critical'
        status: 'Unread', // 'Unread', 'Read', 'Archived'
        timestamp: new Date().toISOString(),
        referenceId: referenceId
      };
      
      // Look up system preference to check if alert toggle is active for this category
      const alertsConfig = JSON.parse(localStorage.getItem('poultry_settings_alerts')) || {
        vax: true, brood: true, incubation: true, feed: true, sales: true
      };
      
      let shouldAlert = true;
      if (module === 'Health Records' && !alertsConfig.vax && title.toLowerCase().includes('vaccine')) shouldAlert = false;
      if (module === 'Brooding' && !alertsConfig.brood) shouldAlert = false;
      if (module === 'Incubation' && !alertsConfig.incubation) shouldAlert = false;
      if (module === 'Feed Management' && !alertsConfig.feed) shouldAlert = false;
      if (module === 'Sales Management' && !alertsConfig.sales) shouldAlert = false;

      if (shouldAlert) {
        list.unshift(newNotif);
        this.saveNotifications(list);
        this.showToast(newNotif);
      }
      
      return newNotif;
    },

    markAsRead: async function(id) {
      if (id.startsWith('NOTF-DEMO-')) {
        const list = this.getNotifications();
        const item = list.find(n => n.id === id);
        if (item) {
          item.status = 'Read';
          this.saveNotifications(list);
        }
        return;
      }
      try {
        await Api.patch(`notifications/${id}/read`);
        await this.syncWithBackend();
      } catch (err) {
        console.error("Error marking notification read in backend", err);
      }
    },

    markAsUnread: function(id) {
      const list = this.getNotifications();
      const item = list.find(n => n.id === id);
      if (item) {
        item.status = 'Unread';
        this.saveNotifications(list);
      }
    },

    archive: async function(id) {
      if (id.startsWith('NOTF-DEMO-')) {
        const list = this.getNotifications();
        const item = list.find(n => n.id === id);
        if (item) {
          item.status = 'Archived';
          this.saveNotifications(list);
        }
        return;
      }
      try {
        await Api.patch(`notifications/${id}/archive`);
        await this.syncWithBackend();
      } catch (err) {
        console.error("Error archiving notification in backend", err);
      }
    },

    deleteChange: async function(id) {
      if (id.startsWith('NOTF-DEMO-')) {
        let list = this.getNotifications();
        list = list.filter(n => n.id !== id);
        this.saveNotifications(list);
        return;
      }
      try {
        await Api.patch(`notifications/${id}/archive`);
        await this.syncWithBackend();
      } catch (err) {
        console.error("Error deleting notification", err);
      }
    },

    markAllAsRead: async function() {
      const list = this.getNotifications();
      let hasDemo = false;
      list.forEach(n => {
        if (n.id.startsWith('NOTF-DEMO-') && n.status === 'Unread') {
          n.status = 'Read';
          hasDemo = true;
        }
      });
      if (hasDemo) {
        this.saveNotifications(list);
      }
      try {
        await Api.patch('notifications/read-all');
        await this.syncWithBackend();
      } catch (err) {
        console.error("Error bulk marking notifications read in backend", err);
      }
    },

    showToast: function(notif) {
      let container = document.getElementById("success-toast-container");
      if (!container) {
        container = document.createElement("div");
        container.id = "success-toast-container";
        container.style.cssText = "position: fixed; bottom: 24px; right: 24px; z-index: 10000000; display: flex; flex-direction: column; gap: 8px;";
        document.body.appendChild(container);
      }

      const toast = document.createElement("div");
      toast.style.cssText = `
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(10px);
        -webkit-backdrop-filter: blur(10px);
        border: 1px solid var(--glass-border);
        border-left: 5px solid ${this.getTypeColor(notif.type)};
        border-radius: var(--border-radius-md);
        box-shadow: 0 10px 30px rgba(0,0,0,0.1);
        padding: 12px 18px;
        width: 320px;
        font-family: inherit;
        cursor: pointer;
        animation: toastIn 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28) forwards;
        display: flex;
        flex-direction: column;
        gap: 2px;
      `;

      if (!document.getElementById("toast-kf-style")) {
        const kfStyle = document.createElement("style");
        kfStyle.id = "toast-kf-style";
        kfStyle.innerHTML = `
          @keyframes toastIn {
            from { transform: translateY(100px) scale(0.8); opacity: 0; }
            to { transform: translateY(0) scale(1); opacity: 1; }
          }
          @keyframes toastOut {
            to { transform: translateX(120%) scale(0.9); opacity: 0; }
          }
        `;
        document.head.appendChild(kfStyle);
      }

      toast.innerHTML = `
        <div style="font-weight: 800; font-size: 0.82rem; color: var(--neutral-dark); display: flex; align-items: center; gap: 6px;">
          <span style="font-size: 0.95rem;">${this.getTypeEmoji(notif.type)}</span>
          ${notif.title}
        </div>
        <div style="font-size: 0.72rem; color: var(--neutral-gray); line-height: 1.3;">
          ${notif.description}
        </div>
      `;

      toast.onclick = () => {
        this.viewRelatedRecord(notif);
        toast.style.animation = "toastOut 0.3s forwards";
        setTimeout(() => toast.remove(), 300);
      };

      container.appendChild(toast);
      setTimeout(() => {
        if (toast.parentNode) {
          toast.style.animation = "toastOut 0.3s forwards";
          setTimeout(() => toast.remove(), 300);
        }
      }, 5000);
    },

    getTypeColor: function(type) {
      if (type === 'Critical') return '#EF4444';
      if (type === 'Warning') return '#F59E0B';
      if (type === 'Success') return '#10B981';
      if (type === 'Reminder') return '#8B5CF6';
      return '#3B82F6';
    },

    getTypeEmoji: function(type) {
      if (type === 'Critical') return '🚨';
      if (type === 'Warning') return '⚠️';
      if (type === 'Success') return '✅';
      if (type === 'Reminder') return '📅';
      return '🔔';
    },

    viewRelatedRecord: function(notification) {
      this.markAsRead(notification.id);
      const refId = notification.referenceId || "";
      let targetUrl = "";

      switch(notification.module) {
        case "Chicken Management":
          targetUrl = `flock.html?id=${refId}`;
          break;
        case "Egg Tracking":
        case "Brooding":
        case "Incubation":
        case "Hatch Results":
          targetUrl = `egg-tracking.html?id=${refId}`;
          break;
        case "Chick Growth":
          targetUrl = `chick-growth.html?id=${refId}`;
          break;
        case "Pairing Management":
          targetUrl = `pairing.html?id=${refId}`;
          break;
        case "Health Records":
          targetUrl = `health-records.html?id=${refId}`;
          break;
        case "Feed Management":
          targetUrl = `feed-management.html?id=${refId}`;
          break;
        case "Sales Management":
          targetUrl = `sales.html?id=${refId}`;
          break;
        case "Finance & Ledger":
          targetUrl = `finance.html?id=${refId}`;
          break;
        case "Reports":
          targetUrl = `reports.html?id=${refId}`;
          break;
        case "Farm Settings":
          targetUrl = `settings.html?id=${refId}`;
          break;
        default:
          targetUrl = `dashboard.html`;
      }
      window.location.href = targetUrl;
    },

    generateDemoNotifications: function() {
      return [
        {
          id: "NOTF-DEMO-01",
          title: "Warning: Feed Stock Below Safety buffer",
          description: "Purina Chick Starter crumbs (FEED-02) quantity is at 180kg, below safety buffer of 250kg.",
          module: "Feed Management",
          type: "Warning",
          priority: "High",
          status: "Unread",
          timestamp: getRelativeDate(0, 1),
          referenceId: "FEED-02"
        },
        {
          id: "NOTF-DEMO-02",
          title: "New Chicken Registered",
          description: "Daisy (LB-105) has been successfully added to Coop A - Laying Cage as Layer.",
          module: "Chicken Management",
          type: "Success",
          priority: "Low",
          status: "Unread",
          timestamp: getRelativeDate(0, 2),
          referenceId: "C005"
        },
        {
          id: "NOTF-DEMO-03",
          title: "Critical Health Alert: Sick Bird Observed",
          description: "Hen Bella (C001) marked with Sick health status. Isolation protocol recommended.",
          module: "Health Records",
          type: "Critical",
          priority: "Critical",
          status: "Unread",
          timestamp: getRelativeDate(0, 4),
          referenceId: "C001"
        },
        {
          id: "NOTF-DEMO-04",
          title: "Egg Laying Started",
          description: "Hen Goldie (C003) has transitioned to Laying status.",
          module: "Egg Tracking",
          type: "Information",
          priority: "Medium",
          status: "Unread",
          timestamp: getRelativeDate(0, 5),
          referenceId: "C003"
        },
        {
          id: "NOTF-DEMO-05",
          title: "Pending Payment Order Invoice",
          description: "Invoice INV-10253 generated for Alice Miller remains in Pending state ($45.00).",
          module: "Sales Management",
          type: "Reminder",
          priority: "Medium",
          status: "Read",
          timestamp: getRelativeDate(1, 2),
          referenceId: "INV-10253"
        },
        {
          id: "NOTF-DEMO-06",
          title: "Incubation Period Completed",
          description: "Day 21 incubation period completed for Hen Bella's breeding batch. Ready for hatching results.",
          module: "Incubation",
          type: "Success",
          priority: "High",
          status: "Read",
          timestamp: getRelativeDate(1, 6),
          referenceId: "C001"
        },
        {
          id: "NOTF-DEMO-07",
          title: "Hatching Success Recorded",
          description: "Hatch outcome submitted successfully: 10 chicks out of 12 hatched (83% success rate).",
          module: "Hatch Results",
          type: "Success",
          priority: "High",
          status: "Read",
          timestamp: getRelativeDate(1, 7),
          referenceId: "BATCH-HN-001"
        },
        {
          id: "NOTF-DEMO-08",
          title: "Chicks Moved to Brooder Care",
          description: "Brooder batch of 10 chicks successfully moved to chick starter growing phase.",
          module: "Chick Growth",
          type: "Success",
          priority: "Low",
          status: "Read",
          timestamp: getRelativeDate(2, 1),
          referenceId: "BATCH-HN-001"
        },
        {
          id: "NOTF-DEMO-09",
          title: "New Breeding Pair Configured",
          description: "Pair PAIR-001 configured between HN-001 (dam) and RT-101 (sire).",
          module: "Pairing Management",
          type: "Information",
          priority: "Low",
          status: "Read",
          timestamp: getRelativeDate(3, 4),
          referenceId: "PAIR-001"
        },
        {
          id: "NOTF-DEMO-10",
          title: "Large Expense Transaction Recorded",
          description: "Expense allocation of $480.00 logged for Feed Purchase from Cargill Feeding Corp.",
          module: "Finance & Ledger",
          type: "Information",
          priority: "Medium",
          status: "Read",
          timestamp: getRelativeDate(4, 5),
          referenceId: "EXP-8902"
        },
        {
          id: "NOTF-DEMO-11",
          title: "Monthly Production Analytics Compiled",
          description: "Full diagnostics reports and egg laying metrics PDF document cached.",
          module: "Reports",
          type: "Success",
          priority: "Low",
          status: "Read",
          timestamp: getRelativeDate(5, 2),
          referenceId: "REP-CSV"
        },
        {
          id: "NOTF-DEMO-12",
          title: "Workspace Config Backup Completed",
          description: "JSON configuration database migration file successfully serialized.",
          module: "Farm Settings",
          type: "Success",
          priority: "Low",
          status: "Read",
          timestamp: getRelativeDate(6, 1),
          referenceId: "BACKUP-JSON"
        },
        {
          id: "NOTF-DEMO-13",
          title: "Biosecurity Core Guidelines Immunization Due",
          description: "Booster vaccination schedule Newcastle Lasota is overdue for flock cohort Coop B.",
          module: "Health Records",
          type: "Reminder",
          priority: "High",
          status: "Unread",
          timestamp: getRelativeDate(2, 0),
          referenceId: "health-records.html"
        }
      ];
    }
  };

  window.NotificationManager = NotificationManager;
})();

// Interceptor hook logic for localStorage actions
window.handleLocalStorageChange = function(key, newValueString) {
  try {
    const newValue = JSON.parse(newValueString);
    if (!newValue) return;

    if (key === 'poultry_birds_list') {
      const oldBirds = JSON.parse(localStorage.getItem('_prev_birds_list') || '[]');
      localStorage.setItem('_prev_birds_list', newValueString);
      if (oldBirds.length === 0) return;

      if (newValue.length > oldBirds.length) {
        newValue.forEach(bird => {
          if (!oldBirds.some(ob => ob.id === bird.id)) {
            window.NotificationManager.add(
              "New Chicken Registered",
              `${bird.name} (${bird.id}) has been added to ${bird.coop || 'the flock'}.`,
              "Chicken Management",
              "Success",
              "Low",
              bird.id
            );
          }
        });
      } else {
        newValue.forEach(bird => {
          const oldBird = oldBirds.find(ob => ob.id === bird.id);
          if (oldBird) {
            if (oldBird.status !== bird.status) {
              if (bird.status === "Sold") {
                window.NotificationManager.add(
                  "Chicken Sold",
                  `${bird.name} (${bird.id}) marked as Sold.`,
                  "Chicken Management",
                  "Success",
                  "Low",
                  bird.id
                );
              } else if (bird.status === "Dead") {
                window.NotificationManager.add(
                  "Chicken Marked as Dead",
                  `Flock update: ${bird.name} (${bird.id}) was marked as Dead.`,
                  "Chicken Management",
                  "Critical",
                  "Critical",
                  bird.id
                );
              } else if (bird.status === "Egg Laying" || bird.status === "Laying") {
                window.NotificationManager.add(
                  "Egg Laying Started",
                  `Hen ${bird.name} (${bird.id}) status transitioned to laying mode.`,
                  "Egg Tracking",
                  "Success",
                  "Medium",
                  bird.id
                );
              }
            }
            if (oldBird.health !== bird.health) {
              if (bird.health === "Sick") {
                window.NotificationManager.add(
                  "Treatment Protocol Initiated",
                  `${bird.name} (${bird.id}) is Sick. Isolation and medication advisory.`,
                  "Health Records",
                  "Critical",
                  "Critical",
                  bird.id
                );
              } else if (bird.health === "Under Observation" || bird.health === "Under Treatment") {
                window.NotificationManager.add(
                  "Health Issue Recorded",
                  `${bird.name} (${bird.id}) health status updated to ${bird.health}.`,
                  "Health Records",
                  "Warning",
                  "Medium",
                  bird.id
                );
              } else if (bird.health === "Vaccinated" && oldBird.health !== "Vaccinated") {
                window.NotificationManager.add(
                  "Vaccination Completed",
                  `Immunization schedule completed for ${bird.name} (${bird.id}).`,
                  "Health Records",
                  "Success",
                  "Low",
                  bird.id
                );
              }
            }
          }
        });
      }
    }

    else if (key === 'poultry_egg_records') {
      const oldRecords = JSON.parse(localStorage.getItem('_prev_egg_records') || '[]');
      localStorage.setItem('_prev_egg_records', newValueString);
      if (oldRecords.length === 0) return;

      if (newValue.length > oldRecords.length) {
        const newRecord = newValue[newValue.length - 1];
        if (newRecord) {
          window.NotificationManager.add(
            "Daily Egg Record Added",
            `Hen ${newRecord.henId} recorded laying ${newRecord.count} eggs.`,
            "Egg Tracking",
            "Success",
            "Low",
            newRecord.henId
          );
        }
      }
    }

    else if (key === 'poultry_brooding_batches') {
      const oldBatches = JSON.parse(localStorage.getItem('_prev_brooding_batches') || '[]');
      localStorage.setItem('_prev_brooding_batches', newValueString);
      if (oldBatches.length === 0) return;

      if (newValue.length > oldBatches.length) {
        const newBatch = newValue.find(nb => !oldBatches.some(ob => ob.henId === nb.henId));
        if (newBatch) {
          window.NotificationManager.add(
            "Brooding Started",
            `Egg batch brooding phase initiated with ${newBatch.totalEggs} eggs for Mother Hen ${newBatch.henId}.`,
            "Brooding",
            "Information",
            "Medium",
            newBatch.henId
          );
        }
      }
    }

    else if (key === 'poultry_hatch_outcomes') {
      const oldOutcomes = JSON.parse(localStorage.getItem('_prev_hatch_outcomes') || '[]');
      localStorage.setItem('_prev_hatch_outcomes', newValueString);
      if (oldOutcomes.length === 0) return;

      if (newValue.length > oldOutcomes.length) {
        const newHatch = newValue[newValue.length - 1];
        if (newHatch) {
          window.NotificationManager.add(
            "Hatching Completed",
            `Incubation complete for ${newHatch.henId}. Result: ${newHatch.hatchedCount} chicks hatched successfully.`,
            "Hatch Results",
            "Success",
            "High",
            newHatch.henId
          );
        }
      }
    }

    else if (key === 'poultry_brooder_batches') {
      const oldBrooder = JSON.parse(localStorage.getItem('_prev_brooder_batches') || '[]');
      localStorage.setItem('_prev_brooder_batches', newValueString);
      if (oldBrooder.length === 0) return;

      newValue.forEach(batch => {
        const oldB = oldBrooder.find(ob => ob.id === batch.id);
        if (oldB && oldB.status !== batch.status && batch.status === 'Completed') {
          window.NotificationManager.add(
            "New Chicks Released",
            `Brooder batch for Mother ${batch.motherId || batch.id} graduated and entered Growing Chicks flock group.`,
            "Chick Growth",
            "Success",
            "Medium",
            batch.id
          );
        }
      });
    }

    else if (key === 'poultry_pairs_list') {
      const oldPairs = JSON.parse(localStorage.getItem('_prev_pairs_list') || '[]');
      localStorage.setItem('_prev_pairs_list', newValueString);
      if (oldPairs.length === 0) return;

      if (newValue.length > oldPairs.length) {
        newValue.forEach(pair => {
          if (!oldPairs.some(op => op.id === pair.id)) {
            window.NotificationManager.add(
              "Breeding Pair Configured",
              `Pair ${pair.id} created: Hen ${pair.henId} matched with Rooster ${pair.roosterId}.`,
              "Pairing Management",
              "Information",
              "Low",
              pair.id
            );
          }
        });
      } else {
        newValue.forEach(pair => {
          const oldPair = oldPairs.find(op => op.id === pair.id);
          if (oldPair && oldPair.status !== pair.status) {
            if (pair.status === "Ended") {
              window.NotificationManager.add(
                "Breeding Pair Ended",
                `Breeding pair coordination ${pair.id} has ended.`,
                "Pairing Management",
                "Information",
                "Low",
                pair.id
              );
            }
          }
        });
      }
    }

    else if (key === 'poultry_feed_inventory') {
      const oldFeeds = JSON.parse(localStorage.getItem('_prev_feed_inventory') || '[]');
      localStorage.setItem('_prev_feed_inventory', newValueString);
      if (oldFeeds.length === 0) return;

      newValue.forEach(feed => {
        const oldF = oldFeeds.find(of => of.id === feed.id);
        if (oldF) {
          if (feed.quantity > oldF.quantity) {
             window.NotificationManager.add(
               "Feed Inventory Replenished",
               `Feed stock ${feed.name} (${feed.id}) re-supplied. New balance: ${feed.quantity}kg.`,
               "Feed Management",
               "Success",
               "Medium",
               feed.id
             );
          }
          if (feed.quantity <= feed.minThreshold && oldF.quantity > feed.minThreshold && feed.quantity > 0) {
             window.NotificationManager.add(
               "Warning: Feed Stock Below Safety buffer",
               `Low stock alert: ${feed.name} is at ${feed.quantity}kg.`,
               "Feed Management",
               "Warning",
               "High",
               feed.id
             );
          } else if (feed.quantity === 0 && oldF.quantity > 0) {
             window.NotificationManager.add(
               "Critical: Feed Out of Stock!",
               `${feed.name} stock has been completely depleted.`,
               "Feed Management",
               "Critical",
               "Critical",
               feed.id
             );
          }
        }
      });
    }

    else if (key === 'poultry_sales_history') {
      const oldSales = JSON.parse(localStorage.getItem('_prev_sales_history') || '[]');
      localStorage.setItem('_prev_sales_history', newValueString);
      if (oldSales.length === 0) return;

      if (newValue.length > oldSales.length) {
        const newSale = newValue[0];
        if (newSale) {
          window.NotificationManager.add(
            "Sale Order Details Filed",
            `Invoice ${newSale.invoiceNo} registered for ${newSale.customerName} ($${newSale.total.toFixed(2)}).`,
            "Sales Management",
            "Success",
            "Medium",
            newSale.invoiceNo
          );
          if (newSale.status === "Pending") {
            window.NotificationManager.add(
              "Invoice Pending Payment",
              `Invoice ${newSale.invoiceNo} for ${newSale.customerName} payment status remains unresolved ($${newSale.total.toFixed(2)}).`,
              "Sales Management",
              "Reminder",
              "High",
              newSale.invoiceNo
            );
          }
        }
      }
    }

    else if (key === 'poultry_ledger_expenses') {
      const oldExp = JSON.parse(localStorage.getItem('_prev_ledger_expenses') || '[]');
      localStorage.setItem('_prev_ledger_expenses', newValueString);
      if (oldExp.length === 0) return;

      if (newValue.length > oldExp.length) {
        const newExp = newValue[0];
        if (newExp) {
          const isLarge = newExp.amount >= 500;
          window.NotificationManager.add(
            isLarge ? "Large Expense Recorded" : "Financial Expense Registered",
            `${newExp.category} expense of $${newExp.amount.toFixed(2)} details recorded.`,
            "Finance & Ledger",
            isLarge ? "Warning" : "Information",
            isLarge ? "High" : "Low",
            newExp.id
          );
        }
      }
    }
  } catch (err) {
    console.error("Error monitoring storage updates", err);
  }
};

window.initializePrevStateCaches = function() {
  const keys = [
    { key: 'poultry_birds_list', prev: '_prev_birds_list' },
    { key: 'poultry_egg_records', prev: '_prev_egg_records' },
    { key: 'poultry_brooding_batches', prev: '_prev_brooding_batches' },
    { key: 'poultry_hatch_outcomes', prev: '_prev_hatch_outcomes' },
    { key: 'poultry_brooder_batches', prev: '_prev_brooder_batches' },
    { key: 'poultry_pairs_list', prev: '_prev_pairs_list' },
    { key: 'poultry_feed_inventory', prev: '_prev_feed_inventory' },
    { key: 'poultry_sales_history', prev: '_prev_sales_history' },
    { key: 'poultry_ledger_expenses', prev: '_prev_ledger_expenses' }
  ];
  keys.forEach(item => {
    if (!localStorage.getItem(item.prev)) {
      const current = localStorage.getItem(item.key);
      if (current) {
        localStorage.setItem(item.prev, current);
      }
    }
  });
};

// Global interceptor logic for localStorage actions
(function() {
  const originalSetItem = localStorage.setItem;
  localStorage.setItem = function(key, value) {
    originalSetItem.apply(this, arguments);
    if (key.startsWith('poultry_')) {
      setTimeout(() => {
        if (window.handleLocalStorageChange) {
          window.handleLocalStorageChange(key, value);
        }
      }, 50);
    }
  };
})();

// Navigation bell menu setup
window.setupNotificationBellDropdown = function() {
  const topNavRight = document.querySelector('.top-nav-right');
  if (!topNavRight) return;

  const existingBellBtn = topNavRight.querySelector('.nav-action-btn[onclick*="notifications.html"]') || 
                          topNavRight.querySelector('.nav-action-btn i.fa-bell')?.parentElement;

  if (!existingBellBtn) return;

  if (!document.getElementById("notif-bell-style")) {
    const styleEl = document.createElement("style");
    styleEl.id = "notif-bell-style";
    styleEl.innerHTML = `
      .notif-bell-container {
        position: relative;
        display: inline-block;
      }
      .notif-unread-badge {
        position: absolute;
        top: -4px;
        right: -4px;
        background: #EF4444;
        color: white;
        border-radius: 50%;
        padding: 2px 5px;
        font-size: 0.65rem;
        font-weight: 800;
        line-height: 1;
        min-width: 16px;
        text-align: center;
        box-shadow: 0 0 0 2px white;
        pointer-events: none;
      }
      .notif-dropdown {
        display: none;
        position: absolute;
        right: 0;
        top: 120%;
        width: 360px;
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(25px);
        -webkit-backdrop-filter: blur(25px);
        border: 1px solid var(--glass-border);
        border-radius: var(--border-radius-lg);
        box-shadow: 0 10px 40px rgba(15, 23, 42, 0.15);
        z-index: 1000000;
        overflow: hidden;
        flex-direction: column;
        animation: dropIn 0.2s cubic-bezier(0.19, 1, 0.22, 1) forwards;
      }
      @keyframes dropIn {
        from { transform: translateY(10px); opacity: 0; }
        to { transform: translateY(0); opacity: 1; }
      }
      .notif-dropdown-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px;
        border-bottom: 1px solid var(--neutral-light-gray);
        background: rgba(0, 0, 0, 0.02);
      }
      .notif-dropdown-list {
        max-height: 280px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
      }
      .notif-dropdown-item {
        display: flex;
        gap: 12px;
        padding: 12px 16px;
        border-bottom: 1px solid var(--neutral-light-gray);
        cursor: pointer;
        transition: background 0.2s ease;
        text-align: left;
        position: relative;
      }
      .notif-dropdown-item:hover {
        background: rgba(22, 163, 74, 0.04);
      }
      .notif-dropdown-item.unread {
        background: rgba(22, 163, 74, 0.02);
      }
      .notif-dropdown-item.unread::before {
        content: '';
        position: absolute;
        left: 6px;
        top: 50%;
        transform: translateY(-50%);
        width: 6px;
        height: 6px;
        background: var(--primary-green);
        border-radius: 50%;
      }
      .notif-dropdown-item-icon {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        font-size: 0.9rem;
      }
      .notif-icon-info { background: rgba(59, 130, 246, 0.1); color: #3B82F6; }
      .notif-icon-success { background: rgba(16, 185, 129, 0.1); color: #10B981; }
      .notif-icon-warning { background: rgba(245, 158, 11, 0.1); color: #F59E0B; }
      .notif-icon-critical { background: rgba(239, 68, 68, 0.1); color: #EF4444; }
      .notif-icon-reminder { background: rgba(139, 92, 246, 0.1); color: #8B5CF6; }
      .notif-dropdown-item-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 2px;
      }
      .notif-dropdown-item-title {
        font-size: 0.82rem;
        font-weight: 700;
        color: var(--neutral-dark);
      }
      .notif-dropdown-item-desc {
        font-size: 0.72rem;
        color: var(--neutral-gray);
        line-height: 1.35;
      }
      .notif-dropdown-item-time {
        font-size: 0.65rem;
        color: var(--light-brown);
        margin-top: 3px;
      }
      .notif-dropdown-empty {
        padding: 40px 20px;
        text-align: center;
        color: var(--neutral-gray);
        font-size: 0.8rem;
      }
    `;
    document.head.appendChild(styleEl);
  }

  // Check if wrapper is already present on click reload
  if (existingBellBtn.parentElement.classList.contains('notif-bell-container')) {
    return;
  }

  const wrapper = document.createElement('div');
  wrapper.className = 'notif-bell-container';
  existingBellBtn.parentNode.insertBefore(wrapper, existingBellBtn);
  wrapper.appendChild(existingBellBtn);

  existingBellBtn.removeAttribute('onclick');

  const badgeEl = document.createElement('span');
  badgeEl.className = 'notif-unread-badge';
  badgeEl.style.display = 'none';
  existingBellBtn.appendChild(badgeEl);

  const dropdownMenu = document.createElement('div');
  dropdownMenu.className = 'notif-dropdown';
  dropdownMenu.id = 'notif-dropdown-el';
  dropdownMenu.innerHTML = `
    <div class="notif-dropdown-header">
      <strong style="font-size: 0.85rem; color: var(--neutral-dark);">Alert Notifications</strong>
      <button id="notif-clear-all" style="font-size: 0.72rem; color: var(--primary-green-dark); border: none; background: none; cursor: pointer; font-weight: 700;">Mark all read</button>
    </div>
    <div class="notif-dropdown-list" id="notif-dropdown-list-el"></div>
    <div style="padding: 10px; border-top: 1px solid var(--neutral-light-gray); text-align: center; background: rgba(0,0,0,0.01);">
      <a href="notifications.html" style="font-size: 0.76rem; color: var(--primary-green); font-weight: 700; text-decoration: none; display: block;">View All Notifications</a>
    </div>
  `;
  wrapper.appendChild(dropdownMenu);

  function renderBellDropdown() {
    const listEl = dropdownMenu.querySelector('#notif-dropdown-list-el');
    const notifs = window.NotificationManager.getNotifications();
    const unread = notifs.filter(n => n.status === 'Unread');

    if (unread.length > 0) {
      badgeEl.textContent = unread.length;
      badgeEl.style.display = 'block';
    } else {
      badgeEl.style.display = 'none';
    }

    listEl.innerHTML = '';
    const displayList = notifs.filter(n => n.status !== 'Archived').slice(0, 5);

    if (displayList.length === 0) {
      listEl.innerHTML = `
        <div class="notif-dropdown-empty">
          <i class="fa-regular fa-bell-slash" style="font-size: 1.5rem; margin-bottom: 8px; opacity: 0.5;"></i>
          <div>No active notifications</div>
        </div>
      `;
    } else {
      displayList.forEach(n => {
        const item = document.createElement('div');
        item.className = `notif-dropdown-item ${n.status === 'Unread' ? 'unread' : ''}`;
        
        let typeClass = 'notif-icon-info';
        let typeIcon = 'fa-info-circle';
        if (n.type === 'Success') { typeClass = 'notif-icon-success'; typeIcon = 'fa-circle-check'; }
        else if (n.type === 'Warning') { typeClass = 'notif-icon-warning'; typeIcon = 'fa-triangle-exclamation'; }
        else if (n.type === 'Critical') { typeClass = 'notif-icon-critical'; typeIcon = 'fa-solid fa-triangle-exclamation'; } // Changed to avoid missing icons
        else if (n.type === 'Reminder') { typeClass = 'notif-icon-reminder'; typeIcon = 'fa-clock'; }

        const timeDiff = Date.now() - new Date(n.timestamp).getTime();
        let timeStr = 'Just now';
        const m = Math.floor(timeDiff / 60000);
        const h = Math.floor(m / 60);
        const d = Math.floor(h / 24);
        if (d > 0) timeStr = `${d} day${d === 1 ? '' : 's'} ago`;
        else if (h > 0) timeStr = `${h} hour${h === 1 ? '' : 's'} ago`;
        else if (m > 0) timeStr = `${m} minute${m === 1 ? '' : 's'} ago`;

        item.innerHTML = `
          <div class="notif-dropdown-item-icon ${typeClass}">
            <i class="fa-solid ${typeIcon}"></i>
          </div>
          <div class="notif-dropdown-item-content">
            <span class="notif-dropdown-item-title">${n.title}</span>
            <span class="notif-dropdown-item-desc">${n.description}</span>
            <span class="notif-dropdown-item-time">${timeStr}</span>
          </div>
          ${n.status === 'Unread' ? `<button class="btn-text item-mark-read" style="border:none; background:none; color:var(--neutral-gray); font-size:0.7rem; align-self:center; cursor:pointer;" onclick="event.stopPropagation(); window.NotificationManager.markAsRead('${n.id}');" title="Mark as read"><i class="fa-solid fa-check"></i></button>` : ''}
        `;

        item.addEventListener('click', (e) => {
          if (e.target.closest('.item-mark-read')) return;
          window.NotificationManager.viewRelatedRecord(n);
        });

        listEl.appendChild(item);
      });
    }
  }

  existingBellBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    const isOpen = dropdownMenu.style.display === 'flex';
    dropdownMenu.style.display = isOpen ? 'none' : 'flex';
  });

  document.addEventListener('click', (e) => {
    if (!wrapper.contains(e.target)) {
      dropdownMenu.style.display = 'none';
    }
  });

  dropdownMenu.querySelector('#notif-clear-all').addEventListener('click', (e) => {
    e.stopPropagation();
    window.NotificationManager.markAllAsRead();
  });

  window.addEventListener('poultry_notifications_updated', () => {
    renderBellDropdown();
  });

  renderBellDropdown();
};

// Deep linking functionality
window.setupDeepLinking = function() {
  const urlParams = new URLSearchParams(window.location.search);
  const refId = urlParams.get('refId') || urlParams.get('id');
  if (refId) {
    setTimeout(() => {
      const searchInputs = [
        document.getElementById('flock-wild-search'),
        document.getElementById('hen-search'),
        document.getElementById('feed-search'),
        document.getElementById('sales-search'),
        document.getElementById('ledger-search'),
        document.getElementById('notif-search')
      ];
      searchInputs.forEach(input => {
        if (input) {
          input.value = refId;
          input.dispatchEvent(new Event('input', { bubbles: true }));
          input.dispatchEvent(new Event('change', { bubbles: true }));
        }
      });

      if (window.location.pathname.includes('flock.html')) {
        const gridCard = Array.from(document.querySelectorAll('.flock-card')).find(c => c.innerHTML.includes(refId));
        if (gridCard) {
          const viewBtn = gridCard.querySelector('.btn-view-bio');
          if (viewBtn) viewBtn.click();
        } else {
          const row = Array.from(document.querySelectorAll('.flock-table-row')).find(r => r.innerHTML.includes(refId));
          if (row) {
            const viewBtn = row.querySelector('.btn-view');
            if (viewBtn) viewBtn.click();
          }
        }
      }

      if (window.location.pathname.includes('feed-management.html')) {
        const feedCard = Array.from(document.querySelectorAll('.feed-card')).find(c => c.innerHTML.includes(refId));
        if (feedCard) {
          const viewBtn = feedCard.querySelector('.btn-view-feed') || feedCard.querySelector('button');
          if (viewBtn) viewBtn.click();
        }
      }

      if (window.location.pathname.includes('finance.html')) {
        const trNode = Array.from(document.querySelectorAll('tr')).find(r => r.textContent.includes(refId));
        if (trNode) {
          const viewBtn = trNode.querySelector('.btn-view') || trNode.querySelector('button');
          if (viewBtn) viewBtn.click();
        }
      }

      if (window.location.pathname.includes('sales.html')) {
        const trNode = Array.from(document.querySelectorAll('tr')).find(r => r.textContent.includes(refId));
        if (trNode) {
          const viewBtn = trNode.querySelector('.btn-view-invoice') || trNode.querySelector('.btn-view') || trNode.querySelector('button');
          if (viewBtn) viewBtn.click();
        }
      }
    }, 550);
  }
};

document.addEventListener('DOMContentLoaded', () => {
  window.initializePrevStateCaches();
  window.setupNotificationBellDropdown();
  window.setupDeepLinking();

  // Initial Sync from backend
  if (window.NotificationManager && window.NotificationManager.syncWithBackend) {
    window.NotificationManager.syncWithBackend();
  }

  // Periodic polling every 60 seconds when document is visible
  let pollInterval = setInterval(() => {
    if (document.visibilityState === 'visible' && window.NotificationManager && window.NotificationManager.syncWithBackend) {
      window.NotificationManager.syncWithBackend();
    }
  }, 60000);

  // Tab visibility changes sync instantly
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && window.NotificationManager && window.NotificationManager.syncWithBackend) {
      window.NotificationManager.syncWithBackend();
    }
  });
});
