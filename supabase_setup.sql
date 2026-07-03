-- Run this once in the Supabase SQL editor (Project -> SQL Editor -> New query)
-- for the project the app's SUPABASE_URL / SUPABASE_ANON_KEY point to.

create table if not exists public.session_records (
  id uuid primary key,                          -- matches SessionRecord.remoteId from the app
  user_id uuid not null references auth.users(id) on delete cascade,
  task_name text not null default 'task',
  start_time_millis bigint not null,
  total_allocated_duration_millis bigint not null,
  target_tasks_count integer not null,
  task_durations_csv text not null default '',
  total_actual_duration_millis bigint not null,
  inserted_at timestamptz not null default now()
);

create index if not exists session_records_user_id_idx
  on public.session_records (user_id);

alter table public.session_records enable row level security;

-- Each signed-in user can only see, insert, and update their own sessions.
create policy "Users can read own sessions"
  on public.session_records for select
  using (auth.uid() = user_id);

create policy "Users can insert own sessions"
  on public.session_records for insert
  with check (auth.uid() = user_id);

create policy "Users can update own sessions"
  on public.session_records for update
  using (auth.uid() = user_id);
