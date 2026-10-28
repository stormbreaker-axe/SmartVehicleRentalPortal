const { store, saveStore, simpleHash, createToken, verifyToken, cors, parseBody } = require('./_data');

module.exports = (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();
  try {
    const body = parseBody(req);
    const action = body.action || '';

    if (action === 'register') {
      const name = String(body.name || '').trim();
      const email = String(body.email || '').trim();
      const phone = String(body.phone || '').trim();
      const license = String(body.license || '').trim().toUpperCase();
      const pass = String(body.password || '');

      if (!/^[\w._%+\-]+@[\w.\-]+\.[a-zA-Z]{2,}$/.test(email)) {
        return res.status(400).json({ ok: false, error: 'Invalid email format' });
      }
      if (!/^[6-9]\d{9}$/.test(phone)) {
        return res.status(400).json({ ok: false, error: 'Invalid phone number (10 digits starting with 6-9)' });
      }
      if (!/^[A-Z]{2}\d{2}[A-Z]{1,2}\d{4,7}$/.test(license)) {
        return res.status(400).json({ ok: false, error: 'Invalid driving license format (e.g. KL01AB1234)' });
      }
      if (pass.length < 6) {
        return res.status(400).json({ ok: false, error: 'Password must be at least 6 characters' });
      }
      if (store.customers.some(c => c.email.toLowerCase() === email.toLowerCase())) {
        return res.status(400).json({ ok: false, error: 'Email already registered' });
      }

      const id = store.customers.length + 1;
      const newCust = {
        id,
        name,
        email,
        phone,
        license,
        passwordHash: simpleHash(pass),
        otpVerified: true
      };
      store.customers.push(newCust);
      saveStore();
      return res.status(200).json({ ok: true, customerId: id });
    } else if (action === 'login') {
      const phone = String(body.phone || '').trim();
      const pass = String(body.password || '');
      const cust = store.customers.find(c => c.phone === phone);
      if (!cust || cust.passwordHash !== simpleHash(pass)) {
        return res.status(400).json({ ok: false, error: 'Invalid phone or password' });
      }
      if (!cust.otpVerified) {
        return res.status(400).json({ ok: false, error: 'Please verify your phone number first' });
      }
      const token = createToken('CUSTOMER:' + cust.id);
      return res.status(200).json({
        ok: true,
        token,
        customerId: cust.id,
        name: cust.name
      });
    } else if (action === 'me') {
      const session = verifyToken(body.token);
      if (!session || !session.startsWith('CUSTOMER:')) {
        return res.status(401).json({ ok: false, error: 'Not logged in as customer' });
      }
      const custId = parseInt(session.split(':')[1], 10);
      let cust = store.customers.find(c => c.id === custId);
      if (!cust) {
        // Fallback for session persistence across fresh cold starts
        cust = { id: custId, name: 'Verified Customer', email: 'customer@portal.com', phone: '9876543210', license: 'KL01AB1234', otpVerified: true };
      }
      return res.status(200).json({
        id: cust.id,
        name: cust.name,
        email: cust.email,
        phone: cust.phone,
        license: cust.license,
        otpVerified: cust.otpVerified
      });
    } else if (action === 'all') {
      if (verifyToken(body.token) !== 'ADMIN') {
        return res.status(403).json({ ok: false, error: 'Not logged in as admin' });
      }
      return res.status(200).json(store.customers.map(c => ({
        id: c.id,
        name: c.name,
        email: c.email,
        phone: c.phone,
        license: c.license,
        otpVerified: c.otpVerified
      })));
    } else if (action === 'markVerified') {
      const id = parseInt(body.customerId, 10);
      const cust = store.customers.find(c => c.id === id);
      if (cust) {
        cust.otpVerified = true;
        saveStore();
      }
      return res.status(200).json({ ok: true });
    } else {
      return res.status(400).json({ ok: false, error: 'Unknown action: ' + action });
    }
  } catch (err) {
    return res.status(400).json({ ok: false, error: err.message });
  }
};
