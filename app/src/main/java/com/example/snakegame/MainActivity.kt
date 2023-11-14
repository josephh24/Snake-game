package com.example.snakegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.snakegame.ui.theme.SnakeGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Iniciar el juego con el ámbito del ciclo de vida actual
        val game = Game(lifecycleScope)

        setContent {
            // Utilizar el tema SnakeGameTheme
            SnakeGameTheme {
                // Una superficie contenedora con el color de fondo LightGreen
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onPrimary
                ) {
                    // Componenente Snake que muestra el juego
                    Snake(game)
                }
            }
        }
    }
}

// Estado del juego, incluye posición de la comida y partes de la serpiente
data class State(val food: Pair<Int, Int>, val snake: List<Pair<Int, Int>>, val score: Int)

class Game(private val scope: CoroutineScope) {

    // Mutex para garantizar la seguridad de las operaciones concurrentes
    private val mutex = Mutex()

    // Estado mutable del juego
    private val mutableState =
        MutableStateFlow(State(food = Pair(5, 5), snake = listOf(Pair(7, 7)), score = 0))

    // Estado inmutable del juego accesible externamente
    val state: Flow<State> = mutableState

    // Variable para controlar la dirección del movimiento de la serpiente
    var move = Pair(1, 0)
        set(value) {
            // Utilizar el mutex para actualizar la dirección de movimiento de forma segura
            scope.launch {
                mutex.withLock {
                    field = value
                }
            }
        }

    init {
        // Inicializar el juego
        scope.launch {
            var snakeLength = 4
            var score = 0

            while (true) {
                delay(150)
                mutableState.update {
                    // Calcular la nueva posición de la cabeza de la serpiente
                    val newPosition = it.snake.first().let { poz ->
                        mutex.withLock {
                            Pair(
                                (poz.first + move.first + BOARD_SIZE) % BOARD_SIZE,
                                (poz.second + move.second + BOARD_SIZE) % BOARD_SIZE
                            )
                        }
                    }

                    // Incrementar la longitud de la serpiente si se encuentra con la comida
                    if (newPosition == it.food) {
                        snakeLength++
                        score += 10 // Puedes ajustar la puntuación según tus preferencias
                    }

                    // Restaurar la longitud de la serpiente si colisiona consigo misma
                    if (it.snake.contains(newPosition)) {
                        snakeLength = 4
                        score = 0
                    }

                    // Actualizar el estado del juego
                    it.copy(
                        food = if (newPosition == it.food) Pair(
                            Random().nextInt(BOARD_SIZE),
                            Random().nextInt(BOARD_SIZE)
                        ) else it.food,
                        snake = listOf(newPosition) + it.snake.take(snakeLength - 1),
                        score = score
                    )
                }
            }
        }
    }

    companion object {
        const val BOARD_SIZE = 16
    }
}

// Composición principal para mostrar el juego
@Composable
fun Snake(game: Game) {
    // Recolectar el estado del juego como estado del componente
    val state = game.state.collectAsState(initial = null)

    // Columna principal que contiene el tablero y los botones de control
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Mostrar el tablero si el estado no es nulo
        state.value?.let {
            Board(it)
            // Mostrar la puntuación
            Text("Score: ${it.score}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        // Mostrar los botones de control
        Buttons {
            game.move = it
        }
    }
}

// Composición para mostrar los botones de control
@Composable
fun Buttons(onDirectionChange: (Pair<Int, Int>) -> Unit) {
    // Tamaño común para todos los botones
    val buttonSize = Modifier.size(64.dp)

    // Columna que contiene los botones de dirección
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        // Botón para mover hacia arriba
        Button(onClick = { onDirectionChange(Pair(0, -1)) }, modifier = buttonSize) {
            Icon(Icons.Default.KeyboardArrowUp, null)
        }
        // Fila que contiene los botones de izquierda y derecha
        Row {
            // Botón para mover hacia la izquierda
            Button(onClick = { onDirectionChange(Pair(-1, 0)) }, modifier = buttonSize) {
                Icon(Icons.Default.KeyboardArrowLeft, null)
            }
            Spacer(modifier = buttonSize) // Espacio entre los botones de izquierda y derecha
            // Botón para mover hacia la derecha
            Button(onClick = { onDirectionChange(Pair(1, 0)) }, modifier = buttonSize) {
                Icon(Icons.Default.KeyboardArrowRight, null)
            }
        }
        // Botón para mover hacia abajo
        Button(onClick = { onDirectionChange(Pair(0, 1)) }, modifier = buttonSize) {
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
    }
}

// Composición para mostrar el tablero del juego
@Composable
fun Board(state: State) {
    // BoxWithConstraints se utiliza para calcular el tamaño de las celdas del tablero
    BoxWithConstraints(Modifier.padding(16.dp)) {
        // Tamaño de una celda del tablero
        val tileSize = maxWidth / Game.BOARD_SIZE

        // Box que representa el tablero del juego
        Box(
            Modifier
                .size(maxWidth)
                .border(2.dp, MaterialTheme.colorScheme.primary)
        )

        val color1 = MaterialTheme.colorScheme.primary
        val color2 = MaterialTheme.colorScheme.primaryContainer

        val largeRadialGradient = object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val biggerDimension = maxOf(size.height, size.width)
                return RadialGradientShader(
                    colors = listOf(color1, color2),
                    center = size.center,
                    radius = biggerDimension / 2f,
                    colorStops = listOf(0f, 0.95f),
                )
            }
        }

        // Box que representa la comida
        Box(
            Modifier
                .offset(x = tileSize * state.food.first, y = tileSize * state.food.second)
                .size(tileSize)
                .background(
                    brush = largeRadialGradient,
                    shape = CircleShape
                )
        )


        // Iterar sobre las partes de la serpiente y mostrarlas como celdas del tablero
        state.snake.forEach {
            Box(
                modifier = Modifier
                    .offset(x = tileSize * it.first, y = tileSize * it.second)
                    .size(tileSize)
                    .background(
                        MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small
                    )
            )
        }
    }
}
