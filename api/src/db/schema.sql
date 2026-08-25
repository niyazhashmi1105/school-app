-- Tender Buds School Management System - schema
-- Applied idempotently on API startup.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  username TEXT UNIQUE NOT NULL,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS students (
  reg_no TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  class TEXT NOT NULL,
  father_name TEXT NOT NULL,
  phone TEXT NOT NULL,
  admission_date DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS fees (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reg_no TEXT NOT NULL REFERENCES students(reg_no) ON UPDATE CASCADE ON DELETE CASCADE,
  fee_type TEXT NOT NULL,
  month TEXT,
  total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
  amount_paid NUMERIC(12, 2) NOT NULL CHECK (amount_paid >= 0),
  due_amount NUMERIC(12, 2) NOT NULL CHECK (due_amount >= 0),
  payment_date DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fees_reg_no ON fees(reg_no);

CREATE TABLE IF NOT EXISTS stock (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  item_type TEXT NOT NULL,        -- Book | Dress
  category TEXT NOT NULL,         -- e.g. English Book, Summer Uniform
  sub_category TEXT NOT NULL DEFAULT '',
  gender TEXT NOT NULL DEFAULT '',
  class TEXT NOT NULL DEFAULT '',
  size TEXT NOT NULL DEFAULT '',
  item_name TEXT NOT NULL,
  total_quantity INTEGER NOT NULL CHECK (total_quantity >= 0),
  quantity_sold INTEGER NOT NULL CHECK (quantity_sold >= 0),
  remaining_stock INTEGER NOT NULL CHECK (remaining_stock >= 0),
  date DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (item_type, category, sub_category, gender, class, size)
);

CREATE TABLE IF NOT EXISTS uniform_sizes (
  piece TEXT NOT NULL,
  size TEXT NOT NULL,
  PRIMARY KEY (piece, size)
);

CREATE TABLE IF NOT EXISTS license (
  id INTEGER PRIMARY KEY DEFAULT 1,
  plan_name TEXT NOT NULL DEFAULT 'Annual Plan',
  start_date DATE NOT NULL,
  expiry_date DATE NOT NULL,
  last_renewed_date DATE,
  renewed_by TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT license_singleton CHECK (id = 1)
);

CREATE TABLE IF NOT EXISTS license_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  renewed_on TIMESTAMPTZ NOT NULL DEFAULT now(),
  previous_expiry_date DATE,
  new_expiry_date DATE NOT NULL,
  renewed_by TEXT,
  note TEXT
);
