// Typed error thrown by apiClient for every non-2xx response. Wraps the
// RFC 9457 application/problem+json bodies GlobalExceptionHandler now
// returns (see backend/src/main/java/com/quoteguard/exception/GlobalExceptionHandler.java)
// so callers can show a real message ("Invalid email or password") instead
// of a generic "Failed to fetch clients" - and, for 400s from Bean
// Validation, a field-level error map for inline form errors.
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }
}
