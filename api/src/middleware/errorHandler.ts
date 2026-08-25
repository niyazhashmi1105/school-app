import { NextFunction, Request, Response } from 'express';
import { AppError } from '../utils/errors';

interface PgError extends Error {
  code?: string;
  constraint?: string;
  detail?: string;
}

export function notFoundHandler(req: Request, res: Response) {
  res.status(404).json({
    error: { code: 'ROUTE_NOT_FOUND', message: `No route for ${req.method} ${req.originalUrl}` },
  });
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function errorHandler(err: unknown, req: Request, res: Response, _next: NextFunction) {
  if (err instanceof AppError) {
    res.status(err.statusCode).json({
      error: { code: err.code, message: err.message, details: err.details },
    });
    return;
  }

  // Malformed JSON body from express.json()
  if (err instanceof SyntaxError && 'body' in (err as unknown as Record<string, unknown>)) {
    res.status(400).json({ error: { code: 'INVALID_JSON', message: 'Request body is not valid JSON' } });
    return;
  }

  const pgErr = err as PgError;
  if (pgErr && typeof pgErr.code === 'string') {
    switch (pgErr.code) {
      case '23505': // unique_violation
        res.status(409).json({
          error: { code: 'CONFLICT', message: 'A record with these details already exists', details: pgErr.detail },
        });
        return;
      case '23503': // foreign_key_violation
        res.status(400).json({
          error: { code: 'INVALID_REFERENCE', message: 'Referenced record does not exist', details: pgErr.detail },
        });
        return;
      case '23502': // not_null_violation
      case '23514': // check_violation
        res.status(400).json({
          error: { code: 'BAD_REQUEST', message: 'Request data failed a database constraint', details: pgErr.detail },
        });
        return;
      case 'ECONNREFUSED':
        res.status(503).json({ error: { code: 'DB_UNAVAILABLE', message: 'Database is unavailable, please try again shortly' } });
        return;
    }
  }

  console.error('Unhandled error:', err);
  res.status(500).json({ error: { code: 'INTERNAL_ERROR', message: 'Something went wrong on our end' } });
}
