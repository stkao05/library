# Library Book Management

Production-ready Spring Boot app for managing books, copies, members, and loans with fast text search.

## Live Demo

- https://library-production-5414.up.railway.app/ (100k+ books loaded)

Test accounts (or self-register):
- Librarian: admin@line.com / line
- Member: member@line.com / line

## Quick Start

Prerequisites
- Docker + Docker Compose
- Java 17 and Maven (or `./mvnw`)

1) Start PostgreSQL (via Docker):
```
docker compose up -d db
```

2) Initialize schema:
- Option A (local psql):
```
PGPASSWORD=library psql -h localhost -p 5432 -U library -d library -v ON_ERROR_STOP=1 -f src/main/sql/schema.sql
```
- Option B (psql in container):
```
docker compose exec -T db psql -U library -d library -v ON_ERROR_STOP=1 < src/main/sql/schema.sql
```

3) (Optional) Seed sample data:
```
python scripts/seed_data.py --database-url postgresql://library:library@localhost:5432/library --in scripts/books.jsonl
```

4) Run the application:
```
./mvnw spring-boot:run
```
or
```
mvn spring-boot:run
```

## Notes

- User registration: librarian verification is abstracted behind `LibrarianVerificationClient`. In development and test, `MockLibrarianVerificationClient` is used and allows all librarian registrations.

- Borrow/return flow: members (role `MEMBER`) can borrow and return books directly after logging in. This is a deliberate simplification for easier testing; in real libraries, lending typically goes through a librarian.

- Database schema: see `src/main/sql/schema.sql`.

- Search: implemented with PostgreSQL GIN indexes. Because titles and authors are short text, index size remains feasible. As shown in the demo with 100,000+ books, performance is reasonably good.

- Overdue notifications: implemented via the scheduled cron job `DueSoonScheduler`.

## Testing

```
./mvnw test
```
or
```
mvn test
```
