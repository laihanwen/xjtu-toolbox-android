package com.xjtu.toolbox.neo

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.xjtu.toolbox.util.safeParseJsonObject

private const val TAG = "NeoApi"

private fun JsonElement?.safeString(): String? =
    if (this == null || this.isJsonNull) null else try { this.asString } catch (_: Exception) { null }

private fun JsonElement?.safeInt(default: Int = 0): Int =
    if (this == null || this.isJsonNull) default else try { this.asInt } catch (_: Exception) { default }

private fun JsonElement?.safeLong(default: Long = 0L): Long =
    if (this == null || this.isJsonNull) default else try { this.asLong } catch (_: Exception) { default }

class NeoApi(private val session: NeoSession) {

    private val baseUrl = NeoSession.BASE_URL

    fun getUserDetail(): NeoUserInfo {
        return try {
            val body = session.postJson("$baseUrl/sdm/bic/user/getUserDetail")
            val json = body.safeParseJsonObject()
            val data = json.getAsJsonObject("data") ?: return NeoUserInfo()
            NeoUserInfo(
                userId = data.get("userId").safeString() ?: data.get("id").safeString() ?: "",
                nickName = data.get("nickName").safeString() ?: "",
                telephone = data.get("telephone").safeString() ?: "",
                avatar = data.get("avatar").safeString() ?: "",
                nameEn = data.get("nameEn").safeString() ?: ""
            )
        } catch (e: Exception) {
            Log.w(TAG, "getUserDetail failed", e)
            NeoUserInfo()
        }
    }

    fun getMyCourseList(pageNum: Int = 1, pageSize: Int = 50): List<NeoCourse> {
        return try {
            val bodyJson = """{"pageNum":$pageNum,"pageSize":$pageSize}"""
            val body = session.postJson(
                "$baseUrl/sdm/cmt/eduCourseStudent/getCourseInfoPageByStudentId",
                bodyJson
            )
            Log.d(TAG, "getMyCourseList response: ${body.take(300)}")
            val json = body.safeParseJsonObject()
            val data = json.getAsJsonObject("data") ?: return emptyList()
            val list = data.getAsJsonArray("records") ?: return emptyList()
            list.mapNotNull { elem ->
                try {
                    val obj = elem.asJsonObject
                    NeoCourse(
                        courseId = obj.get("courseId").safeString() ?: return@mapNotNull null,
                        courseName = obj.get("courseName").safeString() ?: "",
                        courseNameCn = obj.get("courseNameCn").safeString() ?: "",
                        courseNameEn = obj.get("courseNameEn").safeString() ?: "",
                        professorName = obj.get("professorName").safeString() ?: "",
                        majorsName = obj.get("majorsName").safeString() ?: "",
                        coursePoster = obj.get("coursePoster").safeString() ?: "",
                        courseBeginTime = obj.get("courseBeginTime").safeString() ?: "",
                        courseCategory = obj.get("courseCategory").safeInt()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "parse course failed", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getMyCourseList failed", e)
            emptyList()
        }
    }

    fun getCourseAnnouncements(courseId: String): List<NeoAnnouncement> {
        return try {
            val bodyJson = """{"courseId":"$courseId"}"""
            val body = session.postJson(
                "$baseUrl/sdm/cmt/eduCourseware/getTwoNoticeOfStudent",
                bodyJson
            )
            Log.d(TAG, "getCourseAnnouncements response: ${body.take(200)}")
            val json = body.safeParseJsonObject()
            val dataElem = json.get("data")
            val list = when {
                dataElem != null && dataElem.isJsonArray -> dataElem.asJsonArray
                dataElem != null && dataElem.isJsonObject ->
                    dataElem.asJsonObject.getAsJsonArray("list")
                        ?: dataElem.asJsonObject.getAsJsonArray("records")
                else -> null
            } ?: return emptyList()
            list.mapNotNull { elem ->
                try {
                    val obj = elem.asJsonObject
                    NeoAnnouncement(
                        noticeId = obj.get("id").safeString() ?: return@mapNotNull null,
                        title = obj.get("title").safeString() ?: "",
                        content = obj.get("content").safeString() ?: "",
                        publishTime = obj.get("createdTime").safeString()
                            ?: obj.get("updatedTime").safeString() ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "parse announcement failed", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getCourseAnnouncements failed", e)
            emptyList()
        }
    }

    fun getCourseChapters(courseId: String): List<NeoCourseChapter> {
        return try {
            val chapBody = session.postJson(
                "$baseUrl/sdm/cmt/eduCourseware/getCourseNotInChapterAndContent",
                """{"courseId":"$courseId"}"""
            )
            val chapJson = chapBody.safeParseJsonObject()
            val chapData = chapJson.getAsJsonObject("data") ?: return emptyList()
            val chapList = chapData.getAsJsonArray("eduCoursewareVOS") ?: return emptyList()

            // 获取日程（planStartTime/planEndTime），通过 chapterId 对应
            val scheduleMap = mutableMapOf<String, Pair<String, String>>()
            try {
                val schBody = session.postJson(
                    "$baseUrl/sdm/cmt/eduCourseClassroom/getWorkTeachingSchedule",
                    """{"id":"$courseId"}"""
                )
                val schJson = schBody.safeParseJsonObject()
                val schList = schJson.getAsJsonArray("data")
                schList?.forEach { elem ->
                    val obj = elem.asJsonObject
                    val chapId = obj.get("chapterId").safeString() ?: return@forEach
                    val start = obj.get("planStartTime").safeString() ?: ""
                    val end = obj.get("planEndTime").safeString() ?: ""
                    scheduleMap[chapId] = Pair(start, end)
                }
            } catch (e: Exception) {
                Log.w(TAG, "getWorkTeachingSchedule failed", e)
            }

            chapList.mapNotNull { elem ->
                try {
                    val obj = elem.asJsonObject
                    val chapId = obj.get("id").safeString() ?: return@mapNotNull null
                    val (start, end) = scheduleMap[chapId] ?: Pair("", "")
                    NeoCourseChapter(
                        chapterId = chapId,
                        chapterName = obj.get("resourceName").safeString() ?: "",
                        week = obj.get("week").safeInt(),
                        contentNum = obj.get("contentNum").safeInt(),
                        isUnlock = obj.get("isUnlock").safeInt(),
                        planStartTime = start,
                        planEndTime = end
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "parse chapter failed", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getCourseChapters failed", e)
            emptyList()
        }
    }

    fun getChapterContents(courseId: String, chapterId: String): List<NeoChapterContent> {
        return try {
            val body = session.postJson(
                "$baseUrl/sdm/cmt/eduCourseware/getCourseChapterAndContent",
                """{"chapterId":"$chapterId","courseId":"$courseId"}"""
            )
            val json = body.safeParseJsonObject()
            val list = json.getAsJsonArray("data") ?: return emptyList()
            list.mapNotNull { elem ->
                try {
                    val obj = elem.asJsonObject
                    if (obj.get("resourceType").safeInt() != 1) return@mapNotNull null
                    NeoChapterContent(
                        id = obj.get("id").safeString() ?: return@mapNotNull null,
                        resourceName = obj.get("resourceName").safeString() ?: "",
                        fileType = obj.get("fileType").safeString() ?: "",
                        sizeBytes = obj.get("size").safeString()?.toLongOrNull() ?: 0L,
                        pageNum = obj.get("coursewarePageNum").safeInt()
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getChapterContents failed", e)
            emptyList()
        }
    }

    fun getCourseResources(courseId: String): List<NeoResource> {
        return try {
            val infoBody = session.postJson(
                "$baseUrl/sdm/cmt/eduCourse/getInfo",
                """{"courseId":"$courseId"}"""
            )
            Log.d(TAG, "getInfo response: ${infoBody.take(200)}")
            val infoJson = infoBody.safeParseJsonObject()
            val data = infoJson.getAsJsonObject("data") ?: return emptyList()

            val resources = mutableListOf<NeoResource>()
            for (key in listOf("lecture", "research")) {
                val sub = data.getAsJsonObject(key) ?: continue

                val attachList = sub.getAsJsonArray("attachmentList")
                if (attachList != null) {
                    attachList.forEach { elem ->
                        val obj = elem.asJsonObject
                        val resId = obj.get("id").safeString() ?: return@forEach
                        resources.add(
                            NeoResource(
                                resourceId = resId,
                                resourceName = obj.get("fileName").safeString() ?: "",
                                fileSize = obj.get("size").safeString()?.toLongOrNull() ?: 0L,
                                fileType = obj.get("ext").safeString() ?: "",
                                createTime = obj.get("createdTime").safeString() ?: "",
                                downloadUrl = obj.get("url").safeString()
                                    ?: "$baseUrl/bus/ath/resAttachment/noToken/getResAttachment?id=$resId"
                            )
                        )
                    }
                } else {
                    val idsRaw = sub.get("attachmentIds").safeString() ?: continue
                    val idsArray = try {
                        JsonParser.parseString(idsRaw).asJsonArray
                    } catch (_: Exception) { continue }
                    idsArray.forEach { elem ->
                        val resId = try { elem.asString } catch (_: Exception) { return@forEach }
                        try {
                            val resBody = session.postJson(
                                "$baseUrl/bus/ath/resAttachment/noToken/getResAttachment?id=$resId"
                            )
                            val resJson = resBody.safeParseJsonObject()
                            val resData = resJson.getAsJsonObject("data") ?: return@forEach
                            resources.add(
                                NeoResource(
                                    resourceId = resId,
                                    resourceName = resData.get("fileName").safeString() ?: "",
                                    fileSize = resData.get("size").safeString()?.toLongOrNull() ?: 0L,
                                    fileType = resData.get("ext").safeString() ?: "",
                                    createTime = resData.get("createdTime").safeString() ?: "",
                                    downloadUrl = resData.get("url").safeString()
                                        ?: "$baseUrl/bus/ath/resAttachment/noToken/getResAttachment?id=$resId"
                                )
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "getResAttachment failed for $resId", e)
                        }
                    }
                }
            }
            resources
        } catch (e: Exception) {
            Log.w(TAG, "getCourseResources failed", e)
            emptyList()
        }
    }

    fun getResourceDownloadUrl(resourceId: String): String {
        return "$baseUrl/bus/ath/resAttachment/noToken/getResAttachment?id=$resourceId"
    }

    fun verifySession(): Boolean {
        return try {
            val body = session.postJson("$baseUrl/sdm/bic/user/getUserDetail")
            val json = body.safeParseJsonObject()
            json.get("success")?.asBoolean == true
        } catch (e: Exception) {
            Log.w(TAG, "verifySession failed", e)
            false
        }
    }
}
