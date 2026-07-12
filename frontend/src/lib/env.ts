// Single source of truth for the backend base URL. Every service/page used
// to hardcode "http://localhost:8080" independently (9+ call sites) - that
// meant the app could never point at a staging/prod backend without editing
// source code. NEXT_PUBLIC_API_URL is already plumbed through Docker/compose
// (see frontend/Dockerfile, docker-compose.yml) - this is the piece that was
// missing to actually consume it.
export const API_BASE_URL: string =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
