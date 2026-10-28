// Shared data store for Vercel Serverless environment
const globalData = globalThis.__SVRP_STORE__ || {
  vehicles: [
    { id: 1, type: "CAR", make: "Toyota", model: "Etios", plate: "KL01AB1234", rate: 1800, available: true, batteryRange: null, engineCc: 1496, colour: "White", fuelType: "PETROL", numSeats: 5, kmsTravelled: 24000, imageUrl: null },
    { id: 2, type: "CAR", make: "Hyundai", model: "i20", plate: "KL01CD5678", rate: 1600, available: true, batteryRange: null, engineCc: 1197, colour: "Red", fuelType: "PETROL", numSeats: 5, kmsTravelled: 18000, imageUrl: null },
    { id: 3, type: "BIKE", make: "Honda", model: "Hornet 2.0", plate: "KL02EF1111", rate: 700, available: true, batteryRange: null, engineCc: 184, colour: "Black", fuelType: "PETROL", numSeats: 2, kmsTravelled: 12000, imageUrl: null },
    { id: 4, type: "BIKE", make: "Yamaha", model: "MT-15", plate: "KL02GH2222", rate: 850, available: true, batteryRange: null, engineCc: 155, colour: "Blue", fuelType: "PETROL", numSeats: 2, kmsTravelled: 8000, imageUrl: null },
    { id: 5, type: "ELECTRIC_SCOOTER", make: "Ather", model: "450X", plate: "KL03IJ3333", rate: 600, available: true, batteryRange: 150, engineCc: null, colour: "Grey", fuelType: "ELECTRIC", numSeats: 2, kmsTravelled: 5000, imageUrl: null },
    { id: 6, type: "ELECTRIC_SCOOTER", make: "Ola", model: "S1 Pro", plate: "KL03KL4444", rate: 650, available: true, batteryRange: 176, engineCc: null, colour: "Silver", fuelType: "ELECTRIC", numSeats: 2, kmsTravelled: 3000, imageUrl: null }
  ],
  customers: [],
  reservations: [],
  payments: [],
  otps: {},
  sessions: {}
};

globalThis.__SVRP_STORE__ = globalData;

function simpleHash(pass) {
  let h = 0x12345678;
  for (let i = 0; i < pass.length; i++) {
    h = (Math.imul(h, 31) ^ pass.charCodeAt(i)) | 0;
  }
  return (h >>> 0).toString(16);
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

module.exports = { store: globalData, simpleHash, cors, parseBody };
