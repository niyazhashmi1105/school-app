import { Router } from 'express';
import bcrypt from 'bcryptjs';
import { z } from 'zod';
import { query } from '../db';
import { asyncHandler } from '../utils/asyncHandler';
import { ConflictError, UnauthorizedError, ValidationError } from '../utils/errors';
import { requireAuth, signToken } from '../middleware/auth';

const router = Router();

const signupSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  username: z.string().min(3, 'Username must be at least 3 characters'),
  email: z.string().email('A valid email is required'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

router.post(
  '/signup',
  asyncHandler(async (req, res) => {
    const parsed = signupSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());

    const { name, username, email, password } = parsed.data;

    const existing = await query('SELECT id FROM users WHERE username = $1 OR email = $2', [username, email]);
    if (existing.rows.length > 0) throw new ConflictError('Username or email already registered');

    const passwordHash = await bcrypt.hash(password, 10);
    const inserted = await query<{ id: string }>(
      'INSERT INTO users (name, username, email, password_hash) VALUES ($1, $2, $3, $4) RETURNING id',
      [name, username, email, passwordHash]
    );

    const token = signToken({ sub: inserted.rows[0].id, username, name });
    res.status(201).json({ token, user: { name, username, email } });
  })
);

const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});

router.post(
  '/login',
  asyncHandler(async (req, res) => {
    const parsed = loginSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());

    const { username, password } = parsed.data;
    const result = await query<{ id: string; name: string; username: string; email: string; password_hash: string }>(
      'SELECT id, name, username, email, password_hash FROM users WHERE username = $1',
      [username]
    );

    const user = result.rows[0];
    const passwordMatches = user ? await bcrypt.compare(password, user.password_hash) : false;
    if (!user || !passwordMatches) throw new UnauthorizedError('Invalid username or password');

    const token = signToken({ sub: user.id, username: user.username, name: user.name });
    res.json({ token, user: { name: user.name, username: user.username, email: user.email } });
  })
);

router.post('/logout', requireAuth, (_req, res) => {
  // Stateless JWT — nothing to invalidate server-side; client discards the token.
  res.status(204).send();
});

router.get(
  '/me',
  requireAuth,
  asyncHandler(async (req, res) => {
    const result = await query<{ name: string; username: string; email: string }>(
      'SELECT name, username, email FROM users WHERE id = $1',
      [req.user!.sub]
    );
    if (result.rows.length === 0) throw new UnauthorizedError('User no longer exists');
    res.json({ user: result.rows[0] });
  })
);

const resetSchema = z.object({
  username: z.string().min(1),
});

router.post(
  '/reset-password',
  asyncHandler(async (req, res) => {
    const parsed = resetSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());

    const defaultPassword = process.env.ADMIN_RESET_PASSWORD || 'ChangeMe123!';
    const passwordHash = await bcrypt.hash(defaultPassword, 10);

    const result = await query('UPDATE users SET password_hash = $1 WHERE username = $2 RETURNING id', [
      passwordHash,
      parsed.data.username,
    ]);

    if (result.rowCount === 0) {
      // Do not reveal whether the username exists.
      res.status(202).json({ message: 'If that account exists, its password has been reset.' });
      return;
    }

    res.status(202).json({ message: 'Password reset. Use the configured default password to log in.' });
  })
);

export default router;
