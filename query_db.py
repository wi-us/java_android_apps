import sqlite3


def main() -> None:
    con = sqlite3.connect("shop.db")
    cur = con.cursor()

    tables = [r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()]
    print("tables_count", len(tables))
    print("tables", tables)

    con.close()


if __name__ == "__main__":
    main()
