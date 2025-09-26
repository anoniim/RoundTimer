package net.solvetheriddle.roundtimer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform