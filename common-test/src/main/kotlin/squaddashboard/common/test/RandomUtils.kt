package squaddashboard.common.test

import java.time.ZonedDateTime
import kotlin.random.Random

fun Random.nextZonedDateTime(): ZonedDateTime =
    ZonedDateTime.now().plusSeconds(nextLong((60 * 60 * 24 * 365) * -2, 0))

fun <T> Random.nextFromList(list: List<T>): T =
    list[nextInt(0, list.size)]

fun Random.nextPositiveInt(): Int =
    nextInt(0, Int.MAX_VALUE)
