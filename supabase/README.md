# Supabase Setup

This folder contains the SQL needed to initialize Supabase for ProExpense.

## How to apply
1. Create a Supabase project.
2. Open **SQL Editor**.
3. Run: `supabase/migrations/001_init.sql`.

## Local config (Android)
Add these to `local.properties` (not committed):
```
supabase.url=https://YOUR_PROJECT.supabase.co
supabase.anonKey=YOUR_ANON_KEY
```

## What it creates
- `profiles` table for username tied to `auth.users.id`.
- `expenses` table for expense sync.
- RLS policies to restrict rows to the authenticated user.

## Notes
- Auth uses **email+password**. Username is stored in `profiles.username`.
- `amount` is stored as **minor units** (e.g., cents).
- `created_date` / `modified_date` are **epoch millis** to match local Room data.
