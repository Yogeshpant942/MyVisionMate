const mongoose = require('mongoose');
const express = require('express');
const app = express();
const dotenv = require("dotenv");
const userRoutes = require('./routes/UserRoutes');
const guardianRoutes = require('./routes/GuardianRoutes');
const connectDb = require('./config/db');
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.get("/", (req, res) => {
  res.send("Server is running");
});
dotenv.config();
connectDb();
app.use('/api/auth',userRoutes);
app.use('/api/guardian',guardianRoutes);
const PORT = 8080;

app.listen(PORT, "0.0.0.0", () => {
  console.log("server running on the port 8080");
});

