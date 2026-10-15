# Wayline

A Java command-line route planner for bus, tram, metro and train timetables. It finds the earliest arrival while respecting limits on total walking, vehicle changes and allowed transport modes.

## Run

Requires Java 17+ and Maven 3.8+.

```sh
mvn clean verify
java -jar target/wayline-1.0-SNAPSHOT.jar
```

A small fictional network is included, so no download is needed. Run commands from the project root. Search by station name in interactive mode, or use stop IDs:

```sh
java -jar target/wayline-1.0-SNAPSHOT.jar \
  --from CITY-A --to CITY-D --at 08:00 --max-walk 0
```

This arrives at Riverside at **08:30**, with one transfer. For a journey across midnight:

```sh
java -jar target/wayline-1.0-SNAPSHOT.jar \
  --from CITY-A --to CITY-C --at 23:45 --max-walk 0
```

Arrival: **00:12 (+1 day)**. Use `--help` for walking, transfer, mode and operator filters.

## How it works

- Directed graph of scheduled connections and nearby walking links.
- Dijkstra search retaining alternative states for time, walking and transfers.
- UTF-8 CSV loading with validation and separate IDs for each operator.
- Automated tests for routing constraints, midnight journeys, CSV input and the CLI.

Custom data: `--data /path/to/operators`. See [CSV format](docs/data.md).

This is a static timetable project with a 12-hour search horizon. It does not handle service calendars, real-time updates or full GTFS feeds. Walking links use straight-line distances rather than street routes.
