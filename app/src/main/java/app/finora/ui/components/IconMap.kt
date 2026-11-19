package app.finora.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

val CategoryIcons: Map<String, ImageVector> = linkedMapOf(
    "cart" to Icons.Filled.ShoppingCart,
    "restaurant" to Icons.Filled.Restaurant,
    "home" to Icons.Filled.Home,
    "bag" to Icons.Filled.ShoppingBag,
    "tv" to Icons.Filled.Tv,
    "car" to Icons.Filled.DirectionsCar,
    "bolt" to Icons.Filled.Bolt,
    "repeat" to Icons.Filled.Repeat,
    "heart" to Icons.Filled.Favorite,
    "book" to Icons.Filled.MenuBook,
    "coffee" to Icons.Filled.LocalCafe,
    "pets" to Icons.Filled.Pets,
    "flight" to Icons.Filled.Flight,
    "fitness" to Icons.Filled.FitnessCenter,
    "gift" to Icons.Filled.CardGiftcard,
    "phone" to Icons.Filled.PhoneAndroid,
    "game" to Icons.Filled.SportsEsports,
    "work" to Icons.Filled.Work,
    "savings" to Icons.Filled.Savings,
    "child" to Icons.Filled.ChildCare,
    "music" to Icons.Filled.MusicNote,
    "more" to Icons.Filled.MoreHoriz,
)

fun iconFor(key: String): ImageVector = CategoryIcons[key] ?: Icons.Filled.MoreHoriz

val CategoryPalette = listOf(
    "#34C759", "#00C7BE", "#AF52DE", "#FF9500", "#FF2D55", "#007AFF",
    "#FFCC00", "#30B0C7", "#FF3B30", "#5856D6", "#8E8E93", "#A2845E",
)
