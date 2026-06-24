package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SoundManager
import com.example.ui.GameViewModel
import com.example.ui.FloatingText
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CoinTapApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CoinTapApp(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel()
) {
    val stats by gameViewModel.gameStats.collectAsState()
    val floatingTexts by gameViewModel.floatingTexts.collectAsState()
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Tactile spring scaling animation for coin tapping
    var coinTargetScale by remember { mutableStateOf(1f) }
    val coinAnimatedScale by animateFloatAsState(
        targetValue = coinTargetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "coinScaleAnim"
    )

    // Current active Navigation tab (0 = Tap, 1 = Mine, 2 = Earn, 3 = Boost)
    var activeTab by remember { mutableStateOf(0) }

    // Audio status sync state
    var isMutedState by remember { mutableStateOf(SoundManager.isMuted) }

    // Ranks based on game progression level
    val levelName = when (stats.level) {
        1 -> "Bronze Clicker"
        2 -> "Silver Miner"
        3 -> "Gold Tycoon"
        4 -> "Neon Cyber King"
        5 -> "Bitcoin Baron"
        6 -> "Cosmic Overlord"
        7 -> "Solar Emperor"
        else -> "Ultimate Tap God 👑"
    }

    // Color definitions for Immersive Theme
    val bgMain = Color(0xFF121212)
    val cardBg = Color(0xFF1F1F1F)
    val border33 = Color(0xFF333333)
    val border42 = Color(0xFF424242)
    val txt91 = Color(0xFF919191)
    val amberGold = Color(0xFFF59E0B)
    val goldAccent = Color(0xFFFFD700)
    val neonCyan = Color(0xFF10B981) // High contrast emerald/neon green for status signals

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgMain)
    ) {
        // Space cosmic underlying dust
        Image(
            painter = painterResource(id = R.drawable.img_space_bg),
            contentDescription = "Cosmic background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.55f
        )

        // Main structural setup
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp) // Leave clean spacing for the bottom navigation tray
        ) {
            
            // --- TOP HEADER HUD BLOCK ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Miner avatar badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF313131))
                            .border(1.dp, border42, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Golden gradient mini sun spot
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))
                                    )
                                )
                        )
                    }

                    Column {
                        Text(
                            text = "MINER RANK",
                            color = txt91,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "$levelName (Lv ${stats.level})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Interactive Audio Status control
                IconButton(
                    onClick = {
                        SoundManager.isMuted = !SoundManager.isMuted
                        isMutedState = SoundManager.isMuted
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, border33, RoundedCornerShape(12.dp))
                        .testTag("sound_mute_button")
                ) {
                    Text(
                        text = if (isMutedState) "🔇" else "🔊",
                        fontSize = 16.sp
                    )
                }
            }

            // Progression level linear bar
            val currentLevelCoins = stats.coins
            val progressPercentage = when {
                currentLevelCoins < 1000L -> currentLevelCoins / 1000f
                currentLevelCoins < 10000L -> (currentLevelCoins - 1000L) / 9000f
                currentLevelCoins < 50000L -> (currentLevelCoins - 10000L) / 40000f
                currentLevelCoins < 250000L -> (currentLevelCoins - 50000L) / 200000f
                currentLevelCoins < 1000000L -> (currentLevelCoins - 250000L) / 750000f
                currentLevelCoins < 5000000L -> (currentLevelCoins - 1000000L) / 4000000f
                currentLevelCoins < 20000000L -> (currentLevelCoins - 5000000L) / 15000000f
                else -> 1.0f
            }
            LinearProgressIndicator(
                progress = { progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = amberGold,
                trackColor = Color.White.copy(alpha = 0.08f)
            )

            // --- MAIN COIN BALANCE MODULE ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Small glowing prefix symbol
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(amberGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("%,d", stats.coins),
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.testTag("coins_balance_text")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Rate Tag indicator bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(cardBg)
                        .border(1.dp, border33, RoundedCornerShape(50.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+${stats.coinsPerTap} / TAP",
                        color = amberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // --- TAB CONTENT CANVAS ---
            when (activeTab) {
                0 -> {
                    // TAB 0: THE IMMERSIVE GOLDEN TAP ARENA
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Atmospheric subtle background halo gold haze glow
                            Box(
                                modifier = Modifier
                                    .size(280.dp)
                                    .blur(50.dp)
                                    .background(amberGold.copy(alpha = 0.08f), CircleShape)
                            )

                            // Main Coin Button containing multi-layered rings
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = { offset ->
                                                scope.launch {
                                                    coinTargetScale = 0.90f
                                                    delay(70)
                                                    coinTargetScale = 1.0f
                                                }
                                            },
                                            onTap = { offset ->
                                                gameViewModel.handleTap(offset.x, offset.y)
                                            }
                                        )
                                    }
                                    .testTag("coin_tap_target"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Outer Ring Glow Border shadow elements
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFFFDE68A),
                                                    Color(0xFFF59E0B),
                                                    Color(0xFFB45309)
                                                )
                                            )
                                        )
                                        .border(4.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                )

                                // Intermediate Border band
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(CircleShape)
                                        .border(5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                )

                                // Transparent inner core
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // High definition pre-generated gold coin graphics
                                    Image(
                                        painter = painterResource(id = R.drawable.img_golden_coin),
                                        contentDescription = "Shiny coin icon",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .scale(coinAnimatedScale)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Interactive rising dynamic particle overlay
                                Box(modifier = Modifier.fillMaxSize()) {
                                    floatingTexts.forEach { textVal ->
                                        var animY by remember(textVal.id) { mutableStateOf(textVal.y) }
                                        var animAlpha by remember(textVal.id) { mutableStateOf(1.2f) }
                                        
                                        LaunchedEffect(textVal.id) {
                                            animate(
                                                initialValue = textVal.y,
                                                targetValue = textVal.y - 140f,
                                                animationSpec = tween(650, easing = LinearOutSlowInEasing)
                                            ) { valY, _ ->
                                                animY = valY
                                            }
                                        }

                                        LaunchedEffect(textVal.id) {
                                            animate(
                                                initialValue = 1.2f,
                                                targetValue = 0f,
                                                animationSpec = tween(650, easing = FastOutLinearInEasing)
                                            ) { valAlpha, _ ->
                                                animAlpha = valAlpha
                                            }
                                        }

                                        Text(
                                            text = textVal.text,
                                            color = amberGold,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier
                                                .offset { IntOffset(textVal.x.toInt() - 25, animY.toInt() - 20) }
                                                .alpha(animAlpha)
                                        )
                                    }
                                }
                            }
                        }

                        // BOTTOM ENERGY HUD INDICATOR
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡",
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stats.currentEnergy} / ${stats.maxEnergy}",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Refilling...",
                                    color = txt91,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Glowing energy track progress
                            val energyRatio = if (stats.maxEnergy > 0) stats.currentEnergy.toFloat() / stats.maxEnergy else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(cardBg)
                                    .border(1.dp, border33, RoundedCornerShape(50.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(energyRatio)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFFF59E0B), Color(0xFFFCD34D))
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
                
                1 -> {
                    // TAB 1: PASSIVE MINE DRONES UPGRADES
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "PASSIVE MINING ENGINES",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // Info status box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder(true).copy(
                                brush = Brush.linearGradient(colors = listOf(border33, Color.Transparent))
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🤖",
                                    fontSize = 32.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Current Autopilot Speed",
                                        color = txt91,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "+${stats.passiveIncome} Gold Coins / sec",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Drones keep mining for up to 3 hours offline automatically!",
                                        color = txt91,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        // Drone upgrade item
                        UpgradeRowLayout(
                            title = "🤖 Heavy Mining Drone",
                            desc = "Builds autonomous drone cores to passively tap +1 coin every second indefinitely.",
                            cost = gameViewModel.getDroneUpgradeCost(),
                            level = stats.passiveIncome,
                            canAfford = stats.coins >= gameViewModel.getDroneUpgradeCost(),
                            onBuyClick = { gameViewModel.buyDrone() },
                            textColorHex = amberGold,
                            testTagKey = "upgrade_drone"
                        )

                        // Recharge Upgrade
                        UpgradeRowLayout(
                            title = "🌀 Hyper Recharge Generator",
                            desc = "Speeds up secondary energy refill bounds by restoring +1 energy per tick.",
                            cost = gameViewModel.getRechargeUpgradeCost(),
                            level = stats.energyRecoveryRate,
                            canAfford = stats.coins >= gameViewModel.getRechargeUpgradeCost(),
                            onBuyClick = { gameViewModel.buyRechargeBoost() },
                            textColorHex = Color(0xFFA863FF),
                            testTagKey = "upgrade_recharge"
                        )
                    }
                }

                2 -> {
                    // TAB 2: MINING ACHIEVEMENT STATS & GENERAL CONTROLLER
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MINER STATS & MILESTONES",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Main stats profile matrix
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder(true).copy(
                                brush = Brush.linearGradient(colors = listOf(border33, Color.Transparent))
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatRow("Current Level Tier", "Rank ${stats.level} ($levelName)")
                                StatRow("Coins Harvested", String.format("%,d", stats.coins))
                                StatRow("Tapping efficiency", "+${stats.coinsPerTap} Per Screen Tap")
                                StatRow("Robot Drone Power", "+${stats.passiveIncome} Coins / sec")
                                StatRow("Max Energy Reserve", "⚡ ${stats.maxEnergy}")
                                StatRow("Energy Recharge Rate", "+${stats.energyRecoveryRate} Sec")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "GLOBAL RESET CORNER",
                            color = Color.Red.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Redesigned factory reset card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Factory Reset System",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Erase all balances and database row entries to start over from scratch.",
                                        color = txt91,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                Button(
                                    onClick = { gameViewModel.resetGame() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("reset_game_button")
                                ) {
                                    Text("RESET", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: POWER IMPULSE ACTIVE BOOST CABINET
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "IMPULSE ACTIVE BOOSTS",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // Multi-tap upgrade
                        UpgradeRowLayout(
                            title = "⚡ Multi-Tap Multiplier",
                            desc = "Amplifies every individual touch input by transferring extra +1 gold coin per upgrade level.",
                            cost = gameViewModel.getMultiTapUpgradeCost(),
                            level = stats.coinsPerTap,
                            canAfford = stats.coins >= gameViewModel.getMultiTapUpgradeCost(),
                            onBuyClick = { gameViewModel.buyMultiTap() },
                            textColorHex = amberGold,
                            testTagKey = "upgrade_multitap"
                        )

                        // Energy storage container tank upgrade
                        UpgradeRowLayout(
                            title = "🔋 Heavy Energy Storage Tub",
                            desc = "Hooks up supplemental battery banks allowing +100 max reservoir fuel bounds.",
                            cost = gameViewModel.getEnergyUpgradeCost(),
                            level = (stats.maxEnergy - 500) / 100 + 1,
                            canAfford = stats.coins >= gameViewModel.getEnergyUpgradeCost(),
                            onBuyClick = { gameViewModel.buyEnergyCapacity() },
                            textColorHex = neonCyan,
                            testTagKey = "upgrade_energy"
                        )
                    }
                }
            }
        }

        // --- THE MANDATORY MATERIAL 3 STYLED BOTTOM IMMERSIVE TABS TRAY ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(1.dp, border33, RoundedCornerShape(24.dp))
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    emoji = "🏠",
                    title = "Tap",
                    isActive = activeTab == 0,
                    onClick = { activeTab = 0 },
                    activeColor = amberGold
                )
                BottomNavItem(
                    emoji = "⛏️",
                    title = "Mine",
                    isActive = activeTab == 1,
                    onClick = { activeTab = 1 },
                    activeColor = amberGold
                )
                BottomNavItem(
                    emoji = "👥",
                    title = "Earn",
                    isActive = activeTab == 2,
                    onClick = { activeTab = 2 },
                    activeColor = amberGold
                )
                BottomNavItem(
                    emoji = "🚀",
                    title = "Boost",
                    isActive = activeTab == 3,
                    onClick = { activeTab = 3 },
                    activeColor = amberGold
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    emoji: String,
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    activeColor: Color
) {
    Box(
        modifier = Modifier
            .width(66.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) activeColor else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 18.sp
            )
            Text(
                text = title.uppercase(),
                color = if (isActive) Color.Black else Color(0xFF919191),
                fontSize = 9.sp,
                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF919191), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Deprecated("Legacy string layout greeting for backward screenshot verification suitability tests")
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        color = Color.White,
        modifier = modifier
    )
}
