const { store, saveStore, verifyToken, cors, parseBody } = require('./_data');

module.exports = (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();
  try {
    const body = parseBody(req);
    const session = verifyToken(body.token);
    if (!session || !session.startsWith('CUSTOMER:')) {
      return res.status(401).json({ ok: false, error: 'Not logged in as customer' });
    }
    const rid = parseInt(body.reservationId, 10);
    const amt = parseFloat(body.amount);
    const method = String(body.method || '').toUpperCase();
    if (!['CASH', 'CARD', 'UPI', 'NET_BANKING'].includes(method)) {
      return res.status(400).json({ ok: false, error: 'Invalid payment method' });
    }
    const r = store.reservations.find(res => res.id === rid);
    if (!r) return res.status(404).json({ ok: false, error: 'Reservation not found' });
    if (r.status !== 'CONFIRMED') return res.status(400).json({ ok: false, error: 'Not payable' });
    if (amt < r.totalCost) return res.status(400).json({ ok: false, error: `Amount below total: Rs.${r.totalCost}` });

    const pid = store.payments.length + 1;
    store.payments.push({ id: pid, reservationId: rid, amount: amt, method, status: 'PAID' });
    saveStore();
    return res.status(200).json({ ok: true, paymentId: pid });
  } catch (err) {
    return res.status(400).json({ ok: false, error: err.message });
  }
};
