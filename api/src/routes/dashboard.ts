import { Router } from 'express';
import { query } from '../db';
import { asyncHandler } from '../utils/asyncHandler';

const router = Router();

router.get(
  '/summary',
  asyncHandler(async (_req, res) => {
    const [studentsCount, feeTotals, stockCount, outOfStock] = await Promise.all([
      query<{ count: string }>('SELECT COUNT(*)::text AS count FROM students'),
      query<{ collected: string; dues: string }>(
        `SELECT COALESCE(SUM(amount_paid), 0) AS collected, COALESCE(SUM(due_amount), 0) AS dues FROM fees`
      ),
      query<{ count: string }>('SELECT COUNT(*)::text AS count FROM stock'),
      query<{ count: string }>('SELECT COUNT(*)::text AS count FROM stock WHERE remaining_stock <= 0'),
    ]);

    res.json({
      totalStudents: Number(studentsCount.rows[0].count),
      totalCollected: Number(feeTotals.rows[0].collected),
      totalDues: Number(feeTotals.rows[0].dues),
      totalStock: Number(stockCount.rows[0].count),
      outOfStockCount: Number(outOfStock.rows[0].count),
    });
  })
);

export default router;
