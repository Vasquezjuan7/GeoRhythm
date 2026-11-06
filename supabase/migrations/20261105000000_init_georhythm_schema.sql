-- GeoRhythm: esquema inicial
-- Pegar completo en Supabase → SQL Editor → New query → Run
-- (o con la CLI: supabase link --project-ref <ref> && supabase db push)

-- Perfiles de caminante (1:1 con auth.users)
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null default 'Caminante',
  runner_type text not null default 'Explorador Urbano',
  avatar_emoji text not null default '👟',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Caminatas registradas
create table public.walks (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  started_at timestamptz not null,
  ended_at timestamptz not null,
  distance_m double precision not null default 0 check (distance_m >= 0),
  duration_s integer not null default 0 check (duration_s >= 0),
  avg_speed_kmh double precision not null default 0,
  avg_cadence_spm integer not null default 0,
  destination_name text,
  route_id text,
  created_at timestamptz not null default now()
);
create index walks_user_started_idx on public.walks (user_id, started_at desc);

-- Lugares descubiertos (al pasar a < 40 m)
create table public.discovered_pois (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  poi_id text not null,
  discovered_at timestamptz not null default now(),
  primary key (user_id, poi_id)
);

-- Lugares favoritos
create table public.favorite_places (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null,
  lat double precision not null,
  lon double precision not null,
  created_at timestamptz not null default now(),
  unique (user_id, name)
);
create index favorite_places_user_idx on public.favorite_places (user_id);

-- Historial de canciones escuchadas
create table public.music_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  source text not null,
  track_id text not null,
  title text not null,
  artist text,
  bpm integer,
  played_at timestamptz not null default now()
);
create index music_history_user_played_idx on public.music_history (user_id, played_at desc);

-- Seguridad: cada usuario sólo ve y modifica sus propios datos
alter table public.profiles enable row level security;
alter table public.walks enable row level security;
alter table public.discovered_pois enable row level security;
alter table public.favorite_places enable row level security;
alter table public.music_history enable row level security;

create policy "profiles_select_own" on public.profiles for select to authenticated using ((select auth.uid()) = id);
create policy "profiles_update_own" on public.profiles for update to authenticated using ((select auth.uid()) = id) with check ((select auth.uid()) = id);
create policy "profiles_insert_own" on public.profiles for insert to authenticated with check ((select auth.uid()) = id);
create policy "walks_all_own" on public.walks for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "pois_all_own" on public.discovered_pois for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "fav_all_own" on public.favorite_places for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "music_all_own" on public.music_history for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

-- Crear el perfil automáticamente al registrarse (con los datos del formulario)
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, name, runner_type, avatar_emoji)
  values (
    new.id,
    coalesce(nullif(new.raw_user_meta_data->>'name', ''), 'Caminante'),
    coalesce(nullif(new.raw_user_meta_data->>'runner_type', ''), 'Explorador Urbano'),
    coalesce(nullif(new.raw_user_meta_data->>'avatar_emoji', ''), '👟')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;
revoke execute on function public.handle_new_user() from public, anon, authenticated;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Estadísticas del usuario (respeta la seguridad de quien consulta)
create view public.user_stats with (security_invoker = true) as
select
  p.id as user_id,
  coalesce((select sum(w.distance_m) from public.walks w where w.user_id = p.id), 0) / 1000.0 as total_km,
  coalesce((select count(*) from public.walks w where w.user_id = p.id), 0) as total_walks,
  coalesce((select sum(w.duration_s) from public.walks w where w.user_id = p.id), 0) as total_seconds,
  coalesce((select count(*) from public.discovered_pois d where d.user_id = p.id), 0) as discovered_pois
from public.profiles p;

grant select on public.user_stats to authenticated;
