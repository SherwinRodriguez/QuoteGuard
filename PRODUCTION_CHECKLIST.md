# QuoteGuard - Production Deployment Checklist

## ✅ Pre-Production Tasks

Use this checklist to ensure QuoteGuard is production-ready.

---

## 🔐 Security

### Backend
- [ ] **Enable JWT Authentication**  
  Remove `.permitAll()` for `/api/**` endpoints in [SecurityConfig.java](backend/src/main/java/com/quoteguard/config/SecurityConfig.java#L30)
  
- [ ] **Implement JWT Filter**  
  Create `JwtAuthenticationFilter` to validate tokens
  
- [ ] **Verify Public Endpoints**  
  Ensure `/api/invoices/verify/**` remains public (no auth)
  
- [ ] **Add Rate Limiting**  
  Protect verification endpoint from abuse (e.g., Spring Cloud Gateway, Bucket4j)
  
- [ ] **Input Validation**  
  Add `@Valid` annotations to request DTOs
  
- [ ] **SQL Injection Protection**  
  Verify all queries use JPA/Hibernate (no raw SQL with user input)
  
- [ ] **CORS Configuration**  
  Update allowed origins in SecurityConfig for production domain

### Frontend
- [ ] **API URL Configuration**  
  Change `http://localhost:8080` to production API URL
  
- [ ] **Environment Variables**  
  Use `.env.production` for API base URL
  
- [ ] **HTTPS Only**  
  Ensure all API calls use HTTPS in production

---

## 🗄️ Database

- [ ] **Run Migration**  
  Follow [DATABASE_MIGRATION.md](backend/DATABASE_MIGRATION.md)
  
- [ ] **Backup Strategy**  
  Set up automated daily backups
  
- [ ] **Add Constraints**  
  ```sql
  CREATE UNIQUE INDEX idx_user_invoice_number ON invoices(user_id, invoice_number);
  CREATE INDEX idx_invoice_uuid ON invoices(uuid);
  ALTER TABLE invoices ADD CONSTRAINT chk_hash_length CHECK (char_length(invoice_hash) = 64);
  ALTER TABLE invoices ADD CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'REVOKED'));
  ```
  
- [ ] **Verify Immutability**  
  Test that updating hash/uuid/invoice_number fails
  
- [ ] **Connection Pooling**  
  Configure HikariCP in application.properties:
  ```properties
  spring.datasource.hikari.maximum-pool-size=10
  spring.datasource.hikari.minimum-idle=5
  ```

---

## 🛠️ Configuration

### Backend (application.properties)
```properties
# Production settings
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
app.verification.base-url=https://quoteguard.com
logging.level.root=INFO
logging.level.com.quoteguard=DEBUG
```

- [ ] Change `ddl-auto` to `validate` (prevent schema changes)
- [ ] Disable SQL logging
- [ ] Set production verification URL
- [ ] Configure logging levels

### Frontend
- [ ] Update API base URL in environment config
- [ ] Set production domain for verification URLs
- [ ] Enable production build optimizations

---

## 📝 What NOT to Build

As per requirements, **DO NOT** implement:
- ❌ Blockchain integration
- ❌ PDF digital signatures
- ❌ Watermarking
- ❌ Client authentication
- ❌ Public user accounts
- ❌ Invoice editing (only revoke + reissue)
- ❌ Payment processing
- ❌ Email notifications (out of scope)

---

## ✅ Testing

### Manual Testing
- [ ] Create invoice with all fields
- [ ] Verify invoice returns VERIFIED status
- [ ] Revoke invoice, verify returns REVOKED
- [ ] Try to verify non-existent UUID (returns NOT_FOUND)
- [ ] Scan QR code from generated PDF
- [ ] Download invoice PDF
- [ ] Test duplicate invoice number rejection
- [ ] Try to delete invoice (should fail)

### Hash Integrity Testing
- [ ] Manually modify invoice amount in database
- [ ] Verify invoice returns MODIFIED status
- [ ] Restore original amount
- [ ] Verify invoice returns VERIFIED again

### Security Testing
- [ ] Verify public access to `/api/invoices/verify/{uuid}` (no auth)
- [ ] Verify protected access to `/api/invoices` (requires auth)
- [ ] Test CORS from different origins
- [ ] Test rate limiting (if implemented)

---

## 🚀 Deployment Steps

1. **Database Migration**
   ```bash
   # Backup first!
   pg_dump quoteguard > backup_$(date +%Y%m%d).sql
   
   # Run migration SQL
   psql -U sherwin -d quoteguard -f migration.sql
   ```

2. **Backend Deployment**
   ```bash
   cd backend
   ./mvnw clean package -DskipTests
   java -jar target/backend-0.0.1-SNAPSHOT.jar
   ```

3. **Frontend Deployment**
   ```bash
   cd frontend
   npm run build
   npm start
   # OR deploy to Vercel/Netlify
   ```

4. **Smoke Test**
   ```bash
   # Test verification endpoint
   curl https://api.quoteguard.com/api/invoices/verify/{uuid}
   
   # Should return JSON without requiring auth
   ```

---

## 📊 Monitoring

- [ ] **Log Critical Events**
  - Invoice creation
  - Invoice revocation
  - Verification requests (with UUID)
  - Failed hash validations
  
- [ ] **Metrics to Track**
  - Total invoices created
  - Total verification requests
  - VERIFIED vs MODIFIED vs REVOKED ratios
  - Average verification response time

- [ ] **Alerts**
  - High rate of MODIFIED results (possible attack)
  - Database connection failures
  - PDF generation failures

---

## 🔍 Code Review Checklist

Before marking as complete:

- [ ] No invoice update methods exist (only create + revoke)
- [ ] `invoiceHash` has `updatable = false`
- [ ] Hash is generated BEFORE saving
- [ ] Hash includes all immutable fields
- [ ] Line items are sorted before hashing
- [ ] QR code contains ONLY verification URL
- [ ] Verification endpoint is public
- [ ] DELETE endpoint removed
- [ ] All DTOs use proper types (BigDecimal, LocalDate)
- [ ] Comments explain WHY, not WHAT

---

## 📚 Documentation

- [ ] Update README with:
  - Project overview
  - Tech stack
  - Setup instructions
  - API documentation
  - Security model explanation
  
- [ ] Document API endpoints:
  ```
  POST   /api/invoices              (Auth required)
  GET    /api/invoices              (Auth required)
  GET    /api/invoices/{id}         (Auth required)
  POST   /api/invoices/{uuid}/revoke (Auth required)
  GET    /api/invoices/verify/{uuid} (Public)
  ```

- [ ] Add architecture diagram showing:
  - Invoice creation flow
  - Hash generation
  - Verification flow
  - QR code generation

---

## 🎯 Definition of Done

QuoteGuard is production-ready when:

1. ✅ All invoices have UUID + hash
2. ✅ Invoices are immutable (cannot update hash/amounts/items)
3. ✅ QR codes contain verification URL only
4. ✅ Public can verify without authentication
5. ✅ Hash verification detects tampering
6. ✅ Revoked invoices fail verification
7. ✅ No DELETE endpoint exists
8. ✅ Database constraints enforce rules
9. ✅ Production config applied
10. ✅ All manual tests pass

---

## 🚨 Known Issues / Future Improvements

Document these for later:

1. **JWT Implementation**  
   Currently using basic auth. Replace with JWT for production.

2. **Rate Limiting**  
   Add to prevent verification endpoint abuse.

3. **Audit Logging**  
   Store verification attempts in database for analytics.

4. **Multi-tenancy**  
   Consider adding organization/workspace layer if scaling.

5. **Email Notifications**  
   Send email when invoice is created/revoked (out of current scope).

6. **Invoice Templates**  
   Allow customizing PDF template per user.

7. **Bulk Operations**  
   Support bulk invoice creation from CSV.

---

## 📞 Support

For issues during deployment:
- Check logs: `backend/logs/application.log`
- Verify database: `psql -U sherwin -d quoteguard -c "SELECT COUNT(*) FROM invoices WHERE invoice_hash IS NULL;"`
- Test verification: `curl http://localhost:8080/api/invoices/verify/{test-uuid}`

---

**Last Updated:** December 28, 2025  
**Version:** 1.0.0  
**Status:** Ready for Production (after JWT implementation)
