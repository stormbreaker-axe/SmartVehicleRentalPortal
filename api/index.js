const vehiclesHandler = require('./vehicles');
const otpHandler = require('./otp');
const customerHandler = require('./customer');
const reservationsHandler = require('./reservations');
const adminHandler = require('./admin');
const paymentHandler = require('./payment');
const { cors } = require('./_data');

module.exports = (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();

  const url = (req.url || '').toLowerCase();
  if (url.includes('vehicle')) return vehiclesHandler(req, res);
  if (url.includes('otp')) return otpHandler(req, res);
  if (url.includes('customer')) return customerHandler(req, res);
  if (url.includes('reservation')) return reservationsHandler(req, res);
  if (url.includes('admin')) return adminHandler(req, res);
  if (url.includes('payment')) return paymentHandler(req, res);

  return res.status(200).json({ ok: true, message: 'SVRP API is active' });
};
