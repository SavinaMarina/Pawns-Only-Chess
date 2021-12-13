package chess

enum class Color(val value: Char, val description: String, val startLine: Int) {
    WHITE('W',  "white", 1),
    BLACK('B', "black" , 6)
}

object Game {
    private val validMoveRegex = Regex("([a-h][1-8]){2}")

    data class Player(val name: String, val color: Color, var pawnsAmount: Int = 8)

    object Board {
        private const val emptySquare = ' '
        private const val boardSize = 8
        private const val illegalArgumentsErrorMsg = "Illegal Arguments!"
        private val board: MutableList<MutableList<Char>> = (0 until boardSize).map { row ->
            (0 until boardSize).map {
                when (row) {
                    Color.WHITE.startLine -> Color.WHITE.value
                    Color.BLACK.startLine -> Color.BLACK.value
                    else -> emptySquare
                }
            }.toMutableList()
        }.toMutableList()

        fun draw() {
            val separator = List(9){'+'}.joinToString("---", "  ")
            (boardSize - 1 downTo 0).forEach {
                println(separator)
                println(board[it].joinToString(" | ", "${it+1} | ", " |"))
            }
            println(separator)
            println(('a'..'h').joinToString("   ", "   "))
        }

        fun cleanSquare(x: Int, y: Int) {
            if ((x !in 0 until boardSize)||(y !in 0 until boardSize)) error(illegalArgumentsErrorMsg)
            board[y][x] = emptySquare
        }

        fun move(sourceX: Int, sourceY: Int, destX: Int, destY: Int, pawn: Color) {
            if ((sourceX !in 0 until boardSize)||(sourceY !in 0 until boardSize)||
                (destX !in 0 until boardSize)||(destY !in 0 until boardSize)) error(illegalArgumentsErrorMsg)
            cleanSquare(sourceX, sourceY)
            board[destY][destX] = pawn.value
        }

        fun getSquareValue(sourceX: Int, sourceY: Int): Char {
            if ((sourceX !in 0 until boardSize)||(sourceY !in 0 until boardSize))
                error("$illegalArgumentsErrorMsg $sourceX $sourceY")
            return board[sourceY][sourceX]
        }

        fun isThereAnyValidMove(color: Color): Boolean {
            for (i in 1 until boardSize - 1)
                for (j in 0 until boardSize) {
                    if (getSquareValue(j, i) == color.value &&
                        ((color == Color.WHITE && getSquareValue(j, i + 1) == emptySquare) ||
                        (color == Color.BLACK && getSquareValue(j, i - 1) == emptySquare) ||
                        (color == Color.WHITE && j > 0 && getSquareValue(j - 1, i + 1) == Color.BLACK.value) ||
                        (color == Color.WHITE && j < boardSize -1 && getSquareValue(j + 1, i + 1) == Color.BLACK.value) ||
                        (color == Color.BLACK && j > 0 && getSquareValue(j - 1, i - 1) == Color.WHITE.value) ||
                        (color == Color.BLACK && j < boardSize -1 && getSquareValue(j + 1, i - 1) == Color.WHITE.value)
                        ))
                    return true
                }
            return false
        }
    }

    fun play() {
        println("Pawns-Only Chess")
        println("First player's name:")
        val firstPlayer = Player(readLine()!!, Color.WHITE)
        println("Second player's name:")
        val secondPlayer = Player(readLine()!!, Color.BLACK)
        Board.draw()
        var currentPlayer = firstPlayer
        var opponent = secondPlayer
        var preEnPassantMove = false
        var preEnPassantX = 0
        while (true) {
            val pawn = currentPlayer.color
            println("${currentPlayer.name}'s turn:")
            val input = readLine()!!
            when {
                input == "exit" -> {
                    println("Bye!")
                    return
                }
                !input.matches(validMoveRegex) -> {
                    println("Invalid Input")
                    continue
                }
            }

            val source = input.substring(0,2)
            val sourceX = letterToIndex(input[0])
            val sourceY = input[1].digitToInt()-1

            if (Board.getSquareValue(sourceX, sourceY)  != pawn.value) {
                println("No ${pawn.description} pawn at $source")
                continue
            }
            val destX = letterToIndex(input[2])
            val destY = input[3].digitToInt()-1

            if (!isValidMove(sourceX, sourceY, destX, destY, pawn, Board.getSquareValue(destX, destY),
                    preEnPassantMove, preEnPassantX)) {
                println("Invalid Input")
                continue
            }

            if (Board.getSquareValue(destX, destY) != ' ') {
                opponent.pawnsAmount--
            }

            Board.move(sourceX, sourceY, destX, destY, pawn)
            if (preEnPassantMove && pawn == Color.WHITE && (sourceX != destX))
                Board.cleanSquare(destX, destY - 1)
            if (preEnPassantMove && pawn == Color.BLACK && (sourceX != destX))
                Board.cleanSquare(destX, destY + 1)
            preEnPassantMove = isFirstMoveOn2(sourceY, destY, pawn)
            if (preEnPassantMove) preEnPassantX = sourceX
            Board.draw()
            if (isWin(destY, pawn) || opponent.pawnsAmount == 0) {
                println("${pawn.value.toString().replace("W", "White")
                    .replace("B", "Black")} Wins!")
                println("Bye!")
                return
            }
            if (!Board.isThereAnyValidMove(opponent.color)) {
                println("Stalemate!")
                println("Bye!")
                return
            }
            currentPlayer = if (currentPlayer == firstPlayer) secondPlayer else firstPlayer
            opponent = if (currentPlayer == secondPlayer) firstPlayer else secondPlayer
        }
    }

    private fun isWin(destY: Int, pawn: Color): Boolean {
        return (pawn == Color.WHITE && destY == 7) ||
                (pawn == Color.BLACK && destY == 0)
    }

    private fun isFirstMoveOn2(sourceY: Int, destY: Int, pawn: Color): Boolean {
        return when {
            sourceY == Color.WHITE.startLine && pawn == Color.WHITE && sourceY == destY - 2 -> true
            sourceY == Color.BLACK.startLine && pawn == Color.BLACK && sourceY == destY + 2 -> true
            else -> false
        }
    }

    fun isValidMove(sourceX: Int, sourceY: Int, destX: Int, destY: Int,
                     pawn: Color, destPawn: Char, preEnPassantMove: Boolean, preEnPassantX: Int): Boolean {
        return when {
            destX == sourceX && destPawn != ' ' -> false
            destX != sourceX && destPawn == ' ' && !preEnPassantMove  -> false
            destX != sourceX && destPawn == ' ' && preEnPassantMove && preEnPassantX != destX -> false
            destX != sourceX && destPawn == pawn.value  -> false
            destX != sourceX && !((destX == sourceX + 1)||(destX == sourceX - 1)) -> false
            sourceY == Color.WHITE.startLine && pawn == Color.WHITE && !(sourceY == destY - 2 || sourceY == destY - 1) -> false
            sourceY == Color.BLACK.startLine && pawn == Color.BLACK && !(sourceY == destY + 2 || sourceY == destY + 1) -> false
            sourceY != Color.WHITE.startLine && pawn == Color.WHITE && sourceY != destY - 1 -> false
            sourceY != Color.BLACK.startLine && pawn == Color.BLACK && sourceY != destY + 1 -> false
            else -> true
        }
    }

    private fun letterToIndex(c: Char): Int {
        return c - 'a'
    }
}

fun main() {
    Game.play()
}


