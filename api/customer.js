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

      if (!name) {
        return res.status(400).json({ ok: false, error: 'Name is required' });
      }
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

      store.customers = store.customers || [];
      const existing = store.customers.find(c => c.phone === phone || c.email.toLowerCase() === email.toLowerCase());
      if (existing) {
        existing.name = name;
        existing.email = email;
        existing.license = license;
        existing.passwordHash = simpleHash(pass);
        existing.otpVerified = true;
        saveStore();
        return res.status(200).json({ ok: true, customerId: existing.id });
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
      const hash = simpleHash(pass);

      store.customers = store.customers || [];
      let cust = store.customers.find(c => c.phone === phone);

      // Rehydrate if client passed cached registration data
      if (!cust && body.customerData && body.customerData.phone === phone) {
        cust = {
          id: body.customerData.id || (store.customers.length + 1),
          name: body.customerData.name || 'Customer',
          email: body.customerData.email || '',
          phone: body.customerData.phone,
          license: body.customerData.license || '',
          passwordHash: body.customerData.passwordHash || simpleHash(body.customerData.password || pass),
          otpVerified: true
        };
        store.customers.push(cust);
        saveStore();
      }

      // If user registered with this password in client cache, sync the hash
      if (cust && cust.passwordHash !== hash && body.customerData && (body.customerData.password === pass || body.customerData.passwordHash === hash)) {
        cust.passwordHash = hash;
        saveStore();
      }

      if (!cust || cust.passwordHash !== hash) {
        return res.status(400).json({ ok: false, error: 'Invalid phone or password' });
      }

      const token = createToken('CUSTOMER:' + cust.id);
      return res.status(200).json({
        ok: true,
        token,
        customerId: cust.id,
        name: cust.name
      });

    } else if (action === 'me') {
      const sess = verifyToken(body.token);
      if (!sess || !sess.startsWith('CUSTOMER:')) {
        return res.status(401).json({ ok: false, error: 'Not logged in as customer' });
      }
      const custId = parseInt(sess.split(':')[1], 10);
      store.customers = store.customers || [];
      let cust = store.customers.find(c => c.id === custId);
      if (!cust) {
        cust = { id: custId, name: 'Customer #' + custId, email: '', phone: '', license: '', otpVerified: true };
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
      store.customers = store.customers || [];
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
      store.customers = store.customers || [];
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
