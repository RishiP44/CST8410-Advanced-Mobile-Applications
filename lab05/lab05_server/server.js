const express = require("express");

const app = express();
app.use(express.json());

function toNumber(value) {
  const n = Number(value);
  return Number.isNaN(n) ? null : n;
}

// POST /calc  (Android sends JSON: { "a": 5, "b": 2, "op": "add" })
app.post("/calc", (req, res) => {
  const a = toNumber(req.body.a);
  const b = toNumber(req.body.b);
  const op = String(req.body.op || "").toLowerCase();

  if (a === null || b === null) {
    return res.status(400).json({ error: "Invalid numbers" });
  }

  let result;
  if (op === "add") result = a + b;
  else if (op === "subtract") result = a - b;
  else if (op === "multiply") result = a * b;
  else if (op === "divide") {
    if (b === 0) return res.status(400).json({ error: "Division by zero" });
    result = a / b;
  } else {
    return res.status(400).json({ error: "Invalid operation" });
  }

  res.json({ a, b, op, result });
});

// GET /calc?a=5&b=2&op=add  (QR/browser opens this)
app.get("/calc", (req, res) => {
  const a = toNumber(req.query.a);
  const b = toNumber(req.query.b);
  const op = String(req.query.op || "").toLowerCase();

  if (a === null || b === null) {
    return res.status(400).json({ error: "Invalid numbers" });
  }

  let result;
  if (op === "add") result = a + b;
  else if (op === "subtract") result = a - b;
  else if (op === "multiply") result = a * b;
  else if (op === "divide") {
    if (b === 0) return res.status(400).json({ error: "Division by zero" });
    result = a / b;
  } else {
    return res.status(400).json({ error: "Invalid operation" });
  }

  res.json({ a, b, op, result });
});

const PORT = 8080;

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server running on http://localhost:${PORT}`);
  console.log(`For emulator use: http://10.0.2.2:${PORT}`);
});
