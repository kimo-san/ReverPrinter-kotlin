package com.kimo.reverprint.tools.file

fun interface FileCreator {
    fun create(
        type: FileTypes
    ): IFile
}