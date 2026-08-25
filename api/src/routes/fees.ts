import { Router } from 'express';
import { z } from 'zod';
import { query } from '../db';
import { asyncHandler } from '../utils/asyncHandler';
import { NotFoundError, ValidationError } from '../utils/errors';

const router = Router();

const feeSchema = z.object({
  regNo: z.string().min(1, 'Student registration number is required'),
  feeType: z.enum(['Admission', 'Tuition', 'Both'], { errorMap: () => ({ message: 'feeType must be Admission, Tuition, or Both' }) }),
  month: z.string().optional().default(''),
  totalAmount: z.number().nonnegative('totalAmount must be >= 0'),
  amountPaid: z.number().nonnegative('amountPaid must be >= 0'),
  paymentDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'paymentDate must be YYYY-MM-DD'),
});

interface FeeRow {
  id: string;
  reg_no: string;
  student_name: string;
  fee_type: string;
  month: string | null;
  total_amount: string;
  amount_paid: string;
  due_amount: string;
  payment_date: string;
}

function toApiFee(row: FeeRow) {
  return {
    id: row.id,
    regNo: row.reg_no,
    studentName: row.student_name,
    feeType: row.fee_type,
    month: row.month || '',
    totalAmount: Number(row.total_amount),
    amountPaid: Number(row.amount_paid),
    dueAmount: Number(row.due_amount),
    paymentDate: row.payment_date,
  };
}

const FEE_SELECT = `
  SELECT f.id, f.reg_no, s.name AS student_name, f.fee_type, f.month,
         f.total_amount, f.amount_paid, f.due_amount, f.payment_date
  FROM fees f
  JOIN students s ON s.reg_no = f.reg_no
`;

router.get(
  '/',
  asyncHandler(async (req, res) => {
    const search = (req.query.search as string | undefined)?.trim();
    const rows = search
      ? (
          await query<FeeRow>(
            `${FEE_SELECT} WHERE f.reg_no ILIKE $1 OR s.name ILIKE $1 ORDER BY f.created_at DESC`,
            [`%${search}%`]
          )
        ).rows
      : (await query<FeeRow>(`${FEE_SELECT} ORDER BY f.created_at DESC`)).rows;

    res.json({ fees: rows.map(toApiFee) });
  })
);

router.get(
  '/summary',
  asyncHandler(async (_req, res) => {
    const result = await query<{ receivable: string; received: string; pending: string }>(
      `SELECT
         COALESCE(SUM(total_amount), 0) AS receivable,
         COALESCE(SUM(amount_paid), 0) AS received,
         COALESCE(SUM(due_amount), 0) AS pending
       FROM fees`
    );
    const row = result.rows[0];
    res.json({
      totalReceivable: Number(row.receivable),
      totalReceived: Number(row.received),
      totalPending: Number(row.pending),
    });
  })
);

router.post(
  '/',
  asyncHandler(async (req, res) => {
    const parsed = feeSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());
    const f = parsed.data;

    if (f.amountPaid > f.totalAmount) throw new ValidationError({ amountPaid: 'Cannot exceed totalAmount' });

    const student = await query('SELECT 1 FROM students WHERE reg_no = $1', [f.regNo]);
    if (student.rows.length === 0) throw new NotFoundError(`Student ${f.regNo} not found`);

    const dueAmount = Math.max(0, f.totalAmount - f.amountPaid);
    const inserted = await query<{ id: string }>(
      `INSERT INTO fees (reg_no, fee_type, month, total_amount, amount_paid, due_amount, payment_date)
       VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id`,
      [f.regNo, f.feeType, f.month, f.totalAmount, f.amountPaid, dueAmount, f.paymentDate]
    );

    const result = await query<FeeRow>(`${FEE_SELECT} WHERE f.id = $1`, [inserted.rows[0].id]);
    res.status(201).json({ fee: toApiFee(result.rows[0]) });
  })
);

router.put(
  '/:id',
  asyncHandler(async (req, res) => {
    const parsed = feeSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());
    const f = parsed.data;

    if (f.amountPaid > f.totalAmount) throw new ValidationError({ amountPaid: 'Cannot exceed totalAmount' });

    const student = await query('SELECT 1 FROM students WHERE reg_no = $1', [f.regNo]);
    if (student.rows.length === 0) throw new NotFoundError(`Student ${f.regNo} not found`);

    const dueAmount = Math.max(0, f.totalAmount - f.amountPaid);
    const updated = await query(
      `UPDATE fees SET reg_no = $1, fee_type = $2, month = $3, total_amount = $4,
         amount_paid = $5, due_amount = $6, payment_date = $7, updated_at = now()
       WHERE id = $8 RETURNING id`,
      [f.regNo, f.feeType, f.month, f.totalAmount, f.amountPaid, dueAmount, f.paymentDate, req.params.id]
    );
    if (updated.rowCount === 0) throw new NotFoundError(`Fee record ${req.params.id} not found`);

    const result = await query<FeeRow>(`${FEE_SELECT} WHERE f.id = $1`, [req.params.id]);
    res.json({ fee: toApiFee(result.rows[0]) });
  })
);

router.delete(
  '/:id',
  asyncHandler(async (req, res) => {
    const result = await query('DELETE FROM fees WHERE id = $1', [req.params.id]);
    if (result.rowCount === 0) throw new NotFoundError(`Fee record ${req.params.id} not found`);
    res.status(204).send();
  })
);

export default router;
