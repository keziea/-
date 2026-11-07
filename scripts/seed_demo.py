import json, datetime, random

random.seed(7)
today = datetime.date.today()

tasks = [
    dict(id="t1", title="晨间阅读 20 页", note="《置身事内》第 3 章，做两页笔记",
         startDate="2026-09-01", repeat="DAILY", weekDays=[], monthDay=1,
         time="07:30", createdAt=1756684800000),
    dict(id="t2", title="健身 45 分钟", note="",
         startDate="2026-09-07", repeat="WEEKLY", weekDays=[1, 3, 5], monthDay=1,
         time="20:00", createdAt=1757203200000),
    dict(id="t3", title="写周报", note="进度、卡点、下周计划，各一句话",
         startDate="2026-09-04", repeat="WEEKLY", weekDays=[5], monthDay=1,
         time="18:00", createdAt=1756944000000),
    dict(id="t4", title="给妈妈打电话", note="",
         startDate="2026-09-06", repeat="WEEKLY", weekDays=[7], monthDay=1,
         time=None, createdAt=1757030400000),
    dict(id="t5", title="整理下周的选题", note="三个方向，每个写一句话",
         startDate=today.isoformat(), repeat="NONE", weekDays=[], monthDay=1,
         time="10:00", createdAt=1762416000000),
    dict(id="t6", title="把下周的会议邀约发出去", note="约周三下午，两个人",
         startDate=today.isoformat(), repeat="NONE", weekDays=[], monthDay=1,
         time="15:30", createdAt=1762416000001),
]

def occurs(t, d):
    start = datetime.date.fromisoformat(t["startDate"])
    if d < start:
        return False
    r = t["repeat"]
    if r == "NONE":
        return d == start
    if r == "DAILY":
        return True
    if r == "WEEKLY":
        return d.isoweekday() in t["weekDays"]
    return False

done = []
start = datetime.date(2026, 7, 20)
d = start
while d <= today:
    occ = [t for t in tasks if occurs(t, d)]
    for t in occ:
        flag = False
        if d > today - datetime.timedelta(days=7):
            flag = True                     # 最近一周全部完成
        elif d == today - datetime.timedelta(days=7):
            flag = False                    # 第八天断一次，streak = 6
        else:
            p = 0.55 if d.isoweekday() >= 6 else 0.82
            flag = random.random() < p
        if d == today:
            flag = t["id"] != "t6"          # 今天留一件没做
        if flag:
            done.append(f'{t["id"]}|{d.isoformat()}')
    d += datetime.timedelta(days=1)

out = {"version": 1, "tasks": tasks, "done": sorted(done)}
with open("E:/AndroidDev/zhuri_data.json", "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=1)

print("tasks:", len(tasks), "done:", len(done))
print("today:", today, "keys:", [k for k in done if k.endswith(today.isoformat())])
