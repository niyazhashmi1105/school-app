import { Router } from 'express';
import { z } from 'zod';
import { query } from '../db';
import { asyncHandler } from '../utils/asyncHandler';
import { ConflictError, NotFoundError, ValidationError } from '../utils/errors';

const router = Router();

const studentSchema = z.object({
  regNo: z.string().min(1, 'Registration number is required'),
  name: z.string().min(1, 'Student name is required'),
  class: z.string().min(1, 'Class is required'),
  fatherName: z.string().min(1, "Father's name is required"),
  phone: z.string().min(1, 'Phone number is required'),
  admissionDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'admissionDate must be YYYY-MM-DD'),
});

interface StudentRow {
  reg_no: string;
  name: string;
  class: string;
  father_name: string;
  phone: string;
  admission_date: string;
}

function toApiStudent(row: StudentRow) {
  return {
    regNo: row.reg_no,
    name: row.name,
    class: row.class,
    fatherName: row.father_name,
    phone: row.phone,
    admissionDate: row.admission_date,
  };
}

router.get(
  '/',
  asyncHandler(async (req, res) => {
    const search = (req.query.search as string | undefined)?.trim();
    const rows = search
      ? (
          await query<StudentRow>(
            `SELECT * FROM students
             WHERE name ILIKE $1 OR class ILIKE $1 OR reg_no ILIKE $1 OR father_name ILIKE $1
             ORDER BY created_at DESC`,
            [`%${search}%`]
          )
        ).rows
      : (await query<StudentRow>('SELECT * FROM students ORDER BY created_at DESC')).rows;

    res.json({ students: rows.map(toApiStudent) });
  })
);

router.get(
  '/:regNo',
  asyncHandler(async (req, res) => {
    const result = await query<StudentRow>('SELECT * FROM students WHERE reg_no = $1', [req.params.regNo]);
    if (result.rows.length === 0) throw new NotFoundError(`Student ${req.params.regNo} not found`);
    res.json({ student: toApiStudent(result.rows[0]) });
  })
);

router.post(
  '/',
  asyncHandler(async (req, res) => {
    const parsed = studentSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());
    const s = parsed.data;

    const existing = await query('SELECT 1 FROM students WHERE reg_no = $1', [s.regNo]);
    if (existing.rows.length > 0) throw new ConflictError(`Registration number ${s.regNo} already exists`);

    const result = await query<StudentRow>(
      `INSERT INTO students (reg_no, name, class, father_name, phone, admission_date)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [s.regNo, s.name, s.class, s.fatherName, s.phone, s.admissionDate]
    );
    res.status(201).json({ student: toApiStudent(result.rows[0]) });
  })
);

router.put(
  '/:regNo',
  asyncHandler(async (req, res) => {
    const parsed = studentSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());
    const s = parsed.data;
    const originalRegNo = req.params.regNo;

    const existing = await query('SELECT 1 FROM students WHERE reg_no = $1', [originalRegNo]);
    if (existing.rows.length === 0) throw new NotFoundError(`Student ${originalRegNo} not found`);

    if (s.regNo !== originalRegNo) {
      const clash = await query('SELECT 1 FROM students WHERE reg_no = $1', [s.regNo]);
      if (clash.rows.length > 0) throw new ConflictError(`Registration number ${s.regNo} already exists`);
    }

    // fees.reg_no is ON UPDATE CASCADE, so changing reg_no here automatically
    // repoints any existing fee records to the new value.
    const result = await query<StudentRow>(
      `UPDATE students SET reg_no = $1, name = $2, class = $3, father_name = $4, phone = $5,
         admission_date = $6, updated_at = now()
       WHERE reg_no = $7 RETURNING *`,
      [s.regNo, s.name, s.class, s.fatherName, s.phone, s.admissionDate, originalRegNo]
    );

    res.json({ student: toApiStudent(result.rows[0]) });
  })
);

router.delete(
  '/:regNo',
  asyncHandler(async (req, res) => {
    const result = await query('DELETE FROM students WHERE reg_no = $1', [req.params.regNo]);
    if (result.rowCount === 0) throw new NotFoundError(`Student ${req.params.regNo} not found`);
    // Associated fee records are removed automatically via ON DELETE CASCADE.
    res.status(204).send();
  })
);

export default router;
