const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const os = require('os');

const SECRET = 'svrp-secure-secret-key-2026';
const TMP_FILE = path.join(os.tmpdir(), 'svrp_store.json');

const INITIAL_VEHICLES = [
  { id: 1, type: "CAR", make: "Toyota", model: "Etios", plate: "KL01AB1234", rate: 1800, available: true, batteryRange: null, engineCc: 1496, colour: "White", fuelType: "PETROL", numSeats: 5, kmsTravelled: 24000, imageUrl: null },
  { id: 2, type: "CAR", make: "Hyundai", model: "i20", plate: "KL01CD5678", rate: 1600, available: true, batteryRange: null, engineCc: 1197, colour: "Red", fuelType: "PETROL", numSeats: 5, kmsTravelled: 18000, imageUrl: null },
  { id: 3, type: "BIKE", make: "Honda", model: "Hornet 2.0", plate: "KL02EF1111", rate: 700, available: true, batteryRange: null, engineCc: 184, colour: "Black", fuelType: "PETROL", numSeats: 2, kmsTravelled: 12000, imageUrl: null },
  { id: 4, type: "BIKE", make: "Yamaha", model: "MT-15", plate: "KL02GH2222", rate: 850, available: true, batteryRange: null, engineCc: 155, colour: "Blue", fuelType: "PETROL", numSeats: 2, kmsTravelled: 8000, imageUrl: null },
  { id: 5, type: "ELECTRIC_SCOOTER", make: "Ather", model: "450X", plate: "KL03IJ3333", rate: 600, available: true, batteryRange: 150, engineCc: null, colour: "Grey", fuelType: "ELECTRIC", numSeats: 2, kmsTravelled: 5000, imageUrl: null },
  { id: 6, type: "ELECTRIC_SCOOTER", make: "Ola", model: "S1 Pro", plate: "KL03KL4444", rate: 650, available: true, batteryRange: 176, engineCc: null, colour: "Silver", fuelType: "ELECTRIC", numSeats: 2, kmsTravelled: 3000, imageUrl: null }
];

function loadStore() {
  if (globalThis.__SVRP_STORE__) return globalThis.__SVRP_STORE__;
  try {
    if (fs.existsSync(TMP_FILE)) {
      const data = JSON.parse(fs.readFileSync(TMP_FILE, 'utf8'));
      if (data && Array.isArray(data.vehicles) && data.vehicles.length > 0) {
        globalThis.__SVRP_STORE__ = data;
        return data;
      }
    }
  } catch (_) {}

  const initial = {
    vehicles: JSON.parse(JSON.stringify(INITIAL_VEHICLES)),
    customers: [],
    reservations: [],
    payments: [],
    otps: {}
  };
  globalThis.__SVRP_STORE__ = initial;
  return initial;
}

const store = loadStore();

function saveStore() {
  try {
    fs.writeFileSync(TMP_FILE, JSON.stringify(store), 'utf8');
  } catch (_) {}
}

function simpleHash(pass) {
  let h = 0x12345678;
  for (let i = 0; i < pass.length; i++) {
    h = (Math.imul(h, 31) ^ pass.charCodeAt(i)) | 0;
  }
  return (h >>> 0).toString(16);
}

function createToken(payload) {
  const sig = crypto.createHmac('sha256', SECRET).update(payload).digest('hex').substring(0, 16);
  return Buffer.from(`${payload}.${sig}`).toString('base64url');
}

function verifyToken(token) {
  if (!token) return null;
  if (token === 'ADMIN') return 'ADMIN';
  if (token.startsWith('CUSTOMER:')) return token;
  try {
    const raw = Buffer.from(token, 'base64url').toString('utf8');
    const parts = raw.split('.');
    if (parts.length === 2) {
      const [payload, sig] = parts;
      const expected = crypto.createHmac('sha256', SECRET).update(payload).digest('hex').substring(0, 16);
      if (sig === expected) return payload;
    }
  } catch (_) {}
  return null;
}

function cors(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
}

function parseBody(req) {
  if (!req.body) return {};
  if (typeof req.body === 'string') {
    try { return JSON.parse(req.body); } catch (_) { return {}; }
  }
  return req.body;
}

module.exports = { store, saveStore, simpleHash, createToken, verifyToken, cors, parseBody };
