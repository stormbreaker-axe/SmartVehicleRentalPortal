const { store, saveStore, createToken, verifyToken, cors, parseBody } = require('./_data');

module.exports = (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();
  try {
    const body = parseBody(req);
    const action = body.action || '';

    if (action === 'login') {
      const u = body.username;
      const p = body.password;
      if (u === 'admin' && p === 'admin123') {
        const token = createToken('ADMIN');
        return res.status(200).json({ ok: true, token });
      }
      return res.status(400).json({ ok: false, error: 'Invalid admin credentials' });
    } else if (action === 'addVehicle') {
      if (verifyToken(body.token) !== 'ADMIN') {
        return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      }
      store.vehicles = store.vehicles || [];
      const maxId = store.vehicles.length > 0 ? Math.max(...store.vehicles.map(v => v.id || 0)) : 6;
      const id = maxId + 1;
      const v = {
        id,
        type: String(body.vehicleType || '').toUpperCase(),
        make: String(body.make || '').trim(),
        model: String(body.model || '').trim(),
        plate: String(body.plate || '').trim().toUpperCase(),
        rate: parseFloat(body.rate) || 0,
        available: true,
        batteryRange: body.batteryRange ? parseInt(body.batteryRange, 10) : null,
        engineCc: body.engineCc ? parseInt(body.engineCc, 10) : null,
        colour: body.colour || 'Unknown',
        fuelType: body.fuelType || 'PETROL',
        numSeats: parseInt(body.numSeats || '2', 10),
        kmsTravelled: parseInt(body.kmsTravelled || '0', 10),
        imageUrl: null
      };
      store.vehicles.push(v);
      saveStore();
      return res.status(200).json({ ok: true, vehicleId: id, vehicle: v });
    } else if (action === 'setAvailability') {
      if (verifyToken(body.token) !== 'ADMIN') {
        return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      }
      const id = parseInt(body.vehicleId, 10);
      const avail = String(body.available) === 'true';
      const v = store.vehicles.find(veh => veh.id === id);
      if (v) v.available = avail;
      saveStore();
      return res.status(200).json({ ok: true });
    } else {
      return res.status(400).json({ ok: false, error: 'Unknown admin action: ' + action });
    }
  } catch (err) {
    return res.status(400).json({ ok: false, error: err.message });
  }
};
