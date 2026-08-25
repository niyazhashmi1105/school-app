import { Router } from 'express';
import { z } from 'zod';
import { query, withTransaction } from '../db';
import { asyncHandler } from '../utils/asyncHandler';
import { ValidationError } from '../utils/errors';

const router = Router();

router.get(
  '/export',
  asyncHandler(async (_req, res) => {
    const [students, fees, stock] = await Promise.all([
      query('SELECT * FROM students ORDER BY reg_no'),
      query('SELECT * FROM fees ORDER BY created_at'),
      query('SELECT * FROM stock ORDER BY created_at'),
    ]);

    res.setHeader('Content-Disposition', `attachment; filename="TenderBuds_Backup_${new Date().toISOString().slice(0, 10)}.json"`);
    res.json({
      appName: 'Tender Buds School Management System',
      exportDate: new Date().toISOString(),
      version: '1.0',
      data: { students: students.rows, fees: fees.rows, stock: stock.rows },
    });
  })
);

const importSchema = z.object({
  data: z.object({
    students: z.array(z.record(z.unknown())).optional().default([]),
    fees: z.array(z.record(z.unknown())).optional().default([]),
    stock: z.array(z.record(z.unknown())).optional().default([]),
  }),
});

router.post(
  '/import',
  asyncHandler(async (req, res) => {
    const parsed = importSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());
    const { students, fees, stock } = parsed.data.data;

    const summary = await withTransaction(async (client) => {
      let addedStudents = 0;
      let skippedStudents = 0;
      for (const s of students) {
        const regNo = s.reg_no ?? s.regNo;
        if (!regNo) {
          skippedStudents++;
          continue;
        }
        const result = await client.query(
          `INSERT INTO students (reg_no, name, class, father_name, phone, admission_date)
           VALUES ($1, $2, $3, $4, $5, $6)
           ON CONFLICT (reg_no) DO NOTHING`,
          [regNo, s.name ?? '', s.class ?? '', s.father_name ?? s.fatherName ?? '', s.phone ?? '', s.admission_date ?? s.admissionDate ?? null]
        );
        if (result.rowCount && result.rowCount > 0) addedStudents++;
        else skippedStudents++;
      }

      let addedFees = 0;
      let skippedFees = 0;
      for (const f of fees) {
        const id = f.id;
        const regNo = f.reg_no ?? f.regNo;
        if (!id || !regNo) {
          skippedFees++;
          continue;
        }
        const studentExists = await client.query('SELECT 1 FROM students WHERE reg_no = $1', [regNo]);
        if (studentExists.rows.length === 0) {
          skippedFees++;
          continue;
        }
        const totalAmount = Number(f.total_amount ?? f.totalAmount ?? 0);
        const amountPaid = Number(f.amount_paid ?? f.amountPaid ?? 0);
        const dueAmount = Number(f.due_amount ?? f.dueAmount ?? Math.max(0, totalAmount - amountPaid));
        const result = await client.query(
          `INSERT INTO fees (id, reg_no, fee_type, month, total_amount, amount_paid, due_amount, payment_date)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
           ON CONFLICT (id) DO NOTHING`,
          [id, regNo, f.fee_type ?? f.feeType ?? '', f.month ?? '', totalAmount, amountPaid, dueAmount, f.payment_date ?? f.paymentDate ?? null]
        );
        if (result.rowCount && result.rowCount > 0) addedFees++;
        else skippedFees++;
      }

      let addedStock = 0;
      let skippedStock = 0;
      for (const s of stock) {
        const id = s.id;
        if (!id) {
          skippedStock++;
          continue;
        }
        const totalQuantity = Number(s.total_quantity ?? s.totalQuantity ?? 0);
        const quantitySold = Number(s.quantity_sold ?? s.quantitySold ?? 0);
        const remainingStock = Number(s.remaining_stock ?? s.remainingStock ?? Math.max(0, totalQuantity - quantitySold));
        const result = await client.query(
          `INSERT INTO stock (id, item_type, category, sub_category, gender, class, size, item_name,
             total_quantity, quantity_sold, remaining_stock, date)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
           ON CONFLICT DO NOTHING`,
          [
            id,
            s.item_type ?? s.itemType ?? '',
            s.category ?? '',
            s.sub_category ?? s.subCategory ?? '',
            s.gender ?? '',
            s.class ?? '',
            s.size ?? '',
            s.item_name ?? s.itemName ?? '',
            totalQuantity,
            quantitySold,
            remainingStock,
            s.date ?? null,
          ]
        );
        if (result.rowCount && result.rowCount > 0) addedStock++;
        else skippedStock++;
      }

      return { addedStudents, skippedStudents, addedFees, skippedFees, addedStock, skippedStock };
    });

    res.json({ message: 'Restore complete', summary });
  })
);

export default router;
