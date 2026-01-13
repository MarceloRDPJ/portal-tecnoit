const CACHE_NAME = 'tecnoit-portal-v3'; // Bump version again
const urlsToCache = [
  './',
  './index.html',
  './manifest.json',
  './assets/logo.png',
  'https://cdn.tailwindcss.com',
  'https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap',
  'https://unpkg.com/react@18/umd/react.development.js',
  'https://unpkg.com/react-dom@18/umd/react-dom.development.js',
  'https://unpkg.com/@babel/standalone/babel.min.js',
  'https://cdn.skypack.dev/lucide-react',
  'https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js',
  'https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js',
  'https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js'
];

// Install event: cache all core assets
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(urlsToCache);
    })
  );
});

// Activate event: clean up old caches
self.addEventListener('activate', event => {
  const cacheWhitelist = [CACHE_NAME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});

// Fetch event: serve from cache first, then network
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') {
    return;
  }

  event.respondWith(
    caches.match(event.request).then(response => {
      if (response) {
        return response; // Cache hit
      }

      // Not in cache, fetch from network
      return fetch(event.request).then(networkResponse => {
          if (!networkResponse || networkResponse.status !== 200) {
              if(networkResponse.type === 'opaque') {
                  // Opaque responses are for cross-origin requests. We can't see the status.
                  // We'll cache them to allow offline access to CDN scripts.
              } else {
                  return networkResponse;
              }
          }

          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then(cache => {
              // We don't cache API calls to the GLPI proxy
              if (event.request.url.indexOf('/api/proxy/') === -1) {
                  cache.put(event.request, responseToCache);
              }
          });
          return networkResponse;
      });
    })
  );
});
