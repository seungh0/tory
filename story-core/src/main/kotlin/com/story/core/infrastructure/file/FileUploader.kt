package com.story.core.infrastructure.file

import com.story.core.domain.file.FileType
import org.springframework.web.multipart.MultipartFile

fun interface FileUploader {

    suspend fun upload(fileType: FileType, files: Collection<MultipartFile>): List<FileInfo>

}
