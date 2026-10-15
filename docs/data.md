# Timetable data

Pass a directory containing one subdirectory per operator:

```text
operators/
  CITY/
    stops.csv
    routes.csv
    trips.csv
    stop_times.csv
```

Operator folder names use letters, digits or underscores. IDs are prefixed with the uppercased operator name (`A` becomes `CITY-A`); already prefixed IDs stay unchanged. References within each operator's CSV files must match exactly. Duplicate IDs, missing references and malformed rows fail loading.

## CSV columns

Files are UTF-8 with a header. Column order is flexible; quoted commas, escaped quotes and quoted newlines are supported. Additional columns are ignored.

| File | Required columns |
| --- | --- |
| `stops.csv` | `stop_id,stop_name,stop_lat,stop_lon` |
| `routes.csv` | `route_id,route_short_name,route_long_name,route_type` |
| `trips.csv` | `trip_id,route_id` |
| `stop_times.csv` | `trip_id,departure_time,stop_id,stop_sequence` |

`arrival_time` is optional in `stop_times.csv`. When absent, arrival equals departure at that stop. When present, every row must provide it; arrival cannot follow departure. A connection runs from one stop's departure to the next stop's arrival.

Supported route types: `0` tram, `1` metro, `2` train, `3` bus (or their uppercase names). Coordinates use decimal degrees. Stop sequences must be unique within a trip and times must progress in sequence order.

## Time and route rules

- Times use `HH:mm:ss`. Hours above 23 retain their service-day offset: a trip from `23:50:00` to ten minutes after midnight uses `24:10:00` for its arrival. This follows the [GTFS time convention](https://gtfs.org/documentation/schedule/reference/).
- A search starts at a clock time (`--at HH:mm`) on the same service day and lasts at most 12 hours. Missed trips are not repeated automatically the next day.
- Timetables are assumed active for that service day. Dates, calendars, pickup restrictions, minimum transfer times and real-time information are not implemented. This format is a simplified subset, not a complete GTFS importer.
- A transfer means boarding a different trip after the first vehicle, including after walking. Staying on the same trip adds no transfer.
- `--max-walk` limits total walking over the whole journey. Walking uses straight-line distance at 5 km/h, rounded up to seconds, with a 60-second minimum per link. Links do not account for roads, barriers or accessibility.
- Mode and operator filters apply to vehicles. Walking through another operator's stop remains possible.

The files in `examples/demo` describe a fictional network. They are demonstration data, not travel advice or current operator schedules.
