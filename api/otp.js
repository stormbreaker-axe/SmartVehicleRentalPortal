const { cors, parseBody, createToken, verifyToken } = require('./_data');
const crypto = require('crypto');

const OTP_SECRET = 'svrp-otp-signing-key-2026';

function signOtp(phone, otp) {
  // Encode: phone|otp|expiresAt signed with HMAC
  const expiresAt = Date.now() + 10 * 60 * 1000; // 10 minutes
  const payload = `${phone}|${otp}|${expiresAt}`;
  const sig = crypto.createHmac('sha256', OTP_SECRET).update(payload).digest('hex').substring(0, 20);
  return Buffer.from(`${payload}|${sig}`).toString('base64url');
}

function verifyOtpToken(token, phone, otp) {
  try {
    const raw = Buffer.from(token, 'base64url').toString('utf8');
    const parts = raw.split('|');
    if (parts.length !== 4) return false;
    const [tPhone, tOtp, tExpiry, tSig] = parts;
    // Check expiry
    if (Date.now() > parseInt(tExpiry, 10)) return false;
    // Check phone and otp match
    if (tPhone !== phone || tOtp !== otp) return false;
    // Verify signature
    const payload = `${tPhone}|${tOtp}|${tExpiry}`;
    const expected = crypto.createHmac('sha256', OTP_SECRET).update(payload).digest('hex').substring(0, 20);
    return tSig === expected;
  } catch (_) {
    return false;
  }
}

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
      // Sign the OTP into a token that the browser must return for verification
      // This makes verification completely stateless — no in-memory store needed
      const otpToken = signOtp(phone, otp);
      return res.status(200).json({
        ok: true,
        message: `OTP sent to ${phone}`,
        otp,         // shown in toast / auto-filled (demo mode)
        otpToken     // browser must send this back during verify
      });

    } else if (action === 'verify') {
      const phone = String(body.phone || '').trim();
      const otp   = String(body.otp || '').trim();
      const otpToken = String(body.otpToken || '').trim();

      // Primary: stateless token verification (works across all containers)
      if (otpToken && verifyOtpToken(otpToken, phone, otp)) {
        return res.status(200).json({ ok: true });
      }

      // Fallback: if somehow no token was passed, accept with warning (demo mode)
      if (!otpToken && otp && otp.length === 6) {
        console.warn('[OTP] No otpToken passed — allowing demo bypass for phone:', phone);
        return res.status(200).json({ ok: true });
      }

      return res.status(400).json({ ok: false, error: 'Invalid or expired OTP' });

    } else {
      return res.status(400).json({ ok: false, error: 'Unknown OTP action' });
    }
  } catch (err) {
    return res.status(400).json({ ok: false, error: err.message });
  }
};
