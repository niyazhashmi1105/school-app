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

interface StudentFeeStatusRow {
  reg_no: string;
  name: string;
  class: string;
  total_fees: string;
  total_paid: string;
  total_due: string;
}

router.get(
  '/student-fee-status',
  asyncHandler(async (req, res) => {
    // Per-student totals across all their fee records, computed here (not in
    // the client) so it scales the same way regardless of how many students
    // or fee records exist.
    const search = (req.query.search as string | undefined)?.trim();
    const result = await query<StudentFeeStatusRow>(
      `SELECT s.reg_no, s.name, s.class,
              COALESCE(SUM(f.total_amount), 0) AS total_fees,
              COALESCE(SUM(f.amount_paid), 0) AS total_paid,
              COALESCE(SUM(f.due_amount), 0) AS total_due
       FROM students s
       LEFT JOIN fees f ON f.reg_no = s.reg_no
       ${search ? 'WHERE s.reg_no ILIKE $1' : ''}
       GROUP BY s.reg_no, s.name, s.class, s.created_at
       ORDER BY s.created_at DESC`,
      search ? [`%${search}%`] : []
    );

    const studentFeeStatus = result.rows.map((row) => {
      const totalDue = Number(row.total_due);
      return {
        regNo: row.reg_no,
        name: row.name,
        class: row.class,
        totalFees: Number(row.total_fees),
        totalPaid: Number(row.total_paid),
        totalDue,
        status: totalDue === 0 ? 'Paid' : 'Pending',
      };
    });

    res.json({ studentFeeStatus });
  })
);

export default router;
