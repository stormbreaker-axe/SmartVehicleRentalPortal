const { store, cors, parseBody } = require('./_data');

module.exports = (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();
  try {
    const body = parseBody(req);
    const action = body.action || '';
    if (action === 'send') {
      const phone = String(body.phone || '').trim();
      if (!/^[6-9]\d{9}$/.test(phone)) {
        return res.status(400).json({ ok: false, error: 'Invalid phone number (10 digits starting with 6-9)' });
      }
      const otp = String(Math.floor(100000 + Math.random() * 900000));
      store.otps[phone] = { otp, expiresAt: Date.now() + 10 * 60 * 1000 };
      return res.status(200).json({ ok: true, message: `OTP sent to ${phone}`, otp });
    } else if (action === 'verify') {
      const phone = String(body.phone || '').trim();
      const otp = String(body.otp || '').trim();
      const record = store.otps[phone];
      if (!record || record.otp !== otp || Date.now() > record.expiresAt) {
        return res.status(400).json({ ok: false, error: 'Invalid or expired OTP' });
      }
      return res.status(200).json({ ok: true });
    } else {
      return res.status(400).json({ ok: false, error: 'Unknown OTP action' });
    }
  } catch (err) {
    return res.status(400).json({ ok: false, error: err.message });
  }
};
