package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.database.PostHistory
import com.example.data.database.TelegramChannel
import com.example.data.database.UserProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val telegramMessage by viewModel.telegramMessage.collectAsStateWithLifecycle()

    // Handle toast messages from viewmodel status
    LaunchedEffect(telegramMessage) {
        telegramMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.dismissTelegramMessage()
        }
    }

    // Determine Greeting based on current local time
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greetingText = when (hour) {
        in 4..5 -> "শুভ ভোর" // Dawn
        in 6..11 -> "শুভ সকাল" // Morning
        in 12..15 -> "শুভ দুপুর" // Noon
        in 16..17 -> "শুভ বিকাল" // Afternoon
        in 18..20 -> "শুভ সন্ধ্যা" // Evening
        else -> "শুভ রাত্রি" // Night
    }
    val greetingIcon = when (hour) {
        in 6..17 -> "☀️"
        in 18..20 -> "🌆"
        else -> "🌌"
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground),
        containerColor = Color.Transparent,
        bottomBar = {
            GlassyBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .drawBehind {
                    // Modern subtle radial background glows that feel premium and galactic
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x19BF5AF2), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.2f),
                            radius = size.width * 0.8f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x110A84FF), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.8f),
                            radius = size.width * 0.8f
                        )
                    )
                }
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "Main_Pages_Transitions"
            ) { targetTab ->
                when (targetTab) {
                    "Home" -> HomeScreen(
                        viewModel = viewModel,
                        greetingText = greetingText,
                        greetingIcon = greetingIcon,
                        userProfile = userProfile
                    )
                    "Telegram" -> TelegramManagerScreen(viewModel = viewModel)
                    "Creator" -> CreatorHubScreen(viewModel = viewModel)
                    "Profile" -> ProfileScreen(viewModel = viewModel, userProfile = userProfile)
                    else -> HomeScreen(
                        viewModel = viewModel,
                        greetingText = greetingText,
                        greetingIcon = greetingIcon,
                        userProfile = userProfile
                    )
                }
            }
        }
    }
}

// ==================== NAVIGATION COMPONENT ====================

@Composable
fun GlassyBottomNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(0.dp)),
        color = Color(0xEB0F101A), // Sleek OLED friendly near-black gloss
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "হোম",
                iconSelected = Icons.Filled.Home,
                iconUnselected = Icons.Outlined.Home,
                isSelected = currentTab == "Home",
                onClick = { onTabSelected("Home") },
                testTag = "nav_home"
            )
            NavItem(
                label = "টেলিগ্রাম",
                iconSelected = Icons.Filled.Send,
                iconUnselected = Icons.Outlined.Send,
                isSelected = currentTab == "Telegram",
                onClick = { onTabSelected("Telegram") },
                testTag = "nav_telegram"
            )
            NavItem(
                label = "ক্রিয়েটর হাব",
                iconSelected = Icons.Filled.AutoAwesome,
                iconUnselected = Icons.Outlined.AutoAwesome,
                isSelected = currentTab == "Creator",
                onClick = { onTabSelected("Creator") },
                testTag = "nav_creator"
            )
            NavItem(
                label = "প্রোফাইল",
                iconSelected = Icons.Filled.Person,
                iconUnselected = Icons.Outlined.Person,
                isSelected = currentTab == "Profile",
                onClick = { onTabSelected("Profile") },
                testTag = "nav_profile"
            )
        }
    }
}

@Composable
fun RowScope.NavItem(
    label: String,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val activeColor = NeonPurple
    val inactiveColor = TextGray

    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) iconSelected else iconUnselected,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isSelected) activeColor else inactiveColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// ==================== SCREEN 1: HOME DASHBOARD ====================

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    greetingText: String,
    greetingIcon: String,
    userProfile: UserProfile
) {
    val channelsCount by viewModel.totalChannelsCount.collectAsStateWithLifecycle()
    val isKeyValid = viewModel.checkGeminiKeyValid()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Banner Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(NeonPurple, NeonCyan))),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greetingText, ${userProfile.name}! $greetingIcon",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "আপনার অল-ইন-ওয়ান এআই সহকারী ড্যাশবোর্ডে স্বাগতম। সমস্ত ফিচার এখন সম্পূর্ণ সচল আছে!",
                            fontSize = 13.sp,
                            color = TextGray,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(NeonPurple, NeonPink)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Energy Status",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Quick API Key Alert Warning (if key is empty in dev environment)
        if (!isKeyValid) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FF375F))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning Key",
                            tint = NeonPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ডিফল্ট এপিআই কী অনুপস্থিত! এআই ফিচার অ্যাক্টিভ করতে অনুগ্রহ করে AI Studio-র 'Secrets Panel' চেক করুন।",
                            fontSize = 12.sp,
                            color = TextWhite,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Overall Statistic Summary Section
        item {
            Text(
                text = "আজকের পরিসংখ্যান",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatWidget(
                    title = "টেলিগ্রাম চ্যানেল",
                    value = channelsCount.toString(),
                    icon = Icons.Default.Campaign,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatWidget(
                    title = "মোট এআই তৈরি",
                    value = (userProfile.telegramGenerations + userProfile.imageGenerations + userProfile.translations + userProfile.socialPosts + userProfile.songGenerations).toString(),
                    icon = Icons.Default.AutoAwesome,
                    color = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Grid Menu representing shortcut to AI Tools
        item {
            Text(
                text = "ক্রিয়েটর টুলস শর্টকাট",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        val toolsList = listOf(
            HomeToolShortcut(
                title = "ছবি থেকে প্রম্পট",
                desc = "যেকোনো ছবি স্ক্যান করে প্রম্পট বের করুন",
                icon = Icons.Default.ImageSearch,
                color = NeonPurple,
                targetSubTab = "ImageToPrompt"
            ),
            HomeToolShortcut(
                title = "প্রম্পট থেকে ছবি",
                desc = "লিখে দিয়ে চমৎকার এইচডি ফটো আঁকুন",
                icon = Icons.Default.Draw,
                color = NeonPink,
                targetSubTab = "PromptToImage"
            ),
            HomeToolShortcut(
                title = "ভাষা অনুবাদক",
                desc = "যেকোনো ভাষা থেকে বাংলায় সঠিক অনুবাদ",
                icon = Icons.Default.Translate,
                color = NeonCyan,
                targetSubTab = "Translator"
            ),
            HomeToolShortcut(
                title = "সোশ্যাল পোস্ট মেকার",
                desc = "ইউটিউব/টিকটক আকর্ষণীয় পোস্ট মেকার",
                icon = Icons.Default.AddHomeWork,
                color = NeonOrange,
                targetSubTab = "Social"
            ),
            HomeToolShortcut(
                title = "সংগীত লিরিক্স ও সিন্থ",
                desc = "লিরিক ও সিন্থ প্রিভিউ প্লেয়ার বাজান",
                icon = Icons.Default.MusicNote,
                color = NeonTeal,
                targetSubTab = "Song"
            )
        )

        items(toolsList) { tool ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.selectTab("Creator")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(tool.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.title,
                            tint = tool.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tool.desc,
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open tool",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class HomeToolShortcut(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color,
    val targetSubTab: String
)

@Composable
fun StatWidget(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = TextGray
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
    }
}

// ==================== SCREEN 2: TELEGRAM CHANNEL MANAGER ====================

@Composable
fun TelegramManagerScreen(viewModel: DashboardViewModel) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val historyLog by viewModel.histories.collectAsStateWithLifecycle()
    val isTelegramLoading by viewModel.telegramLoading.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    var botTokenInput by remember { mutableStateOf("") }
    var channelIdInput by remember { mutableStateOf("") }

    var postMessageInput by remember { mutableStateOf("") }
    var postImageUriInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "টেলিগ্রাম কন্ট্রোল বেস",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "চ্যানেলসমূহ কনফিগারেশন এবং ব্রডকাস্ট করুন",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_channel_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Channel")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("যোগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Channels Status Row Slider List
        item {
            AnimatedContent(targetState = channels.isEmpty(), label = "Telegram_Channels_State") { isEmpty ->
                if (isEmpty) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "No Channels",
                                tint = TextGray,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "কোনো টেলিগ্রাম চ্যানেল যুক্ত নেই!",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = " can add bot channels dynamically by using custom Bot API Credentials.",
                                color = TextGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        channels.forEach { channel ->
                            ChannelCard(
                                channel = channel,
                                onDelete = { viewModel.deleteTelegramChannel(channel) }
                            )
                        }
                    }
                }
            }
        }

        // Posting Console Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ব্রডকাস্ট নতুন পোস্ট তৈরি",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    OutlinedTextField(
                        value = postMessageInput,
                        onValueChange = { postMessageInput = it },
                        label = { Text("পোস্ট লেখার বার্তা (HTML ফরম্যাট সমর্থিত)", color = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("broadcast_msg_text"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    OutlinedTextField(
                        value = postImageUriInput,
                        onValueChange = { postImageUriInput = it },
                        label = { Text("ঐচ্ছিক ইমেজ ইউআরএল (imageUrl)", color = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_img_text"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )

                    Button(
                        onClick = {
                            if (postMessageInput.trim().isEmpty()) {
                                Toast.makeText(context, "দয়া করে পোস্ট বডি প্যারাগ্রাফ কিছু লিখুন।", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.sendBroadcastPost(
                                text = postMessageInput,
                                imageUrl = if (postImageUriInput.trim().isEmpty()) null else postImageUriInput.trim()
                            ) {
                                postMessageInput = ""
                                postImageUriInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_broadcast"),
                        enabled = !isTelegramLoading && channels.isNotEmpty()
                    ) {
                        if (isTelegramLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Broadcast")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("সব চ্যানেলে পোস্ট পাঠান", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Detailed Broadcast Log History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পোস্ট ইতিহাস হিস্ট্রি লগার",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                if (historyLog.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("সব মুছুন", color = NeonPink, fontSize = 12.sp)
                    }
                }
            }
        }

        if (historyLog.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো পোস্টের পূর্ব ইতিহাস পাওয়া যায়নি।",
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(historyLog) { log ->
                HistoryItemRow(log = log)
            }
        }
    }

    // Modal dialog to add a new Channel
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন চ্যানেল এড করুন", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "আগে আপনার বটের API টোকেন দিন এবং চ্যানেলটিকে পাবলিক বা প্রাইভেট আইডি দিয়ে এডমিন পারমিশন দিন।",
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = botTokenInput,
                        onValueChange = { botTokenInput = it },
                        label = { Text("টেলিগ্রাম বট এপিআই টোকেন", color = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bot_token_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x22FFFFFF)
                        )
                    )

                    OutlinedTextField(
                        value = channelIdInput,
                        onValueChange = { channelIdInput = it },
                        label = { Text("চ্যানেল আইডি (যেমন: @mychannel বা -100xxx)", color = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_id_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color(0x22FFFFFF)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (botTokenInput.trim().isEmpty() || channelIdInput.trim().isEmpty()) {
                            Toast.makeText(context, "দয়া করে সবগুলো ইনপুট দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.verifyAndAddChannel(
                            botToken = botTokenInput.trim(),
                            channelIdInput = channelIdInput.trim()
                        ) { isOK, _ ->
                            if (isOK) {
                                showAddDialog = false
                                botTokenInput = ""
                                channelIdInput = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    enabled = !isTelegramLoading
                ) {
                    if (isTelegramLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("যাচাই করুন ও যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isTelegramLoading
                ) {
                    Text("বাতিল", color = TextGray)
                }
            },
            containerColor = CosmicSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ChannelCard(
    channel: TelegramChannel,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x1F0A84FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Chan icon",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete config",
                        tint = NeonPink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = channel.channelName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = channel.channelId,
                fontSize = 11.sp,
                color = TextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x1230D158)
            ) {
                Text(
                    text = "${channel.subscriberCount} সাবস্ক্রাইবার",
                    color = NeonTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryItemRow(log: PostHistory) {
    val dateString = remember(log.timestamp) {
        val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        formatter.format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (log.status == "সফল") Color(0x1A30D158) else Color(0x1AFF375F),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.status == "সফল") Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = log.status,
                    tint = if (log.status == "সফল") NeonTeal else NeonPink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.channelName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = dateString,
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.postText,
                    fontSize = 12.sp,
                    color = TextWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!log.imageUri.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📷 ইমেজসহ: ${log.imageUri}",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!log.reason.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "কারণ: ${log.reason}",
                        fontSize = 11.sp,
                        color = NeonPink,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==================== SCREEN 3: CREATOR HUB (CONTAINS ALL 5 AI TOOLS) ====================

@Composable
fun CreatorHubScreen(viewModel: DashboardViewModel) {
    // Current active tool: Vision, Draw, Translate, Social, Song
    var activeToolSubPage by remember { mutableStateOf("Vision") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Horizontally Scrollable Header Tabs to choose active AI engine
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(CosmicSurface, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "Vision" to "ছবি প্রম্পট",
                "Draw" to "ছবি বানান",
                "Translate" to "অনুবাদ করুন",
                "Social" to "সোশ্যাল পোস্ট",
                "Song" to "সংগীত লিরিক্স"
            ).forEach { (code, label) ->
                val isSelected = activeToolSubPage == code
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonPurple else Color.Transparent)
                        .clickable { activeToolSubPage = code }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("subnav_${code.lowercase()}")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else TextGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Display targeted Sub Page
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = activeToolSubPage,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                },
                label = "Sub_Creator_Tools"
            ) { actualPage ->
                when (actualPage) {
                    "Vision" -> ToolVisionScreen(viewModel = viewModel)
                    "Draw" -> ToolDrawScreen(viewModel = viewModel)
                    "Translate" -> ToolTranslateScreen(viewModel = viewModel)
                    "Social" -> ToolSocialScreen(viewModel = viewModel)
                    "Song" -> ToolSongScreen(viewModel = viewModel)
                    else -> ToolVisionScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// --- SUB TOOL 1: VISION (IMAGE TO PROMPT) ---

@Composable
fun ToolVisionScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val selectedBitmap by viewModel.selectedBitmap.collectAsStateWithLifecycle()
    val isVisionLoading by viewModel.imageToPromptLoading.collectAsStateWithLifecycle()
    val promptResult by viewModel.imageToPromptResult.collectAsStateWithLifecycle()

    var customInstruction by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Setup photo gallery launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewModel.selectBitmap(bitmap)
            } catch (e: Exception) {
                Toast.makeText(context, "ছবি ওড়াতে ব্যার্থ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🖼️ ছবি থেকে প্রম্পট রিসিভার",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "যেকোনো ছবি আপলোড করুন অথবা ক্যামেরা ফাইল চয়ন করুন। এআই ছবিটি নিখুঁত রিভার্স প্রম্পটে রূপান্তর করবে যা আপনি অন্য ইমেজ মেকার টুলে ও ব্যবহার করতে পারেন!",
                    fontSize = 12.sp,
                    color = TextGray,
                    lineHeight = 16.sp
                )

                // Placeholder preview area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .testTag("upload_image_box"),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected pic",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Mini change badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xCC000000)
                        ) {
                            Text(
                                "পরিবর্তন করুন 🔄",
                                color = TextWhite,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload action",
                                tint = NeonPurple,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("গ্যালারি থেকে ছবি সিলেক্ট করুন", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Click to choose format", color = TextGray, fontSize = 11.sp)
                        }
                    }
                }

                // AI Instruction
                OutlinedTextField(
                    value = customInstruction,
                    onValueChange = { customInstruction = it },
                    label = { Text("ঐচ্ছিক প্রম্পট ইনস্ট্রাকশন (যেমন: 'Describe in poetic style')", color = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vision_instruction_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )

                Button(
                    onClick = { viewModel.generatePromptFromImage(customInstruction) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("extract_prompt_btn"),
                    enabled = !isVisionLoading && selectedBitmap != null
                ) {
                    if (isVisionLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Scan")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ছবি থেকে প্রম্পট তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Result Container
        if (promptResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ এক্সট্র্যাক্ট এআই প্রম্পট:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Image Prompt", promptResult)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "প্রম্পট কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy text", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = promptResult,
                        fontSize = 13.sp,
                        color = TextWhite,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("vision_result_box")
                    )
                }
            }
        }
    }
}

// --- SUB TOOL 2: DRAW (PROMPT TO IMAGE) ---

@Composable
fun ToolDrawScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val drawPrompt by viewModel.originalImagePrompt.collectAsStateWithLifecycle()
    val isDrawLoading by viewModel.promptToImageLoading.collectAsStateWithLifecycle()
    val generatedImageUrl by viewModel.generatedImageUrl.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🌌 প্রম্পট থেকে ছবি আর্টমেকার",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "যেকোনো কাল্পনিক ছবি আঁকার বিবরণ দিন। আমরা এআই দিয়ে প্রম্পটটিকে আল্ট্রা-ডিটেইলড বানিয়ে ছবি জেনারেট করে দেখাবো!",
                    fontSize = 12.sp,
                    color = TextGray,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = drawPrompt,
                    onValueChange = { viewModel.setOriginalImagePrompt(it) },
                    label = { Text("ছবিটির সুন্দর বর্ণনা লিখুন (যেমন: 'Unreal cyberpunk green dragon')", color = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("draw_prompt_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonPink,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )

                Button(
                    onClick = { viewModel.generateImageFromPrompt(drawPrompt) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("draw_art_btn"),
                    enabled = !isDrawLoading && drawPrompt.trim().isNotEmpty()
                ) {
                    if (isDrawLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Draw")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ছবি জেনারেট করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Live Image Rendering Preview Area
        if (isDrawLoading || generatedImageUrl.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isDrawLoading) "🎨 এআই আর্ট অঙ্কন করছে..." else "✨ আপনার এআই জেনারেটেড আর্ট",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(CosmicSurface, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDrawLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonPink)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("গভীরভাবে ক্রাফট করা হচ্ছে...", color = TextGray, fontSize = 12.sp)
                            }
                        } else {
                            AsyncImage(
                                model = generatedImageUrl,
                                contentDescription = "Generated art preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("art_image_result"),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    if (generatedImageUrl.isNotEmpty() && !isDrawLoading) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Immediate share post to configured Telegram channels!
                            Button(
                                onClick = {
                                    viewModel.sendBroadcastPost(
                                        text = "সোশ্যাল মিডিয়াতে শেয়ারড এআই ইমেজ প্রম্পট আর্ট: \n\n#AIArt #Universe",
                                        imageUrl = generatedImageUrl
                                    ) {
                                        Toast.makeText(context, "টেলিগ্রাম চ্যানেলে ইমেজ পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share to tg")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("টেলিগ্রামে পাঠান", fontSize = 12.sp)
                            }

                            // Copy Link
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Image Link", generatedImageUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "ইমেজ লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicSurface),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy link")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("লিংক কপি করুন", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB TOOL 3: TRANSLATOR ---

@Composable
fun ToolTranslateScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val input by viewModel.translatorInput.collectAsStateWithLifecycle()
    val output by viewModel.translatorOutput.collectAsStateWithLifecycle()
    val isLoading by viewModel.translatorLoading.collectAsStateWithLifecycle()

    var sourceLang by remember { mutableStateOf("English") }
    var targetLang by remember { mutableStateOf("Bengali") }

    val languages = listOf("English", "Bengali", "Hindi", "Arabic", "Spanish", "Japanese", "Urdu")

    var showSourceMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🌐 এআই ভাষা অনুবাদক",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                // Select Language Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Source Select Box
                    Box {
                        Button(
                            onClick = { showSourceMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(130.dp)
                        ) {
                            Text(sourceLang, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop")
                        }
                        DropdownMenu(
                            expanded = showSourceMenu,
                            onDismissRequest = { showSourceMenu = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = TextWhite) },
                                    onClick = {
                                        sourceLang = lang
                                        showSourceMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Swap", tint = NeonCyan)

                    // Target Select Box
                    Box {
                        Button(
                            onClick = { showTargetMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(130.dp)
                        ) {
                            Text(targetLang, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop")
                        }
                        DropdownMenu(
                            expanded = showTargetMenu,
                            onDismissRequest = { showTargetMenu = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = TextWhite) },
                                    onClick = {
                                        targetLang = lang
                                        showTargetMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Input Text Card
                OutlinedTextField(
                    value = input,
                    onValueChange = { viewModel.setTranslatorInput(it) },
                    label = { Text("এখানে টেক্সট ইনপুট করুন", color = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("translate_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )

                Button(
                    onClick = { viewModel.translateText(sourceLang, targetLang) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("translate_btn"),
                    enabled = !isLoading && input.trim().isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Translate, contentDescription = "Translate")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("অনুবাদ করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Output Result card
        if (output.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ অনুবাদ রেসপন্স:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTeal
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Translation", output)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "টেক্সট কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy output", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = output,
                        fontSize = 13.sp,
                        color = TextWhite,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("translate_result_box")
                    )
                }
            }
        }
    }
}

// --- SUB TOOL 4: SOCIAL MEDIA POST METADATA CREATOR ---

@Composable
fun ToolSocialScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val title by viewModel.socialTitle.collectAsStateWithLifecycle()
    val desc by viewModel.socialDescription.collectAsStateWithLifecycle()
    val hashtags by viewModel.socialHashtags.collectAsStateWithLifecycle()
    val isLoading by viewModel.socialLoading.collectAsStateWithLifecycle()

    var topicKeywords by remember { mutableStateOf("") }
    var targetPlatform by remember { mutableStateOf("YouTube") }
    var selectedTone by remember { mutableStateOf("Catchy & Viral") }

    val platforms = listOf("YouTube", "Facebook", "TikTok", "Instagram")
    val tones = listOf("Catchy & Viral", "Informative", "Humorous", "Professional", "Storytelling")

    var showPlatformMenu by remember { mutableStateOf(false) }
    var showToneMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "📝 সোশ্যাল পোস্ট সলিউশনস মেকার",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                // Platforms & Tones Select Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showPlatformMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(targetPlatform, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop")
                        }
                        DropdownMenu(
                            expanded = showPlatformMenu,
                            onDismissRequest = { showPlatformMenu = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            platforms.forEach { pl ->
                                DropdownMenuItem(
                                    text = { Text(pl, color = TextWhite) },
                                    onClick = {
                                        targetPlatform = pl
                                        showPlatformMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showToneMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedTone, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop")
                        }
                        DropdownMenu(
                            expanded = showToneMenu,
                            onDismissRequest = { showToneMenu = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            tones.forEach { tn ->
                                DropdownMenuItem(
                                    text = { Text(tn, color = TextWhite) },
                                    onClick = {
                                        selectedTone = tn
                                        showToneMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Topic Outlined Key Words
                OutlinedTextField(
                    value = topicKeywords,
                    onValueChange = { topicKeywords = it },
                    label = { Text("পোস্ট বা ভিডিওর মূল বিষয়বস্তু/কী-ওয়ার্ড", color = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .testTag("social_keywords_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonOrange,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )

                Button(
                    onClick = {
                        viewModel.generateSocialMediaPost(
                            platform = targetPlatform,
                            mode = "All-In-One Metadata Pack",
                            keywords = topicKeywords,
                            tone = selectedTone
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_social_btn"),
                    enabled = !isLoading && topicKeywords.trim().isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = "Gen")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পোস্ট ডিজাইন জেনারেট করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Distinct Result Output Blocks
        if (title.isNotEmpty() || desc.isNotEmpty() || hashtags.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🔥 আপনার সোশ্যাল মিডিয়া কিট প্যাক:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonOrange,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (title.isNotEmpty()) {
                    SocialResultCustomBox(
                        titleText = "🎯 আকর্ষণীয় টাইটেলসমূহ (Titles)",
                        bodyText = title,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Titles", title)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "টাইটেল কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (desc.isNotEmpty()) {
                    SocialResultCustomBox(
                        titleText = "📝 ভিডিও ডেসক্রিপশন স্ক্রিপ্ট (Description)",
                        bodyText = desc,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Description", desc)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "ডেসক্রিপশন কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (hashtags.isNotEmpty()) {
                    SocialResultCustomBox(
                        titleText = "🏷️ ভাইরাল ট্রেন্ডিং ট্যাগস (Hashtags)",
                        bodyText = hashtags,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Hashtags", hashtags)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "হ্যাশট্যাগ কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SocialResultCustomBox(
    titleText: String,
    bodyText: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonOrange
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy element", tint = NeonCyan, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = bodyText,
                fontSize = 12.sp,
                color = TextWhite,
                lineHeight = 16.sp
            )
        }
    }
}

// --- SUB TOOL 5: SONG CREATOR (SONGSSTUDIO) ---

@Composable
fun ToolSongScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val lyricSheet by viewModel.songResultLyrics.collectAsStateWithLifecycle()
    val isLoading by viewModel.songLoading.collectAsStateWithLifecycle()
    
    val isPlaying by viewModel.isPlayingSong.collectAsStateWithLifecycle()
    val visualizerBars by viewModel.visualizerBars.collectAsStateWithLifecycle()

    var themeTopic by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Pop") }
    var selectedMood by remember { mutableStateOf("Happy & Uplifting") }

    val genres = listOf("Pop", "Rock", "Hip Hop & Rap", "Folk/Baul (লোকগীতি)", "Classical Mehfil")
    val moods = listOf("Happy & Uplifting", "Romantic & Melodic", "Sad & Heartbroken", "Patriotic (দেশপ্রেম)", "Mystic & Sufi")

    var showGenreMenu by remember { mutableStateOf(false) }
    var showMoodMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🎵 গান ও কুইক সিন্থ লিরিক্স স্টুডিও",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                // Dropdowns Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showGenreMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedGenre, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop")
                        }
                        DropdownMenu(
                            expanded = showGenreMenu,
                            onDismissRequest = { showGenreMenu = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            genres.forEach { ge ->
                                DropdownMenuItem(
                                    text = { Text(ge, color = TextWhite) },
                                    onClick = {
                                        selectedGenre = ge
                                        showGenreMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showMoodMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedMood, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop")
                        }
                        DropdownMenu(
                            expanded = showMoodMenu,
                            onDismissRequest = { showMoodMenu = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            moods.forEach { md ->
                                DropdownMenuItem(
                                    text = { Text(md, color = TextWhite) },
                                    onClick = {
                                        selectedMood = md
                                        showMoodMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Theme Outlined Box
                OutlinedTextField(
                    value = themeTopic,
                    onValueChange = { themeTopic = it },
                    label = { Text("গানের বিষয়বস্তু বা মূল থিম (যেমন: 'জীবন যুদ্ধ বা একাকী মন')", color = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("song_theme_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )

                Button(
                    onClick = {
                        viewModel.generateSongLyrics(
                            theme = themeTopic,
                            genre = selectedGenre,
                            mood = selectedMood
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_song_btn"),
                    enabled = !isLoading && themeTopic.trim().isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.MusicVideo, contentDescription = "Song")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("লিরিক্স ও গান তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Live Graphic Visualizer Audio Track Synthesis Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isPlaying) "⚡ লাইভ সিন্থ প্লেয়ার বাজছে (Synthesizer Preview Track)" else "🎹 থিম সিন্থেসাইজার কন্ট্রোলার",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonTeal
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Modern visualizer bars using lightweight Canvas implementation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(CosmicSurface, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    visualizerBars.forEach { heightRatio ->
                        val animHeight by animateFloatAsState(
                            targetValue = heightRatio,
                            label = "Waveform_Animator"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(animHeight.coerceIn(0.1f, 1.0f))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(NeonTeal, NeonCyan)
                                    ),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.togglePlaySynthesizer() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) NeonPink else NeonTeal
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("toggle_synth_player")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play/StopSynth"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "সুর বাজানো থামান" else "মেলেডি চমৎকার সুর শুনুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Lyrics Result Output Sheet representation
        if (lyricSheet.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📜 জেনারেটেড লিরিক্স ও কর্ডস",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTeal
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Song Lyrics", lyricSheet)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "লিরিক্স কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy lyrics", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    viewModel.sendBroadcastPost(
                                        text = "🎶 এআই ক্রিয়েটেড গান ও লিরিক্স: \n\n$lyricSheet"
                                    ) {
                                        Toast.makeText(context, "টেলিগ্রাম চ্যানেলে লিরিক্স ব্রডকাস্ট সফল!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share lyrics", tint = NeonPurple, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = lyricSheet,
                        fontSize = 13.sp,
                        color = TextWhite,
                        lineHeight = 20.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .testTag("lyrics_result_area")
                            .fillMaxWidth()
                            .background(CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

// ==================== SCREEN 4: USER SETTINGS PROFILE ====================

@Composable
fun ProfileScreen(
    viewModel: DashboardViewModel,
    userProfile: UserProfile
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }

    var tempName by remember { mutableStateOf(userProfile.name) }
    var tempBio by remember { mutableStateOf(userProfile.bio) }

    // Preselected beautiful neon modern avatar images the user can tap to change instantly!
    val avatarChoices = listOf(
        "https://image.pollinations.ai/prompt/cyberpunk_avatar_neon_glowing_purple_flat_vector-no-logos?width=256&height=256",
        "https://image.pollinations.ai/prompt/glowing_robotic_astronaut_profile_digital_art_vector-no-logos?width=256&height=256",
        "https://image.pollinations.ai/prompt/mysterious_bengali_boul_saint_playing_instrument_vector-no-logos?width=256&height=256",
        "https://image.pollinations.ai/prompt/stylish_female_futuristic_designer_avatar_flat_vector-no-logos?width=256&height=256"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section Header
        item {
            Text(
                text = "ইউজার প্রোফাইল সেশন",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        // Dynamic Profile Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(NeonPurple, NeonCyan)),
                                shape = CircleShape
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = CosmicSurface
                        ) {
                            if (!userProfile.avatarUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userProfile.avatarUri,
                                    contentDescription = "Avatar picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userProfile.name.takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "A",
                                        color = NeonPurple,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = userProfile.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    Text(
                        text = userProfile.bio,
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            isEditing = !isEditing
                            if (isEditing) {
                                tempName = userProfile.name
                                tempBio = userProfile.bio
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditing) NeonPink else NeonPurple
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("toggle_edit_profile_btn")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Cancel else Icons.Default.ModeEdit,
                            contentDescription = "Edit Profile"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEditing) "বাতিল করুন" else "প্রোফাইল সম্পাদন করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Editing Panel
        if (isEditing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📝 তথ্য সম্পাদন প্যানেল",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("আপনার পুরো নাম", color = TextGray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_name_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        OutlinedTextField(
                            value = tempBio,
                            onValueChange = { tempBio = it },
                            label = { Text("ব্যবহারকারী বায়ো (Bio)", color = TextGray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("edit_bio_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        // Cool Avatar Swapper
                        Text(
                            text = "🖼️ এআই অবতার ছবি সিলেক্ট করুন (Tap to Change):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            avatarChoices.forEach { uriString ->
                                val isSelected = userProfile.avatarUri == uriString
                                Card(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) NeonPurple else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.updateProfileAvatar(uriString) },
                                    shape = CircleShape
                                ) {
                                    AsyncImage(
                                        model = uriString,
                                        contentDescription = "Avatar options",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (tempName.trim().isEmpty()) {
                                    Toast.makeText(context, "দয়া করে নাম ফিলাপ করুন", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.updateProfileInfo(tempName, tempBio)
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("submit_profile_edit_btn")
                        ) {
                            Text("তথ্য সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Stat counter log history breakdown
        item {
            Text(
                text = "📊 ফিচার ব্যবহার ডায়েরি",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Start
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBreakdownRow(label = "টেলিগ্রাম ব্রডকাস্ট পোস্ট", value = userProfile.telegramGenerations, color = NeonCyan)
                    Divider(color = Color(0x11FFFFFF))
                    StatBreakdownRow(label = "ছবি বিশ্লেষণ ও রূপান্তর", value = userProfile.imageGenerations, color = NeonPurple)
                    Divider(color = Color(0x11FFFFFF))
                    StatBreakdownRow(label = "ভাষা অনুবাদ সম্পাদন", value = userProfile.translations, color = NeonTeal)
                    Divider(color = Color(0x11FFFFFF))
                    StatBreakdownRow(label = "সোশ্যাল মিডিয়া তৈরি পোস্ট", value = userProfile.socialPosts, color = NeonOrange)
                    Divider(color = Color(0x11FFFFFF))
                    StatBreakdownRow(label = "সংগীত লিরিক্স রচনা সমূহ", value = userProfile.songGenerations, color = NeonPink)
                }
            }
        }
    }
}

@Composable
fun StatBreakdownRow(
    label: String,
    value: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, color = TextWhite, fontSize = 13.sp)
        }
        Text(
            text = "$value বার",
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
