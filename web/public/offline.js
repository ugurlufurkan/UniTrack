/* No tokens or private HTML are cached. This copy is explicitly enabled by its owner. */
(async () => {
  const status = document.getElementById('status');
  const request = indexedDB.open('unitrack-device', 1);
  request.onupgradeneeded = () => request.result.createObjectStore('preferences');
  request.onerror = () => {
    status.textContent = 'Bu cihazda çevrimdışı kopya yok.';
  };
  request.onsuccess = () => {
    const db = request.result;
    db.onversionchange = () => db.close();
    const tx = db.transaction('preferences', 'readonly');
    const store = tx.objectStore('preferences');
    const enabled = store.get('enabled'),
      snapshot = store.get('snapshot');
    tx.oncomplete = () => {
      db.close();
      const data = snapshot.result;
      if (
        !enabled.result ||
        !data ||
        !Number.isFinite(data.savedAt) ||
        !Array.isArray(data.courses) ||
        Date.now() - data.savedAt > 7 * 86400000
      ) {
        status.textContent =
          'Geçerli çevrimdışı kopya yok. İnternet bağlantısıyla giriş yapıp ayarlardan etkinleştirebilirsin.';
        return;
      }
      status.textContent = `Son kayıt: ${new Date(data.savedAt).toLocaleString('tr-TR')}. Bu kopya güncel olmayabilir.`;
      document.getElementById('snapshot').hidden = false;
      for (const course of data.courses) {
        const row = document.createElement('div');
        row.className = 'course';
        const title = document.createElement('strong');
        title.textContent = course.name;
        const detail = document.createElement('small');
        detail.textContent = `${course.credit} kredi · ${course.letterGrade || 'Not girilmedi'}`;
        row.append(title, detail);
        document.getElementById('courses').append(row);
      }
      document.getElementById('clear').onclick = () => {
        const open = indexedDB.open('unitrack-device', 1);
        open.onsuccess = () => {
          const clear = open.result.transaction('preferences', 'readwrite');
          clear.objectStore('preferences').delete('snapshot');
          clear.objectStore('preferences').put(false, 'enabled');
          clear.oncomplete = () => {
            open.result.close();
            location.reload();
          };
        };
      };
    };
  };
})();
