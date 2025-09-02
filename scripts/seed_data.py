#!/usr/bin/env python3
import os, argparse, json, random, string, time
from typing import List, Dict, Optional
import psycopg2
import psycopg2.extras

DEFAULT_LIBRARIES = [
    ("Taipei Main Library",   "No. 125, Sec. 3, Nanjing E Rd, Taipei"),
    ("Taichung City Library", "No. 1049, Jianxing Rd, Taichung"),
    ("Kaohsiung City Library","No. 61, Xinguang Rd, Kaohsiung"),
]

def connect(dsn: str):
    conn = psycopg2.connect(dsn)
    conn.autocommit = False
    return conn

def load_jsonl(path: str, limit: Optional[int]) -> List[Dict]:
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        for i, line in enumerate(f, 1):
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue
            rows.append(obj)
            if limit and len(rows) >= limit:
                break
    return rows

def normalize_book(obj: Dict) -> Dict:
    title = (obj.get("title") or "").strip()
    if not title:
        return {}
    author = (obj.get("author") or "").strip() or "(Unknown)"
    year = obj.get("first_publish_year")
    try:
        year = int(year) if year is not None else None
    except Exception:
        year = None

    # Randomize type: 80% BOOK, 20% PUBLICATION
    book_type = "BOOK" if random.random() < 0.8 else "PUBLICATION"

    return {
        "title": title[:500],
        "author": author[:500],
        "pub_year": year,
        "type": book_type,
    }

def batch_insert_books(conn, books: List[Dict], batch_size: int = 1000) -> List[int]:
    ids: List[int] = []
    with conn.cursor(cursor_factory=psycopg2.extras.DictCursor) as cur:
        for i in range(0, len(books), batch_size):
            batch = books[i:i+batch_size]
            if not batch:
                continue
            psycopg2.extras.execute_values(
                cur,
                """
                INSERT INTO books (title, author, pub_year, type)
                VALUES %s
                ON CONFLICT DO NOTHING
                RETURNING id
                """,
                [(b["title"], b["author"], b["pub_year"], b["type"]) for b in batch],
                page_size=min(1000, len(batch))
            )
            ids.extend([row["id"] for row in cur.fetchall()])
    return ids

def random_shelf() -> str:
    letter = random.choice(string.ascii_uppercase[:6])  # A–F
    number = random.randint(1, 50)
    return f"{letter}{number:02d}"

def insert_copies(conn, book_ids: List[int], libraries: List[int], min_copies=1, max_copies=3):
    copy_rows = []
    for bid in book_ids:
        c = random.randint(min_copies, max_copies)
        chosen = random.sample(libraries, k=min(c, len(libraries)))
        for lib_id in chosen:
            copy_rows.append((bid, lib_id, random_shelf()))
    if not copy_rows:
        return
    with conn.cursor() as cur:
        psycopg2.extras.execute_values(
            cur,
            "INSERT INTO book_copies (book_id, library_id, shelf_location) VALUES %s",
            copy_rows,
            page_size=min(2000, len(copy_rows))
        )

def get_library_ids(conn) -> List[int]:
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM libraries ORDER BY id")
        return [r[0] for r in cur.fetchall()]

def ensure_default_libraries(conn) -> List[int]:
    ids = get_library_ids(conn)
    if ids:
        return ids
    with conn.cursor() as cur:
        psycopg2.extras.execute_values(
            cur,
            "INSERT INTO libraries (name, address) VALUES %s",
            DEFAULT_LIBRARIES
        )
    conn.commit()
    return get_library_ids(conn)

def main():
    ap = argparse.ArgumentParser(description="Insert books & copies into existing schema.")
    ap.add_argument("--database-url", default=os.getenv("DATABASE_URL", "postgresql://library:library@localhost:5432/library"))
    ap.add_argument("--in", dest="infile", required=True, help="Path to books.jsonl")
    ap.add_argument("--target", type=int, default=None, help="Optional cap on number of books to insert.")
    ap.add_argument("--copies-min", type=int, default=1)
    ap.add_argument("--copies-max", type=int, default=3)
    args = ap.parse_args()

    conn = connect(args.database_url)
    try:
        libs = ensure_default_libraries(conn)
        print(f"Libraries in DB: {libs}")

        raw = load_jsonl(args.infile, args.target)
        books = [b for b in (normalize_book(o) for o in raw) if b]
        if not books:
            print("No valid books found in input.")
            return

        t0 = time.time()
        ids = batch_insert_books(conn, books)
        insert_copies(conn, ids, libs, args.copies_min, args.copies_max)
        conn.commit()
        print(f"Inserted {len(ids)} new books and ~{len(ids)} copies into {len(libs)} libraries in {time.time()-t0:.1f}s.")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

if __name__ == "__main__":
    main()
