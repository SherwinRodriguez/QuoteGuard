# QuoteGuard - Database Migration Guide

## ⚠️ CRITICAL: Breaking Changes to Invoice Schema

The invoice entity has been completely rewritten to enforce immutability and integrity verification. **You MUST migrate your database.**

---

## Migration Steps

### Option 1: Fresh Start (Recommended for Development)

If you have no production data, drop and recreate the database:

```sql
-- Drop existing schema
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO sherwin;
GRANT ALL ON SCHEMA public TO public;
```

Then restart your Spring Boot application. Hibernate will auto-create the new schema.

---

### Option 2: Manual Migration (For Production/Existing Data)

⚠️ **Backup your database first!**

```sql
-- 1. Add new required columns
ALTER TABLE invoices 
  ADD COLUMN uuid VARCHAR(36) UNIQUE NOT NULL DEFAULT gen_random_uuid()::text,
  ADD COLUMN invoice_hash VARCHAR(64),
  ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE',
  ADD COLUMN due_date DATE,
  ADD COLUMN currency VARCHAR(3) DEFAULT 'INR',
  ADD COLUMN subtotal DECIMAL(19,2),
  ADD COLUMN tax DECIMAL(19,2),
  ADD COLUMN revoked_at TIMESTAMP,
  ADD COLUMN revoked_reason VARCHAR(500);

-- 2. Make immutable fields non-updatable (JPA handles this, but add DB constraint for safety)
ALTER TABLE invoices 
  ALTER COLUMN uuid SET NOT NULL,
  ALTER COLUMN invoice_number SET NOT NULL,
  ALTER COLUMN issue_date SET NOT NULL,
  ALTER COLUMN total_amount SET NOT NULL,
  ALTER COLUMN created_at SET NOT NULL;

-- 3. Change created_at from DATE to TIMESTAMP (if needed)
-- WARNING: This will lose time information if you have existing data
ALTER TABLE invoices ALTER COLUMN created_at TYPE TIMESTAMP;

-- 4. Set default values for existing invoices
UPDATE invoices 
SET 
  due_date = issue_date + INTERVAL '30 days',
  currency = 'INR',
  subtotal = total_amount * 0.85,  -- Estimate: 85% of total
  tax = total_amount * 0.15,        -- Estimate: 15% tax
  status = 'ACTIVE'
WHERE due_date IS NULL;

-- 5. Drop deprecated columns (after verifying migration)
-- ALTER TABLE invoices DROP COLUMN qr_token;
-- ALTER TABLE invoices DROP COLUMN paid;

-- 6. CRITICAL: Generate hashes for existing invoices
-- This requires running a Java utility class (see below)
```

---

## Hash Generation for Existing Invoices

You **cannot** generate hashes via SQL. Create a Spring Boot migration service:

```java
@Service
@RequiredArgsConstructor
public class InvoiceMigrationService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceHashUtil hashUtil;
    
    @Transactional
    public void generateHashesForExistingInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll();
        
        for (Invoice invoice : invoices) {
            if (invoice.getInvoiceHash() == null || invoice.getInvoiceHash().isEmpty()) {
                String hash = hashUtil.generateHash(invoice);
                // Direct SQL update to bypass updatable=false constraint
                invoiceRepository.updateHashById(invoice.getId(), hash);
            }
        }
    }
}
```

Add to repository:
```java
@Modifying
@Query("UPDATE Invoice i SET i.invoiceHash = :hash WHERE i.id = :id")
void updateHashById(@Param("id") Long id, @Param("hash") String hash);
```

Run migration:
```bash
curl -X POST http://localhost:8080/api/admin/migrate-hashes
```

---

## New Database Constraints

After migration, enforce these constraints:

```sql
-- Invoice number must be unique per user
CREATE UNIQUE INDEX idx_user_invoice_number ON invoices(user_id, invoice_number);

-- UUID index for fast verification lookups
CREATE INDEX idx_invoice_uuid ON invoices(uuid);

-- Hash must be exactly 64 characters (SHA-256 hex)
ALTER TABLE invoices ADD CONSTRAINT chk_hash_length CHECK (char_length(invoice_hash) = 64);

-- Status must be ACTIVE or REVOKED
ALTER TABLE invoices ADD CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'REVOKED'));

-- If revoked, must have revoked_at and reason
ALTER TABLE invoices ADD CONSTRAINT chk_revoked_data 
  CHECK ((status = 'REVOKED' AND revoked_at IS NOT NULL) OR status != 'REVOKED');
```

---

## Verification

After migration, verify:

1. All invoices have UUIDs:
   ```sql
   SELECT COUNT(*) FROM invoices WHERE uuid IS NULL;
   -- Should return 0
   ```

2. All invoices have hashes:
   ```sql
   SELECT COUNT(*) FROM invoices WHERE invoice_hash IS NULL;
   -- Should return 0
   ```

3. All hashes are 64 characters:
   ```sql
   SELECT COUNT(*) FROM invoices WHERE char_length(invoice_hash) != 64;
   -- Should return 0
   ```

4. No duplicate UUIDs:
   ```sql
   SELECT uuid, COUNT(*) FROM invoices GROUP BY uuid HAVING COUNT(*) > 1;
   -- Should return 0 rows
   ```

---

## What Changed?

| Old Field     | New Field      | Notes                                    |
|---------------|----------------|------------------------------------------|
| `id` (Long)   | `uuid` (String)| UUID is now the PUBLIC identifier        |
| `qrToken`     | REMOVED        | QR now uses `uuid` in verification URL   |
| `paid` (bool) | DEPRECATED     | Use payment tracking system instead      |
| N/A           | `invoiceHash`  | SHA-256 hash for integrity verification  |
| N/A           | `status`       | ACTIVE or REVOKED                        |
| N/A           | `dueDate`      | Payment due date                         |
| N/A           | `currency`     | e.g., "INR", "USD"                       |
| N/A           | `subtotal`     | Amount before tax                        |
| N/A           | `tax`          | Tax amount                               |
| N/A           | `revokedAt`    | When invoice was revoked                 |
| N/A           | `revokedReason`| Why invoice was revoked                  |
| `createdAt` (DATE) | `createdAt` (TIMESTAMP) | Now includes time |

---

## Production Deployment Checklist

Before deploying to production:

1. ✅ Backup database
2. ✅ Run migration SQL
3. ✅ Generate hashes for existing invoices
4. ✅ Add database constraints
5. ✅ Run verification queries
6. ✅ Update `app.verification.base-url` in application.properties
7. ✅ Enable JWT authentication (remove `.permitAll()` for `/api/**`)
8. ✅ Test verification endpoint publicly
9. ✅ Test invoice creation
10. ✅ Test invoice revocation

---

## Rollback Plan

If migration fails:

```sql
-- Restore from backup
psql -U sherwin -d quoteguard < backup_before_migration.sql

-- OR manually rollback (if no backup)
ALTER TABLE invoices 
  DROP COLUMN uuid,
  DROP COLUMN invoice_hash,
  DROP COLUMN status,
  DROP COLUMN due_date,
  DROP COLUMN currency,
  DROP COLUMN subtotal,
  DROP COLUMN tax,
  DROP COLUMN revoked_at,
  DROP COLUMN revoked_reason;
```

---

## FAQ

**Q: Can I keep the old `qrToken` field?**  
A: No. The QR code now encodes the verification URL with `uuid`. The `qrToken` field is deprecated and should be removed after migration.

**Q: What if I don't have `dueDate` for existing invoices?**  
A: Set it to `issue_date + 30 days` or whatever your standard payment terms are.

**Q: Can I edit invoices after this migration?**  
A: NO. Invoices are IMMUTABLE after issuance. To correct errors, you must REVOKE the old invoice and issue a new one.

**Q: How do I handle invoices created before the hash system?**  
A: Generate hashes using the migration service. The hash will be based on the current stored values, so ensure data is correct before generating hashes.
