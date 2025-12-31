--Create roles table
create table if not exists roles (
    id uuid primary key default gen_random_uuid(),
    name varchar(60) not null unique,
    description varchar(100),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- seed roles
insert into roles (name, description)
values
    ('USER', 'Default user role'),
    ('ADMIN', 'Admin role'),
    ('SUPER_ADMIN', 'Super admin role')
on conflict(name) do nothing;

-- user table alter
alter table users add column if not exists role_id uuid;
--add foreign key
alter table users add constraint fk_users_roles foreign key (role_id) references roles(id);
--make role_id required
alter table users alter column role_id set not null;

--index for faster lookup by role
create index if not exists idx_roles_name on roles(name);