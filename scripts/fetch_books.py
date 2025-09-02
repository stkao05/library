#!/usr/bin/env python3
import argparse, json, time
from typing import Dict, Any, Iterable, List, Set, Tuple
import requests

BASE = "https://openlibrary.org"

def fetch_subject_page(subject: str, limit: int, offset: int) -> Dict[str, Any]:
    url = f"{BASE}/subjects/{subject}.json"
    r = requests.get(url, params={"limit": limit, "offset": offset}, timeout=20)
    r.raise_for_status()
    return r.json()

def normalize(work: Dict[str, Any]) -> Dict[str, Any] | None:
    title = (work.get("title") or "").strip()
    if not title:
        return None
    authors = work.get("authors") or []
    author_names = ", ".join([a.get("name", "").strip() for a in authors if a.get("name")]) or None
    return {
        "title": title,
        "author": author_names,
        "first_publish_year": work.get("first_publish_year"),
        "work_key": work.get("key"),           # e.g. "/works/OL45883W"
        "subject_sample": (work.get("subject") or work.get("subject_people") or [])[:5],
    }

def iter_works(subjects: List[str], target: int, page_size: int = 100) -> Iterable[Dict[str, Any]]:
    seen: Set[Tuple[str, str]] = set()
    offsets = {s: 0 for s in subjects}
    collected = 0

    while collected < target:
        progressed = False
        for s in subjects:
            try:
                data = fetch_subject_page(s, limit=page_size, offset=offsets[s])
            except Exception as e:
                # brief backoff and continue the loop
                time.sleep(0.8)
                continue

            works = data.get("works") or []
            if not works:
                continue

            for w in works:
                item = normalize(w)
                if not item:
                    continue
                key = (item["title"].lower(), (item["author"] or "").lower())
                if key in seen:
                    continue
                seen.add(key)
                yield item
                collected += 1
                if collected >= target:
                    return
            offsets[s] += page_size
            progressed = True

        if not progressed:  # all subjects exhausted
            break

def main():
    parser = argparse.ArgumentParser(description="Fetch real book data from Open Library into JSONL.")
    parser.add_argument("--subjects", nargs="*", default=["fiction","history","science","biography","technology"],
                        help="Subjects to pull from (Open Library subject slugs).")
    parser.add_argument("--target", type=int, default=1000, help="Number of books to fetch.")
    parser.add_argument("--page-size", type=int, default=100, help="API page size (<=100 recommended).")
    parser.add_argument("--out", default="books.jsonl", help="Output JSONL file.")
    args = parser.parse_args()

    count = 0
    with open(args.out, "w", encoding="utf-8") as f:
        for item in iter_works(args.subjects, args.target, args.page_size):
            f.write(json.dumps(item, ensure_ascii=False) + "\n")
            count += 1
            if count % 200 == 0:
                print(f"Wrote {count} records...")

    print(f"Done. Wrote {count} books to {args.out}")

if __name__ == "__main__":
    main()

