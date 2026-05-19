package com.kimo.reverprint.data.file

import com.kimo.reverprint.tools.file.FileCreator
import com.kimo.reverprint.tools.file.FileTypes
import com.kimo.reverprint.tools.file.IFile
import java.io.File

class ConcreteFileCreator(
    private val createFile: () -> File
): FileCreator {
    override fun create(type: FileTypes): IFile {
        return when (type) {
            FileTypes.MAPPED -> MappedFile(
                file = createFile(),
                initialLogicalSize = 0,
                createNewFile = createFile
            )
        }
    }
}