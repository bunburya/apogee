package eu.bunburya.apogee.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatTime(millis: Long): String {
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toOffsetDateTime()
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime)
}