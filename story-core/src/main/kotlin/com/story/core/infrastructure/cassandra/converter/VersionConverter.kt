package com.story.core.infrastructure.cassandra.converter

import com.story.core.common.model.Version
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

@Component
@WritingConverter
class VersionWriteConverter : Converter<Version, String> {

    override fun convert(version: Version): String = version.toString()

}

@Component
@ReadingConverter
class VersionReadConverter : Converter<String, Version> {

    override fun convert(versionStr: String) = Version.of(versionStr)

}