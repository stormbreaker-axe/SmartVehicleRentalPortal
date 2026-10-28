const { store, saveStore, cors, parseBody } = require('./_data');

module.exports = (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();

  try {
    store.vehicles = store.vehicles || [];

    // Support client syncing local custom vehicles across serverless containers
    if (req.method === 'POST') {
      const body = parseBody(req);
      if (Array.isArray(body.customVehicles)) {
        body.customVehicles.forEach(cv => {
          if (cv && cv.id) {
            const idx = store.vehicles.findIndex(v => v.id === cv.id || (cv.plate && v.plate === cv.plate));
            if (idx >= 0) {
              store.vehicles[idx] = Object.assign({}, store.vehicles[idx], cv);
            } else {
              store.vehicles.push(cv);
            }
          }
        });
        saveStore();
      }
    }
  } catch (_) {}

  return res.status(200).json(store.vehicles || []);
};
