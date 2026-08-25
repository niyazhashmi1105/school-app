import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import bcrypt from 'bcryptjs';
import fs from 'fs';
import path from 'path';
import pool, { query } from './db';
import { requireAuth } from './middleware/auth';
import { enforceLicense } from './middleware/license';
import { errorHandler, notFoundHandler } from './middleware/errorHandler';

import authRoutes from './routes/auth';
import studentRoutes from './routes/students';
import feeRoutes from './routes/fees';
import stockRoutes from './routes/stock';
import dashboardRoutes from './routes/dashboard';
import backupRoutes from './routes/backup';
import licenseRoutes from './routes/license';

const app = express();
const PORT = Number(process.env.PORT ?? 4000);

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '5mb' }));

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

app.use('/api/auth', authRoutes);

// License status/renew/history stay reachable without an active license so an
// expired school can still see why it's locked and renew.
app.use('/api/license', requireAuth, licenseRoutes);

app.use('/api/students', requireAuth, enforceLicense, studentRoutes);
app.use('/api/fees', requireAuth, enforceLicense, feeRoutes);
app.use('/api/stock', requireAuth, enforceLicense, stockRoutes);
app.use('/api/dashboard', requireAuth, enforceLicense, dashboardRoutes);
app.use('/api/backup', requireAuth, enforceLicense, backupRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

async function runMigrations() {
  const schemaPath = path.join(__dirname, 'db', 'schema.sql');
  const schema = fs.readFileSync(schemaPath, 'utf-8');
  await pool.query(schema);
}

async function seedDefaults() {
  const adminUsername = process.env.SEED_ADMIN_USERNAME || 'admin';
  const existing = await query('SELECT 1 FROM users WHERE username = $1', [adminUsername]);
  if (existing.rows.length === 0) {
    const password = process.env.SEED_ADMIN_PASSWORD || 'ChangeMe123!';
    const passwordHash = await bcrypt.hash(password, 10);
    await query('INSERT INTO users (name, username, email, password_hash) VALUES ($1, $2, $3, $4)', [
      'Admin',
      adminUsername,
      process.env.SEED_ADMIN_EMAIL || 'admin@school.com',
      passwordHash,
    ]);
    console.log(`Seeded default admin user "${adminUsername}". Set SEED_ADMIN_PASSWORD to control its password.`);
  }

  const license = await query('SELECT 1 FROM license WHERE id = 1');
  if (license.rows.length === 0) {
    const start = new Date().toISOString().slice(0, 10);
    const expiry = new Date();
    expiry.setDate(expiry.getDate() + 365);
    await query('INSERT INTO license (id, plan_name, start_date, expiry_date) VALUES (1, $1, $2, $3)', [
      'Annual Plan',
      start,
      expiry.toISOString().slice(0, 10),
    ]);
    console.log('Seeded a 1-year license starting today.');
  }
}

async function start() {
  // Retry DB connection briefly so the API container can come up before
  // Postgres finishes initializing in docker-compose.
  const maxAttempts = 15;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      await pool.query('SELECT 1');
      break;
    } catch (err) {
      if (attempt === maxAttempts) throw err;
      console.log(`Waiting for database... (attempt ${attempt}/${maxAttempts})`);
      await new Promise((resolve) => setTimeout(resolve, 2000));
    }
  }

  await runMigrations();
  await seedDefaults();

  app.listen(PORT, () => {
    console.log(`School App API listening on port ${PORT}`);
  });
}

start().catch((err) => {
  console.error('Failed to start server:', err);
  process.exit(1);
});

process.on('unhandledRejection', (reason) => {
  console.error('Unhandled promise rejection:', reason);
});

process.on('uncaughtException', (err) => {
  console.error('Uncaught exception:', err);
  process.exit(1);
});
