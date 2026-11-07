package com.zhuri.daybyday.data

import android.content.Context
import com.zhuri.daybyday.model.Repeat
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.util.doneKey
import com.zhuri.daybyday.util.parseIso
import com.zhuri.daybyday.util.toIso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * 唯一数据源。所有写操作立即落盘（数据量极小），读操作给 StateFlow。
 * 存储格式见 docs/04。
 */
class TaskRepository(context: Context) {

    private val dir: File = context.filesDir
    private val dataFile = File(dir, "zhuri_data.json")
    private val bakFile = File(dir, "zhuri_data.bak.json")
    private val tmpFile = File(dir, "zhuri_data.tmp")

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _done = MutableStateFlow<Set<String>>(emptySet())
    val done: StateFlow<Set<String>> = _done.asStateFlow()

    init {
        load()
    }

    // ---------- 写 ----------

    fun add(task: Task) {
        _tasks.update { it + task }
        persist()
    }

    fun update(task: Task) {
        _tasks.update { list -> list.map { if (it.id == task.id) task else it } }
        persist()
    }

    fun delete(taskId: String) {
        _tasks.update { list -> list.filterNot { it.id == taskId } }
        _done.update { set -> set.filterNot { it.startsWith("$taskId|") }.toSet() }
        persist()
    }

    fun toggle(taskId: String, date: LocalDate) {
        val key = doneKey(taskId, date)
        _done.update { set -> if (key in set) set - key else set + key }
        persist()
    }

    fun isDone(taskId: String, date: LocalDate): Boolean = doneKey(taskId, date) in _done.value

    // ---------- 落盘 ----------

    private fun persist() {
        val snapshotTasks = _tasks.value
        val snapshotDone = _done.value
        io.launch {
            runCatching { write(dataFile, snapshotTasks, snapshotDone) }
        }
    }

    private fun write(file: File, tasks: List<Task>, done: Set<String>) {
        val root = JSONObject()
        root.put("version", 1)

        val taskArray = JSONArray()
        tasks.forEach { task ->
            val o = JSONObject()
            o.put("id", task.id)
            o.put("title", task.title)
            o.put("note", task.note)
            o.put("startDate", task.startDate.toIso())
            o.put("repeat", task.repeat.name)
            val days = JSONArray()
            task.weekDays.sorted().forEach { days.put(it) }
            o.put("weekDays", days)
            o.put("monthDay", task.monthDay)
            if (task.time == null) o.put("time", JSONObject.NULL) else o.put("time", task.time)
            o.put("createdAt", task.createdAt)
            taskArray.put(o)
        }
        root.put("tasks", taskArray)

        val doneArray = JSONArray()
        done.sorted().forEach { doneArray.put(it) }
        root.put("done", doneArray)

        if (tmpFile.exists()) tmpFile.delete()
        tmpFile.writeText(root.toString())
        if (!tmpFile.renameTo(file)) {
            file.delete()
            tmpFile.renameTo(file)
        }
    }

    // ---------- 读 ----------

    private fun load() {
        if (!dataFile.exists()) return
        val text = runCatching { dataFile.readText() }.getOrNull()
        if (text.isNullOrBlank()) return

        val parsed = runCatching { parse(text) }.getOrNull()
        if (parsed == null) {
            // 数据损坏：备份后以空数据启动，绝不崩在启动页
            runCatching {
                if (bakFile.exists()) bakFile.delete()
                dataFile.renameTo(bakFile)
            }
            return
        }
        _tasks.value = parsed.first
        _done.value = parsed.second
    }

    private fun parse(text: String): Pair<List<Task>, Set<String>> {
        val root = JSONObject(text)

        val tasks = ArrayList<Task>()
        val array = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val start = parseIso(o.optString("startDate")) ?: continue
            val repeat = runCatching { Repeat.valueOf(o.optString("repeat", "NONE")) }
                .getOrDefault(Repeat.NONE)

            val weeks = LinkedHashSet<Int>()
            val days = o.optJSONArray("weekDays")
            if (days != null) {
                for (j in 0 until days.length()) {
                    val v = days.optInt(j, 0)
                    if (v in 1..7) weeks.add(v)
                }
            }

            val time = if (o.isNull("time")) null else o.optString("time").takeIf { it.isNotBlank() }

            tasks.add(
                Task(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    note = o.optString("note"),
                    startDate = start,
                    repeat = repeat,
                    weekDays = weeks,
                    monthDay = o.optInt("monthDay", 1).coerceIn(1, 31),
                    time = time,
                    createdAt = o.optLong("createdAt", 0L)
                )
            )
        }

        val done = LinkedHashSet<String>()
        val doneArray = root.optJSONArray("done")
        if (doneArray != null) {
            for (i in 0 until doneArray.length()) {
                val v = doneArray.optString(i)
                if (v.contains("|")) done.add(v)
            }
        }
        return tasks to done
    }
}
