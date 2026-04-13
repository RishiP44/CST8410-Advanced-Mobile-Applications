const express = require("express");
const cors = require("cors");
const path = require("path");
const fs = require("fs");

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json({ limit: "20mb" }));
app.use(express.urlencoded({ extended: true, limit: "20mb" }));

let readings = [];

app.get("/", (req, res) => {
  const htmlRows = readings
    .map((reading, index) => {
      const imageHtml = reading.photoBase64
        ? `<img src="data:image/jpeg;base64,${reading.photoBase64}" alt="Reading Photo" style="width:150px;height:auto;border:1px solid #ccc;" />`
        : `<span>No photo</span>`;

      return `
        <div style="border:1px solid #ccc; padding:16px; margin-bottom:16px; border-radius:10px;">
          <h3>Reading ${index + 1}</h3>
          <p><strong>Time:</strong> ${new Date(reading.timestamp).toLocaleString()}</p>
          <p><strong>Ambient Light:</strong> ${reading.ambientLight}</p>
          <p><strong>Proximity:</strong> ${reading.proximity}</p>
          <p><strong>Local Device Name:</strong> ${reading.localDeviceName}</p>
          <p><strong>Local Device UUID:</strong> ${reading.localDeviceUuid}</p>
          <p><strong>Remote Device Name:</strong> ${reading.remoteDeviceName}</p>
          <p><strong>Remote Device UUID:</strong> ${reading.remoteDeviceUuid}</p>
          <div>${imageHtml}</div>
        </div>
      `;
    })
    .join("");

  res.send(`
    <html>
      <head>
        <title>Photo History</title>
      </head>
      <body style="font-family:Arial; padding:20px;">
        <h1>Server Photo History</h1>
        ${readings.length === 0 ? "<p>No readings stored.</p>" : htmlRows}
      </body>
    </html>
  `);
});

app.post("/api/readings", (req, res) => {
  const reading = req.body;
  readings.unshift(reading);
  res.status(200).json({ message: "Reading stored successfully" });
});

app.post("/api/readings/clear", (req, res) => {
  readings = [];
  res.status(200).json({ message: "Server history cleared successfully" });
});

app.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}`);
});