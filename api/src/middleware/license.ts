import { NextFunction, Request, Response } from 'express';
import { query } from '../db';
import { LicenseExpiredError } from '../utils/errors';

const GRACE_DAYS = Number(process.env.LICENSE_GRACE_DAYS ?? 14);

interface LicenseRow {
  expiry_date: string;
}

/**
 * Enforced server-side so it can't be bypassed by changing the device clock.
 * Applied after requireAuth; skipped on the license routes themselves so an
 * expired school can still check status / renew.
 */
export async function enforceLicense(req: Request, _res: Response, next: NextFunction) {
  try {
    const result = await query<LicenseRow>('SELECT expiry_date FROM license WHERE id = 1');
    if (result.rows.length === 0) {
      // No license row provisioned yet — fail safe by allowing access rather
      // than locking out a fresh install; /license/status will report this.
      return next();
    }

    const expiryDate = new Date(result.rows[0].expiry_date);
    const hardCutoff = new Date(expiryDate);
    hardCutoff.setDate(hardCutoff.getDate() + GRACE_DAYS);

    if (new Date() > hardCutoff) {
      return next(new LicenseExpiredError());
    }

    next();
  } catch (err) {
    next(err);
  }
}
