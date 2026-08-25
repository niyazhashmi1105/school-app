import { Router } from 'express';
import { z } from 'zod';
import { query } from '../db';
import { asyncHandler } from '../utils/asyncHandler';
import { NotFoundError, ValidationError } from '../utils/errors';

const router = Router();
const GRACE_DAYS = Number(process.env.LICENSE_GRACE_DAYS ?? 14);

interface LicenseRow {
  plan_name: string;
  start_date: string;
  expiry_date: string;
  last_renewed_date: string | null;
  renewed_by: string | null;
}

function shapeStatus(row: LicenseRow) {
  const expiry = new Date(row.expiry_date);
  const now = new Date();
  const daysRemaining = Math.ceil((expiry.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  const hardCutoff = new Date(expiry);
  hardCutoff.setDate(hardCutoff.getDate() + GRACE_DAYS);

  let status: 'active' | 'grace_period' | 'expired';
  if (now <= expiry) status = 'active';
  else if (now <= hardCutoff) status = 'grace_period';
  else status = 'expired';

  return {
    planName: row.plan_name,
    startDate: row.start_date,
    expiryDate: row.expiry_date,
    lastRenewedDate: row.last_renewed_date,
    renewedBy: row.renewed_by,
    status,
    daysRemaining,
  };
}

router.get(
  '/status',
  asyncHandler(async (_req, res) => {
    const result = await query<LicenseRow>('SELECT * FROM license WHERE id = 1');
    if (result.rows.length === 0) throw new NotFoundError('No license has been provisioned for this installation');
    res.json(shapeStatus(result.rows[0]));
  })
);

const renewSchema = z.object({
  extendByDays: z.number().int().positive().optional().default(365),
  note: z.string().optional(),
});

router.post(
  '/renew',
  asyncHandler(async (req, res) => {
    const parsed = renewSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());

    const existing = await query<LicenseRow>('SELECT * FROM license WHERE id = 1');
    const renewedBy = req.user?.username ?? 'system';
    const today = new Date();

    // Renewing extends from whichever is later: today, or the current expiry
    // date (so renewing early doesn't lose the remaining paid-for days).
    const base =
      existing.rows.length > 0 && new Date(existing.rows[0].expiry_date) > today
        ? new Date(existing.rows[0].expiry_date)
        : today;
    const newExpiry = new Date(base);
    newExpiry.setDate(newExpiry.getDate() + parsed.data.extendByDays);
    const newExpiryStr = newExpiry.toISOString().slice(0, 10);
    const previousExpiry = existing.rows[0]?.expiry_date ?? null;

    if (existing.rows.length === 0) {
      await query(
        `INSERT INTO license (id, plan_name, start_date, expiry_date, last_renewed_date, renewed_by)
         VALUES (1, 'Annual Plan', $1, $2, $1, $3)`,
        [today.toISOString().slice(0, 10), newExpiryStr, renewedBy]
      );
    } else {
      await query(
        `UPDATE license SET expiry_date = $1, last_renewed_date = $2, renewed_by = $3 WHERE id = 1`,
        [newExpiryStr, today.toISOString().slice(0, 10), renewedBy]
      );
    }

    await query(
      `INSERT INTO license_history (previous_expiry_date, new_expiry_date, renewed_by, note)
       VALUES ($1, $2, $3, $4)`,
      [previousExpiry, newExpiryStr, renewedBy, parsed.data.note ?? null]
    );

    const updated = await query<LicenseRow>('SELECT * FROM license WHERE id = 1');
    res.json(shapeStatus(updated.rows[0]));
  })
);

router.get(
  '/history',
  asyncHandler(async (_req, res) => {
    const result = await query(
      'SELECT id, renewed_on, previous_expiry_date, new_expiry_date, renewed_by, note FROM license_history ORDER BY renewed_on DESC'
    );
    res.json({ history: result.rows });
  })
);

export default router;
