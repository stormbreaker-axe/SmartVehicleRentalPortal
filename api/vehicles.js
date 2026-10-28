const { store, cors } = require('./_data');

module.exports = (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') return res.status(204).end();
  return res.status(200).json(store.vehicles);
};
