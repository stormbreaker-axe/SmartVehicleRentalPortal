const { store, saveStore, verifyToken, cors, parseBody } = require('./_data');

module.exports = (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();
  try {
    const body = parseBody(req);
    const action = body.action || '';

    if (action === 'reserve') {
      const session = verifyToken(body.token);
      if (!session || !session.startsWith('CUSTOMER:')) {
        return res.status(401).json({ ok: false, error: 'Not logged in as customer' });
      }
      const custId = parseInt(session.split(':')[1], 10);
      const vehId = parseInt(body.vehicleId, 10);
      const s = body.startDate;
      const e = body.endDate;
      if (!s || !e || e < s) return res.status(400).json({ ok: false, error: 'Invalid dates' });
      const v = store.vehicles.find(veh => veh.id === vehId);
      if (!v) return res.status(404).json({ ok: false, error: 'Vehicle not found' });
      if (!v.available) return res.status(400).json({ ok: false, error: 'Vehicle not available' });

      // Conflict check
      const conflict = store.reservations.some(r => r.vehicleId === vehId && r.status === 'CONFIRMED' && r.startDate <= e && r.endDate >= s);
      if (conflict) return res.status(400).json({ ok: false, error: 'Vehicle already booked for these dates' });

      const d1 = new Date(s), d2 = new Date(e);
      const days = Math.round((d2 - d1) / (1000 * 60 * 60 * 24)) + 1;
      const cost = v.rate * days;
      const rid = store.reservations.length + 1;
      const reservation = {
        id: rid,
        customerId: custId,
        vehicleId: vehId,
        startDate: s,
        endDate: e,
        totalCost: cost,
        status: 'CONFIRMED',
        actualReturnDate: null,
        fineAmount: 0,
        fineRatePerDay: 0,
        overdueFlagged: false
      };
      store.reservations.push(reservation);
      saveStore();
      return res.status(200).json({ ok: true, reservationId: rid, totalCost: cost });
    } else if (action === 'check') {
      const vehId = parseInt(body.vehicleId, 10);
      const s = body.startDate, e = body.endDate;
      const v = store.vehicles.find(veh => veh.id === vehId);
      if (!v) return res.status(404).json({ ok: false, error: 'Vehicle not found' });
      const conflict = store.reservations.some(r => r.vehicleId === vehId && r.status === 'CONFIRMED' && r.startDate <= e && r.endDate >= s);
      const d1 = new Date(s), d2 = new Date(e);
      const days = Math.round((d2 - d1) / (1000 * 60 * 60 * 24)) + 1;
      const cost = v.rate * (days > 0 ? days : 1);
      return res.status(200).json({ available: v.available && !conflict, cost });
    } else if (action === 'mine') {
      const session = verifyToken(body.token);
      if (!session || !session.startsWith('CUSTOMER:')) {
        return res.status(401).json({ ok: false, error: 'Not logged in as customer' });
      }
      const custId = parseInt(session.split(':')[1], 10);
      // Return ALL statuses for this customer, newest first
      const mine = [...store.reservations]
        .filter(r => r.customerId === custId)
        .sort((a, b) => b.id - a.id);
      return res.status(200).json(mine);
    } else if (action === 'all') {
      if (verifyToken(body.token) !== 'ADMIN') {
        return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      }
      // Return ALL reservations (CONFIRMED, CANCELLED, COMPLETED) sorted newest first
      const all = [...store.reservations].sort((a, b) => b.id - a.id);
      return res.status(200).json(all);
    } else if (action === 'cancel') {
      const rid = parseInt(body.reservationId, 10);
      const r = store.reservations.find(res => res.id === rid);
      if (!r || r.status !== 'CONFIRMED') {
        return res.status(400).json({ ok: false, error: 'Reservation not found or already cancelled' });
      }
      r.status = 'CANCELLED';
      saveStore();
      return res.status(200).json({ ok: true });
    } else if (action === 'markReturn') {
      if (verifyToken(body.token) !== 'ADMIN') return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      const rid = parseInt(body.reservationId, 10);
      const r = store.reservations.find(res => res.id === rid);
      if (!r) return res.status(404).json({ ok: false, error: 'Reservation not found' });
      const returnDate = body.returnDate;
      const ratePerDay = parseFloat(body.fineRatePerDay || '0');
      const lateDays = Math.max(0, Math.round((new Date(returnDate) - new Date(r.endDate)) / (1000 * 60 * 60 * 24)));
      const fine = lateDays * ratePerDay;
      r.actualReturnDate = returnDate;
      r.fineAmount = fine;
      r.fineRatePerDay = ratePerDay;
      r.status = 'COMPLETED';
      saveStore();
      return res.status(200).json({ ok: true, fine, lateDays });
    } else if (action === 'flagOverdue') {
      if (verifyToken(body.token) !== 'ADMIN') return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      const rid = parseInt(body.reservationId, 10);
      const r = store.reservations.find(res => res.id === rid);
      if (r) {
        r.overdueFlagged = true;
        saveStore();
      }
      return res.status(200).json({ ok: true });
    } else if (action === 'overdue') {
      if (verifyToken(body.token) !== 'ADMIN') return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      const today = new Date().toISOString().slice(0, 10);
      const list = store.reservations.filter(r => r.status === 'CONFIRMED' && r.endDate < today && !r.actualReturnDate && !r.overdueFlagged);
      return res.status(200).json(list);
    } else if (action === 'severelyOverdue') {
      if (verifyToken(body.token) !== 'ADMIN') return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      const today = new Date();
      const list = store.reservations.filter(r => {
        if (r.status !== 'CONFIRMED' || r.actualReturnDate) return false;
        const diff = Math.floor((today - new Date(r.endDate)) / (1000 * 60 * 60 * 24));
        return diff >= 7;
      });
      return res.status(200).json(list);
    } else if (action === 'legalReport') {
      if (verifyToken(body.token) !== 'ADMIN') return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      const rid = parseInt(body.reservationId, 10);
      const r = store.reservations.find(res => res.id === rid);
      if (!r) return res.status(404).json({ ok: false, error: 'Reservation not found' });
      const c = store.customers.find(cust => cust.id === r.customerId);
      const v = store.vehicles.find(veh => veh.id === r.vehicleId);
      const overdueDays = Math.max(0, Math.floor((new Date() - new Date(r.endDate)) / (1000 * 60 * 60 * 24)));
      return res.status(200).json({
        reservationId: rid,
        customerName: c ? c.name : 'Unknown',
        customerEmail: c ? c.email : 'Unknown',
        customerPhone: c ? c.phone : 'Unknown',
        licenseNumber: c ? c.license : 'Unknown',
        vehicleType: v ? v.type : 'Unknown',
        vehicleMake: v ? v.make : 'Unknown',
        vehicleModel: v ? v.model : 'Unknown',
        vehiclePlate: v ? v.plate : 'Unknown',
        startDate: r.startDate,
        endDate: r.endDate,
        overdueDays,
        totalCost: r.totalCost,
        fineAmount: r.fineAmount || 0,
        reportDate: new Date().toISOString().slice(0, 10)
      });
    } else {
      return res.status(400).json({ ok: false, error: 'Unknown action: ' + action });
    }
  } catch (err) {
    return res.status(400).json({ ok: false, error: err.message });
  }
};
