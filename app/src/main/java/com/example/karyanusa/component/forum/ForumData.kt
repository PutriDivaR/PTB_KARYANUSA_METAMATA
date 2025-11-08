package com.example.karyanusa.component.forum

import androidx.compose.runtime.mutableStateListOf
import android.content.Context
import android.net.Uri
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore

// Data class utama untuk setiap pertanyaan
data class Question(
    val id: Int,
    val displayName: String,
    val username: String,
    val time: String,
    var question: String,
    var replies: Int,
    var category: String,
    val isMyQuestion: Boolean,
    var isAnswered: Boolean,
    val imageUris: List<String> = emptyList(),
    val replyList: MutableList<Reply> = mutableListOf()
)

// Data class untuk setiap balasan
data class Reply(
    val id: Int,
    val displayName: String,
    val username: String,
    val time: String,
    val text: String,
    val imageUris: List<String> = emptyList()
)

// 🔹 Object penyimpanan data global Forum
object ForumData {
    val currentUserDisplayName = "Didip"
    val currentUserUsername = "putrididip"

    // Data dummy awal
    val questions = mutableStateListOf(
        Question(
            id = 1,
            displayName = "Vania",
            username = "ivangunawan",
            time = "30 Sep",
            question = "izin kbt, ini kot izi bgt ya",
            replies = 2,
            category = "Batik",
            isMyQuestion = false,
            isAnswered = true,
            replyList = mutableListOf(
                Reply(1, "Wanda", "iwanfals", "30 Sep", "benci banget"),
                Reply(2, "baban", "@iban", "30 Sep", "benci banget atau benci aja")
            )
        ),
        Question(
            id = 2,
            displayName = "Wanda",
            username = "iwanfals",
            time = "29 Sep",
            question = "Pernah nggak kalian ngerasa anyaman kalian malah lepas kalau karakternya beda?",
            replies = 4,
            category = "Anyaman",
            isMyQuestion = true,
            isAnswered = true,
            replyList = mutableListOf(
                Reply(1, "Vania", "@anggauzan", "29 Sep", "Iya betul, aku juga pernah."),
                Reply(2, "Ahmad", "@ahmad123", "29 Sep", "Coba pakai simpul ganda, biasanya lebih kuat.")
            )
        ),
        Question(
            id = 3,
            displayName = "Ahmad",
            username = "ahmad123",
            time = "2d ago",
            question = "Bagaimana cara implementasi RecyclerView dengan multiple view types?",
            replies = 1,
            category = "Android",
            isMyQuestion = false,
            isAnswered = true,
            replyList = mutableListOf(
                Reply(1, "Siti", "@sitirah", "2d ago", "Gunakan `getItemViewType()` untuk membedakan layout-nya.")
            )
        ),
        Question(
            id = 4,
            displayName = "Siti",
            username = "sitirah",
            time = "1d ago",
            question = "Ada yang tau cara fix error gradle build failed?",
            replies = 0,
            category = "Build",
            isMyQuestion = false,
            isAnswered = false
        )
    )

    fun addQuestion(
        displayName: String = currentUserDisplayName,
        username: String = currentUserUsername,
        time: String,
        question: String,
        category: String,
        imageUris: List<String> = emptyList()
    ) {
        val newQuestion = Question(
            id = if (questions.isEmpty()) 1 else (questions.maxOf { it.id } + 1),
            displayName = displayName,
            username = username,
            time = time,
            question = question,
            replies = 0,
            category = category.ifEmpty { "Umum" },
            isMyQuestion = true,
            isAnswered = false,
            imageUris = imageUris
        )
        questions.add(0, newQuestion)
    }

    fun updateQuestion(
        id: Int,
        newText: String,
        newCategory: String,
        newImages: List<String>
    ) {
        val index = questions.indexOfFirst { it.id == id }
        if (index != -1) {
            val q = questions[index]
            questions[index] = q.copy(
                question = newText,
                category = newCategory.ifEmpty { q.category },
                imageUris = newImages.ifEmpty { q.imageUris }
            )
        }
    }



    fun deleteQuestion(id: Int) {
        questions.removeAll { it.id == id }
    }

    fun addReplyDetail(questionId: Int, text: String, imageUris: List<String> = emptyList()) {
        val index = questions.indexOfFirst { it.id == questionId }
        if (index != -1) {
            val question = questions[index]
            val newReply = Reply(
                id = (question.replyList.maxOfOrNull { it.id } ?: 0) + 1,
                displayName = currentUserDisplayName,
                username = currentUserUsername,
                time = "Baru saja",
                text = text,
                imageUris = imageUris
            )
            question.replyList.add(0, newReply)
            questions[index] = question.copy(
                replies = question.replyList.size,
                isAnswered = true
            )
        }
    }
}

// Utility Uri Kamera
object ImageUtils {
    fun createImageUri(context: Context): Uri? {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }
}
