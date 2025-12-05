/**
 * 🔧 SERVICE WORKER - EDL SafeWork PWA
 * 
 * Chức năng:
 * 1. Cache các file static để chạy offline
 * 2. Network-first strategy cho API calls
 * 3. Cache-first strategy cho assets
 */

const CACHE_NAME = 'safework-v2';
const STATIC_CACHE = 'safework-static-v2';
const DYNAMIC_CACHE = 'safework-dynamic-v2';

// Files to cache immediately on install
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/location.html',
  '/positioning-2d.html',
  '/alerts.html',
  '/employees.html',
  '/reports.html',
  '/manage-employees.html',
  '/manage-helmets.html',
  '/css/style.css',
  '/css/employees.css',
  '/js/location.js',
  '/js/script.js',
  '/js/alerts.js',
  '/js/reports.js',
  '/js/employees.js',
  '/js/manage-helmets.js',
  '/js/global-alerts.js',
  '/js/vietnam-time.js',
  '/js/mobile-nav.js',
  '/images/icon-192.png',
  '/images/icon-512.png',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js',
  'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'
];

// Install event - cache static assets
self.addEventListener('install', event => {
  console.log('📦 Service Worker: Installing...');
  
  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then(cache => {
        console.log('📦 Service Worker: Caching static assets');
        return cache.addAll(STATIC_ASSETS);
      })
      .then(() => {
        console.log('✅ Service Worker: Installed successfully');
        return self.skipWaiting();
      })
      .catch(err => {
        console.error('❌ Service Worker: Install failed', err);
      })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', event => {
  console.log('🔄 Service Worker: Activating...');
  
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames
          .filter(cacheName => cacheName !== STATIC_CACHE && cacheName !== DYNAMIC_CACHE)
          .map(cacheName => {
            console.log('🗑️ Service Worker: Deleting old cache', cacheName);
            return caches.delete(cacheName);
          })
      );
    }).then(() => {
      console.log('✅ Service Worker: Activated');
      return self.clients.claim();
    })
  );
});

// Fetch event - serve from cache or network
self.addEventListener('fetch', event => {
  const { request } = event;
  const url = new URL(request.url);
  
  // Skip non-GET requests
  if (request.method !== 'GET') {
    return;
  }
  
  // Skip WebSocket connections
  if (url.protocol === 'ws:' || url.protocol === 'wss:') {
    return;
  }
  
  // Skip API calls - always go to network
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/ws')) {
    event.respondWith(networkFirst(request));
    return;
  }
  
  // For static assets - cache first
  event.respondWith(cacheFirst(request));
});

/**
 * Cache-first strategy
 * Try cache first, fallback to network
 */
async function cacheFirst(request) {
  try {
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    
    const networkResponse = await fetch(request);
    
    // Cache successful responses
    if (networkResponse.ok) {
      const cache = await caches.open(DYNAMIC_CACHE);
      cache.put(request, networkResponse.clone());
    }
    
    return networkResponse;
  } catch (error) {
    console.error('❌ Fetch failed:', error);
    
    // Return offline page if available
    const offlineResponse = await caches.match('/index.html');
    if (offlineResponse) {
      return offlineResponse;
    }
    
    return new Response('Offline - Vui lòng kiểm tra kết nối mạng', {
      status: 503,
      statusText: 'Service Unavailable'
    });
  }
}

/**
 * Network-first strategy
 * Try network first, fallback to cache
 */
async function networkFirst(request) {
  try {
    const networkResponse = await fetch(request);
    
    // Cache successful responses
    if (networkResponse.ok) {
      const cache = await caches.open(DYNAMIC_CACHE);
      cache.put(request, networkResponse.clone());
    }
    
    return networkResponse;
  } catch (error) {
    console.log('📴 Network failed, trying cache...');
    
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    
    return new Response(JSON.stringify({ error: 'Offline' }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' }
    });
  }
}

// Listen for messages from the main thread
self.addEventListener('message', event => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

// ==================== PUSH NOTIFICATIONS (PWA) ====================

/**
 * Handle notification click - open app and navigate to alert page
 */
self.addEventListener('notificationclick', event => {
  console.log('🔔 Notification clicked:', event.notification.tag);
  
  event.notification.close();
  
  // Get URL from notification data or default to location page
  const urlToOpen = event.notification.data?.url || '/location.html';
  
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then(windowClients => {
        // Check if app is already open
        for (let client of windowClients) {
          if (client.url.includes(self.registration.scope)) {
            // App is open, focus it and navigate
            client.focus();
            return client.navigate(urlToOpen);
          }
        }
        // App is not open, open new window
        return clients.openWindow(urlToOpen);
      })
  );
});

/**
 * Handle notification close
 */
self.addEventListener('notificationclose', event => {
  console.log('🔔 Notification closed:', event.notification.tag);
});

/**
 * Handle push events (for future Web Push implementation)
 * This requires VAPID keys and a push service
 */
self.addEventListener('push', event => {
  console.log('📲 Push received:', event);
  
  if (event.data) {
    const data = event.data.json();
    
    const title = data.title || '🚨 Cảnh Báo Mới';
    const options = {
      body: data.body || 'Có cảnh báo mới từ hệ thống SafeWork',
      icon: '/images/icon-192.png',
      badge: '/images/icon-72.png',
      vibrate: [200, 100, 200, 100, 200],
      tag: data.tag || 'safework-alert',
      requireInteraction: data.critical || false,
      data: {
        url: data.url || '/location.html',
        alertId: data.alertId
      }
    };
    
    event.waitUntil(
      self.registration.showNotification(title, options)
    );
  }
});

console.log('🔧 Service Worker: Loaded (with Push Notification support)');
