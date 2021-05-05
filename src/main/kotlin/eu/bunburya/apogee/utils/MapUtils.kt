package eu.bunburya.apogee.utils

import java.util.regex.Pattern

/**
 * Take a map where the keys are strings representing regex patters, and return a map where the keys are compiled
 * Pattern objects.
 */
fun <valType> compileKeys(inMap: Map<String, valType>): Map<Pattern, valType> =
    inMap.map { (k, v) -> Pattern.compile(k) to v }.toMap()