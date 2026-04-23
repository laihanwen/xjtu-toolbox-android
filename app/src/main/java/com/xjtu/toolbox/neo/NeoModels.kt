package com.xjtu.toolbox.neo

data class NeoUserInfo(
    val userId: String = "",
    val nickName: String = "",
    val telephone: String = "",
    val avatar: String = "",
    val nameEn: String = ""
)

data class NeoCourse(
    val courseId: String = "",
    val courseName: String = "",
    val courseNameCn: String = "",
    val courseNameEn: String = "",
    val professorName: String = "",
    val majorsName: String = "",
    val coursePoster: String = "",
    val courseBeginTime: String = "",
    val courseCategory: Int = 0
)

data class NeoAnnouncement(
    val noticeId: String = "",
    val title: String = "",
    val content: String = "",
    val publishTime: String = ""
)

data class NeoResource(
    val resourceId: String = "",
    val resourceName: String = "",
    val fileSize: Long = 0L,
    val fileType: String = "",
    val createTime: String = "",
    val downloadUrl: String = ""
)

data class NeoChapterContent(
    val id: String = "",
    val resourceName: String = "",
    val fileType: String = "",
    val sizeBytes: Long = 0L,
    val pageNum: Int = 0
)

data class NeoCourseChapter(
    val chapterId: String = "",
    val chapterName: String = "",
    val week: Int = 0,
    val contentNum: Int = 0,
    val isUnlock: Int = 0,
    val planStartTime: String = "",
    val planEndTime: String = ""
)
