#!/usr/bin/env python3
"""
发布说明工具：新建一个版本的说明文件，并刷新 docs/releases/README.md 索引。

用法：
    python scripts/new_release.py 3.1 "修复返回今天闪烁"
    python scripts/new_release.py 3.1.1 "修复奖杯不显示" --type 问题修复 --summary "修复部分机型奖杯墙空白"
    python scripts/new_release.py --index        # 只刷新索引

约定：
- 一个版本一个文件，文件名 v<版本>.md；
- 版本是发布单位，功能与修复都写在同一份说明里，靠小节区分；
- 索引按日期、版本号倒序生成，不手工排序。
"""
from __future__ import annotations

import argparse
import re
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RELEASES = ROOT / "docs" / "releases"
INDEX = RELEASES / "README.md"
TYPES = {"功能新增", "体验改进", "问题修复", "首个版本", "预发布"}


def parse_front_matter(text: str) -> dict[str, str]:
    if not text.startswith("---"):
        return {}
    end = text.find("\n---", 3)
    if end == -1:
        return {}
    data: dict[str, str] = {}
    for line in text[3:end].strip().splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            data[key.strip()] = value.strip()
    return data


def version_key(version: str) -> tuple[int, ...]:
    return tuple(int(part) for part in version.split("."))


def refresh_index() -> None:
    rows = []
    for path in RELEASES.glob("v*.md"):
        meta = parse_front_matter(path.read_text(encoding="utf-8"))
        if not meta.get("version"):
            continue
        rows.append(
            (
                meta.get("date", ""),
                meta["version"],
                meta.get("type", "更新"),
                meta.get("summary", ""),
                path.name,
            )
        )
    rows.sort(key=lambda row: (row[0], version_key(row[1])), reverse=True)

    lines = [
        "# 发布说明 · 索引",
        "",
        "每个版本一份说明，记录新增、改进与修复。想看某次具体改了什么，点对应版本即可。",
        "",
        "| 版本 | 日期 | 类型 | 一句话 |",
        "|---|---|---|---|",
    ]
    for day, version, kind, summary, filename in rows:
        lines.append(f"| [v{version}]({filename}) | {day} | {kind} | {summary} |")
    lines += [
        "",
        "---",
        "",
        "由 `scripts/new_release.py` 维护：新增版本时自动生成模板并刷新本索引。",
        "",
    ]
    INDEX.write_text("\n".join(lines), encoding="utf-8")
    print(f"索引已刷新：{len(rows)} 个版本")


def create_release(version: str, title: str, kind: str, summary: str, day: str) -> None:
    if not re.fullmatch(r"\d+\.\d+(\.\d+)?", version):
        sys.exit("版本号格式应为 3.1 或 3.1.1")
    if kind not in TYPES:
        sys.exit(f"类型必须是：{' / '.join(sorted(TYPES))}")

    RELEASES.mkdir(parents=True, exist_ok=True)
    path = RELEASES / f"v{version}.md"
    if path.exists():
        sys.exit(f"{path.name} 已存在，不覆盖；要改内容请直接编辑它，然后运行 --index")

    path.write_text(
        f"""---
version: {version}
date: {day}
type: {kind}
summary: {summary}
---

# v{version} · {title}

**发布日期**：{day}　　**版本类型**：{kind}

{summary}

## 新增

- 

## 改进

- 

## 修复

- 
""",
        encoding="utf-8",
    )
    print(f"已创建 {path.relative_to(ROOT)}，请补全三个小节")
    refresh_index()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("version", nargs="?", help="版本号，如 3.1 / 3.1.1")
    parser.add_argument("title", nargs="?", help="这一版的一句话标题")
    parser.add_argument("--type", default="体验改进", help="功能新增 / 体验改进 / 问题修复")
    parser.add_argument("--summary", default="", help="索引里显示的一句话摘要")
    parser.add_argument("--date", default=date.today().isoformat(), help="发布日期 YYYY-MM-DD")
    parser.add_argument("--index", action="store_true", help="只刷新索引")
    args = parser.parse_args()

    if args.index:
        refresh_index()
        return
    if not args.version or not args.title:
        parser.print_help()
        sys.exit(1)
    create_release(args.version, args.title, args.type, args.summary or args.title, args.date)


if __name__ == "__main__":
    main()
