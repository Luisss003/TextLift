-- Track revoked JWTs so logout can invalidate tokens early
create table if not exists revoked_token (
    id uuid primary key default gen_random_uuid(),
    token varchar(512) not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz not null default now()
);

create index if not exists idx_revoked_token_token on revoked_token(token);
create index if not exists idx_revoked_token_expires_at on revoked_token(expires_at);
