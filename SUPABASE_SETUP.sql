-- Run this in Supabase SQL Editor for Namma-HomeStay MVP.
-- Firebase Auth is used only for login; Supabase stores app data.

alter table users add column if not exists firebase_uid text unique;

alter table homestays add column if not exists latitude double precision default 14.8;
alter table homestays add column if not exists longitude double precision default 74.1;
alter table homestays add column if not exists view_count int default 0;
alter table homestays add column if not exists host_name text;
alter table homestays add column if not exists phone text;

alter table local_spots add column if not exists latitude double precision default 14.8;
alter table local_spots add column if not exists longitude double precision default 74.1;

alter table users disable row level security;
alter table homestays disable row level security;
alter table menus disable row level security;
alter table inquiries disable row level security;
alter table local_spots disable row level security;

insert into storage.buckets (id, name, public)
values
  ('homestay-photos', 'homestay-photos', true),
  ('menu-photos', 'menu-photos', true),
  ('spot-photos', 'spot-photos', true)
on conflict (id) do update set public = true;

do $$
begin
  if not exists (
    select 1 from pg_policies
    where schemaname = 'storage'
      and tablename = 'objects'
      and policyname = 'Namma public photo read'
  ) then
    create policy "Namma public photo read"
    on storage.objects for select
    using (bucket_id in ('homestay-photos', 'menu-photos', 'spot-photos'));
  end if;

  if not exists (
    select 1 from pg_policies
    where schemaname = 'storage'
      and tablename = 'objects'
      and policyname = 'Namma anon photo upload'
  ) then
    create policy "Namma anon photo upload"
    on storage.objects for insert
    with check (bucket_id in ('homestay-photos', 'menu-photos', 'spot-photos'));
  end if;

  if not exists (
    select 1 from pg_policies
    where schemaname = 'storage'
      and tablename = 'objects'
      and policyname = 'Namma anon photo update'
  ) then
    create policy "Namma anon photo update"
    on storage.objects for update
    using (bucket_id in ('homestay-photos', 'menu-photos', 'spot-photos'))
    with check (bucket_id in ('homestay-photos', 'menu-photos', 'spot-photos'));
  end if;
end $$;

notify pgrst, 'reload schema';
