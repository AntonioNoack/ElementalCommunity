package me.antonio.noack.elementalcommunity.time

object SimpleDate {

    private val monthDays = intArrayOf(
        31, 28, 31, 30, 31, 30,
        31, 31, 30, 31, 30, 31
    )

    private val monthNames = arrayOf(
        "Jan", "Feb", "Mar",
        "Apr", "May", "Jun", "Jul", "Aug",
        "Sep", "Oct", "Nov", "Dec"
    )

    fun formatMinutesSince1970(minutesSince1970: Int) =
        formatMinutesSince1970(minutesSince1970.toLong())

    fun formatMinutesSince1970(minutesSince1970: Long): String {

        val minutesPerDay = 24 * 60
        val days = minutesSince1970 / minutesPerDay
        val dayMinutes = (minutesSince1970 % minutesPerDay).toInt()

        var year = 1970
        var remainingDays = days

        while (true) {
            val yearLength = if (isLeapYear(year)) 366 else 365
            if (remainingDays < yearLength) break

            remainingDays -= yearLength
            year++
        }

        var month = 0
        for (i in monthDays.indices) {
            val daysInMonth = if (i == 1 && isLeapYear(year)) {
                29
            } else {
                monthDays[i]
            }

            if (remainingDays < daysInMonth) {
                month = i
                break
            }

            remainingDays -= daysInMonth
        }

        val day = remainingDays.toInt() + 1
        val hour = dayMinutes / 60
        val minute = dayMinutes % 60

        return "${monthNames[month]} $day, $year, ${hour / 10}${hour % 10}:${minute / 10}${minute % 10}"
    }

    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }

    fun countMinutesSince1970(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        var days = 0L

        // Years since 1970
        for (year in 1970 until year) {
            days += if (isLeapYear(year)) 366 else 365
        }

        // Months before current month
        for (month in 1 until month) {
            days += if (month == 2 && isLeapYear(year)) {
                29
            } else {
                monthDays[month - 1]
            }
        }

        // Days before current day
        days += day - 1

        return days * 24 * 60 +
                hour * 60L +
                minute
    }
}
