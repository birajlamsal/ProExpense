-- Supabase initial schema for ProExpense (offline-first + sync)
-- Run this in Supabase SQL editor or via migrations.

-- Extensions
create extension if not exists "pgcrypto";
create extension if not exists "citext";

-- Helper: keep updated_at current
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- Profiles (username linked to auth.users)
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username citext unique not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create trigger set_profiles_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

-- Expenses (sync target)
create table if not exists public.expenses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text,
  amount bigint not null, -- store as minor units (e.g., cents)
  category integer not null,
  note text,
  created_date bigint not null,  -- epoch millis
  modified_date bigint not null, -- epoch millis
  deleted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists expenses_user_id_idx on public.expenses(user_id);

create trigger set_expenses_updated_at
before update on public.expenses
for each row execute function public.set_updated_at();

-- RLS
alter table public.profiles enable row level security;
alter table public.expenses enable row level security;

-- Profiles policies (only owner can read/write)
create policy "profiles_select_own"
  on public.profiles for select
  using (auth.uid() = id);

create policy "profiles_insert_own"
  on public.profiles for insert
  with check (auth.uid() = id);

create policy "profiles_update_own"
  on public.profiles for update
  using (auth.uid() = id)
  with check (auth.uid() = id);

-- Expenses policies (only owner can read/write)
create policy "expenses_select_own"
  on public.expenses for select
  using (auth.uid() = user_id);

create policy "expenses_insert_own"
  on public.expenses for insert
  with check (auth.uid() = user_id);

create policy "expenses_update_own"
  on public.expenses for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "expenses_delete_own"
  on public.expenses for delete
  using (auth.uid() = user_id);
