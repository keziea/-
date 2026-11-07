import json, datetime, random, sys

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

notes = [
    (0, "21:10", "傍晚的风", "下班路上闻到桂花，绕远走了十分钟。天暗得比上周早了，风里有那种干净的凉。"),
    (0, "08:20", "早起", "六点半醒了一次，没赖床。煮了咖啡，把昨天没读完的那页读完了。"),
    (1, "22:05", "一件小事", "把厨房台面彻底擦了一遍。没什么理由，就是突然想让明天早上看到它的时候舒服一点。"),
    (2, "20:40", "慢一点", "今天效率不高，但也没有责怪自己。晚上把手机放远，读完了一章书。"),
    (3, "21:30", None, "地铁上看到一个小孩趴在窗边数灯，数到第七盏的时候到站了。他妈妈说下次再数，他认真地点了点头。"),
    (4, "19:55", "周末的菜市场", "买了两把青菜和一小束雏菊。摊主说雏菊好养，换水就行。回家插在玻璃瓶里，桌子一下就不一样了。"),
    (5, "23:10", "雨", "雨从下午开始下，没带伞，在便利店门口站了十分钟。后来决定走回去，鞋湿了，但心情不坏。"),
    (6, "21:00", None, "把拖了很久的那封邮件发了。发送之前改了四遍，发出去以后反而很平静。"),
    (7, "22:40", "一锅汤", "第一次自己炖番茄牛腩，盐放少了，但喝到最后还是见底。下次记得多炖半小时。"),
    (8, "20:15", "散步", "吃完晚饭下楼走了两圈。小区里有人在练萨克斯，吹得不太熟，但很好听。"),
    (9, "23:20", "今天有点累", "没做什么大事，就是一直被打断。睡前把明天最重要的三件事写下来了，心里稳一点。"),
    (10, "21:45", "读到一句", "「日子是过以后，不是过从前。」合上书的时候，窗外正好有风。"),
    (11, "19:30", "周五", "下班路上买了半只烤鸭。回家和家里人视频，聊了四十分钟，都是些没什么用的小事。"),
    (13, "22:00", "整理相册", "翻到去年秋天在山上拍的照片。那天雾很大，什么都没看清，但记得当时很高兴。"),
    (15, "20:50", None, "把阳台上的枯叶剪掉了。新长出来的那两片很小，但颜色很好看。"),
    (17, "23:05", "晚归", "加班到九点，路上买了一个热乎的红薯。到家以后发现猫把我的笔碰掉在地上，捡起来还能写。"),
    (20, "21:25", "一封回信", "给很久没联系的朋友回了消息。打字删删改改，最后只发了一句「最近怎么样」。她很快回了。"),
    (23, "19:10", "换季", "把厚被子拿出来晒了。阳光很好，拍上去有那种蓬松的声音。"),
    (27, "22:30", "第一杯热美式", "天气转凉，第一次点了热美式。苦味比冰的明显，但握在手里很舒服。"),
    (31, "20:00", "开始", "决定从这个月开始每天写一点。不用写很多，真实就好。"),
    (36, "21:15", "周末下午", "在书店坐了一下午，什么也没买。看别人翻书、聊天、打瞌睡，也觉得挺有意思。"),
]

def classify(title, content):
    text = (title or "") + content
    if any(k in text for k in ("邮件", "工作", "加班", "效率", "会议", "周报")):
        return "WORK"
    if any(k in text for k in ("读", "书", "章", "一句")):
        return "STUDY"
    if any(k in text for k in ("菜市场", "厨房", "阳台", "猫", "红薯", "散步", "被子", "咖啡")):
        return "LIFE"
    if any(k in text for k in ("雨", "风", "累", "决定", "想", "平静", "高兴")):
        return "IDEA"
    return "OTHER"


entries = []
for i, (offset, hm, title, content) in enumerate(notes):
    day = today - datetime.timedelta(days=offset)
    hour, minute = map(int, hm.split(":"))
    created = datetime.datetime.combine(day, datetime.time(hour, minute))
    ts = int(created.timestamp() * 1000)
    entries.append(dict(
        id=f"e{i + 1}",
        title=title or "",
        content=content,
        date=day.isoformat(),
        category=classify(title, content),
        createdAt=ts,
        updatedAt=ts,
    ))

out = {"version": 1, "tasks": tasks, "done": sorted(done), "entries": entries}
target = sys.argv[1] if len(sys.argv) > 1 else "zhuri_data.json"
with open(target, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=1)

print("tasks:", len(tasks), "done:", len(done), "entries:", len(entries), "->", target)
