import { Router } from 'express';
import { z } from 'zod';
import { query, withTransaction } from '../db';
import { asyncHandler } from '../utils/asyncHandler';
import { NotFoundError, ValidationError } from '../utils/errors';

const router = Router();

const stockSchema = z.object({
  itemType: z.enum(['Book', 'Dress']),
  category: z.string().min(1, 'category is required'),
  subCategory: z.string().optional().default(''),
  gender: z.string().optional().default(''),
  class: z.string().optional().default(''),
  size: z.string().optional().default(''),
  itemName: z.string().min(1, 'itemName is required'),
  totalQuantity: z.number().int().nonnegative(),
  quantitySold: z.number().int().nonnegative(),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'date must be YYYY-MM-DD'),
});

interface StockRow {
  id: string;
  item_type: string;
  category: string;
  sub_category: string;
  gender: string;
  class: string;
  size: string;
  item_name: string;
  total_quantity: number;
  quantity_sold: number;
  remaining_stock: number;
  date: string;
}

function toApiStock(row: StockRow) {
  return {
    id: row.id,
    itemType: row.item_type,
    category: row.category,
    subCategory: row.sub_category,
    gender: row.gender,
    class: row.class,
    size: row.size,
    itemName: row.item_name,
    totalQuantity: row.total_quantity,
    quantitySold: row.quantity_sold,
    remainingStock: row.remaining_stock,
    date: row.date,
  };
}

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** stock.id is a UUID column — a malformed id would otherwise reach Postgres and raise a raw 22P02 error. */
function requireStockId(id: string): void {
  if (!UUID_REGEX.test(id)) throw new NotFoundError(`Stock item ${id} not found`);
}

router.get(
  '/',
  asyncHandler(async (_req, res) => {
    const result = await query<StockRow>('SELECT * FROM stock ORDER BY created_at DESC');
    res.json({ stock: result.rows.map(toApiStock) });
  })
);

router.get(
  '/class-availability',
  asyncHandler(async (req, res) => {
    const filter = (req.query.filter as string | undefined)?.trim();
    const rows = filter
      ? (
          await query<StockRow>(
            `SELECT * FROM stock
             WHERE class ILIKE $1 OR item_name ILIKE $1 OR item_type ILIKE $1
                OR ('Size ' || size) ILIKE $1`,
            [`%${filter}%`]
          )
        ).rows
      : (await query<StockRow>('SELECT * FROM stock')).rows;

    const CLASS_ORDER = ['Play', 'Nursery', 'KG', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10'];
    const classIndex = (cls: string) => {
      const idx = CLASS_ORDER.indexOf(cls);
      return idx === -1 ? CLASS_ORDER.length : idx;
    };

    const shaped = rows
      .map((r) => ({
        ...toApiStock(r),
        classLabel: r.class || (r.size ? `Size ${r.size}` : 'Unassigned / Legacy'),
      }))
      .sort((a, b) => {
        const diff = classIndex(a.class) - classIndex(b.class);
        if (diff !== 0) return diff;
        if (a.classLabel !== b.classLabel) return a.classLabel.localeCompare(b.classLabel);
        return a.itemName.localeCompare(b.itemName);
      })
      .map((s) => ({
        classLabel: s.classLabel,
        itemType: s.itemType,
        itemName: s.itemName,
        remainingStock: s.remainingStock,
        status: s.remainingStock <= 0 ? 'Out of Stock' : s.remainingStock < 10 ? 'Low Stock' : 'In Stock',
      }));

    res.json({ classAvailability: shaped, outOfStockCount: rows.filter((r) => r.remaining_stock <= 0).length });
  })
);

router.get(
  '/uniform-sizes',
  asyncHandler(async (_req, res) => {
    const result = await query<{ piece: string; size: string }>('SELECT piece, size FROM uniform_sizes ORDER BY piece, size');
    const byPiece: Record<string, string[]> = { Pant: [], Skirt: [], Shirt: [] };
    for (const row of result.rows) {
      if (!byPiece[row.piece]) byPiece[row.piece] = [];
      byPiece[row.piece].push(row.size);
    }
    res.json({ uniformSizesByPiece: byPiece });
  })
);

const uniformSizeSchema = z.object({
  piece: z.enum(['Pant', 'Skirt', 'Shirt']),
  size: z.string().min(1),
});

router.post(
  '/uniform-sizes',
  asyncHandler(async (req, res) => {
    const parsed = uniformSizeSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());
    await query('INSERT INTO uniform_sizes (piece, size) VALUES ($1, $2) ON CONFLICT DO NOTHING', [
      parsed.data.piece,
      parsed.data.size,
    ]);
    res.status(201).json({ message: 'Size suggestion saved' });
  })
);

router.get(
  '/:id',
  asyncHandler(async (req, res) => {
    requireStockId(req.params.id);
    const result = await query<StockRow>('SELECT * FROM stock WHERE id = $1', [req.params.id]);
    if (result.rows.length === 0) throw new NotFoundError(`Stock item ${req.params.id} not found`);
    res.json({ stock: toApiStock(result.rows[0]) });
  })
);

router.post(
  '/',
  asyncHandler(async (req, res) => {
    const parsed = stockSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());
    const s = parsed.data;

    if (s.quantitySold > s.totalQuantity) {
      throw new ValidationError({ quantitySold: 'Cannot exceed totalQuantity' });
    }

    const result = await withTransaction(async (client) => {
      // Same item+class(+gender/size) combination is treated as a restock:
      // quantities are added to the existing row instead of creating a
      // duplicate, matching the original app's behaviour.
      const existing = await client.query<StockRow>(
        `SELECT * FROM stock WHERE item_type = $1 AND category = $2 AND sub_category = $3
           AND gender = $4 AND class = $5 AND size = $6
         FOR UPDATE`,
        [s.itemType, s.category, s.subCategory, s.gender, s.class, s.size]
      );

      if (existing.rows.length > 0) {
        const row = existing.rows[0];
        const totalQuantity = row.total_quantity + s.totalQuantity;
        const quantitySold = row.quantity_sold + s.quantitySold;
        const remainingStock = Math.max(0, totalQuantity - quantitySold);
        const updated = await client.query<StockRow>(
          `UPDATE stock SET total_quantity = $1, quantity_sold = $2, remaining_stock = $3,
             date = $4, item_name = $5, updated_at = now()
           WHERE id = $6 RETURNING *`,
          [totalQuantity, quantitySold, remainingStock, s.date, s.itemName, row.id]
        );
        return { row: updated.rows[0], merged: true };
      }

      const remainingStock = Math.max(0, s.totalQuantity - s.quantitySold);
      const inserted = await client.query<StockRow>(
        `INSERT INTO stock (item_type, category, sub_category, gender, class, size, item_name,
           total_quantity, quantity_sold, remaining_stock, date)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11) RETURNING *`,
        [s.itemType, s.category, s.subCategory, s.gender, s.class, s.size, s.itemName, s.totalQuantity, s.quantitySold, remainingStock, s.date]
      );
      return { row: inserted.rows[0], merged: false };
    });

    res.status(201).json({ stock: toApiStock(result.row), merged: result.merged });
  })
);

router.put(
  '/:id',
  asyncHandler(async (req, res) => {
    requireStockId(req.params.id);
    const parsed = stockSchema.safeParse(req.body);
    if (!parsed.success) throw new ValidationError(parsed.error.flatten());
    const s = parsed.data;

    if (s.quantitySold > s.totalQuantity) {
      throw new ValidationError({ quantitySold: 'Cannot exceed totalQuantity' });
    }

    const remainingStock = Math.max(0, s.totalQuantity - s.quantitySold);
    const result = await query<StockRow>(
      `UPDATE stock SET item_type = $1, category = $2, sub_category = $3, gender = $4, class = $5,
         size = $6, item_name = $7, total_quantity = $8, quantity_sold = $9, remaining_stock = $10,
         date = $11, updated_at = now()
       WHERE id = $12 RETURNING *`,
      [s.itemType, s.category, s.subCategory, s.gender, s.class, s.size, s.itemName, s.totalQuantity, s.quantitySold, remainingStock, s.date, req.params.id]
    );
    if (result.rowCount === 0) throw new NotFoundError(`Stock item ${req.params.id} not found`);
    res.json({ stock: toApiStock(result.rows[0]) });
  })
);

router.delete(
  '/:id',
  asyncHandler(async (req, res) => {
    requireStockId(req.params.id);
    const result = await query('DELETE FROM stock WHERE id = $1', [req.params.id]);
    if (result.rowCount === 0) throw new NotFoundError(`Stock item ${req.params.id} not found`);
    res.status(204).send();
  })
);

export default router;
