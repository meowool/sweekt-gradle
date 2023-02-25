package com.meowool.sweekt.gradle.job

import com.meowool.sweekt.gradle.utils.castOrNull

/**
 * A persistable set of key/value pairs which are used as inputs and
 * outputs for [BotJob].
 *
 * @author chachako
 */
@JvmInline
value class BotJobData(private val map: Map<Key<*>, Any?>) {
  /**
   * Returns the value of [T] type corresponding to the given [key],
   * or throws an exception if the given [key] is not found.
   */
  operator fun <T> get(key: Key<T>): T = getOrNull(key)
    ?: throw NoSuchElementException("Key $key is missing in the BotJobData.")

  /**
   * Returns the value of [T] type corresponding to the given [key],
   * or `null` if such a key is not present in the map.
   */
  fun <T> getOrNull(key: Key<T>): T? = map[key]?.castOrNull<T>()

  /**
   * Returns a new [BotJobData] object which is a combination of this.
   */
  operator fun plus(other: BotJobData): BotJobData =
    BotJobData(map + other.map)

  /**
   * Returns `true` if this data is empty (the map contains no elements).
   */
  fun isEmpty() = map.isEmpty()

  /**
   * Key for values stored in [BotJobData.map]. Type [T] is the type of
   * the value associated with the Key.
   */
  @JvmInline
  value class Key<T>(val name: String)
}

/**
 * Returns an empty [BotJobData] object.
 */
fun emptyJobData() = BotJobData(emptyMap())

/**
 * Converts a list of [pairs] to a [BotJobData] object.
 *
 * If multiple pairs have the same key, the resulting map will contain
 * the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 */
fun jobDataOf(vararg pairs: Pair<BotJobData.Key<*>, Any?>): BotJobData =
  BotJobData(pairs.toMap())

/**
 * Creates a key for an [T] type value.
 *
 * @param name the name of the data.
 */
fun <T> jobDataKey(name: Any): BotJobData.Key<T> =
  BotJobData.Key(name.toString())
