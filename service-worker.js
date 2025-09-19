const CACHE_NAME = 'tecnoit-portal-v2'; // Bump version to ensure update
const urlsToCache = [
  '/',
  'index.html',
  'manifest.json', // Add manifest
  'assets/logo.png', // Add logo
  'https://cdn.tailwindcss.com',
  'https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap',
  'https://unpkg.com/react@18/umd/react.development.js',
  'https://unpkg.com/react-dom@18/umd/react-dom.development.js',
  'https://unpkg.com/@babel/standalone/babel.min.js',
  'https://cdn.skypack.dev/lucide-react',
  'https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js', // Pre-cache firebase libs
  'https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js',
  'https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js'
];

// Install event: cache all core assets
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      console.log('Opened cache');
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
  // We only want to cache GET requests.
  if (event.request.method !== 'GET') {
    return;
  }

  event.respondWith(
    caches.match(event.request).then(response => {
      // Cache hit - return response
      if (response) {
        return response;
      }

      // Not in cache - fetch from network
      return fetch(event.request).then(
        (networkResponse) => {
          // Check if we received a valid response
          if(!networkResponse || (networkResponse.status !== 200 && networkResponse.status !== 0) ) {
            return networkResponse;
          }

          // IMPORTANT: Clone the response. A response is a stream
          // and because we want the browser to consume the response
          // as well as the cache consuming the response, we need
          // to clone it so we have two streams.
          const responseToCache = networkResponse.clone();

          caches.open(CACHE_NAME)
            .then(cache => {
              // We don't cache API calls to the GLPI proxy
              if(event.request.url.indexOf('/api/proxy/') === -1) {
                 cache.put(event.request, responseToCache);
              }
            });

          return networkResponse;
        }
      ).catch(err => {
        // Network request failed, try to get it from the cache.
        // This part is redundant if we always check cache first, but good for safety.
        return caches.match(event.request);
      });
    })
  );
});
