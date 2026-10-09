package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material3.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.AnalyzedFoodResponse
import com.example.data.api.MealSuggestionsResponse
import com.example.data.model.MealLog
import com.example.ui.viewmodel.DietViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietTrackerMainScreen(
    viewModel: DietViewModel,
    modifier: Modifier = Modifier
) {
    // Collect StateFlows from ViewModel
    val calorieGoal by viewModel.calorieGoal.collectAsStateWithLifecycle()
    val proteinGoal by viewModel.proteinGoal.collectAsStateWithLifecycle()
    val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
    val loggedMeals by viewModel.loggedMeals.collectAsStateWithLifecycle()

    val analyzerInput by viewModel.analyzerInput.collectAsStateWithLifecycle()
    val analyzerImage by viewModel.analyzerImage.collectAsStateWithLifecycle()
    val analyzerLoading by viewModel.analyzerLoading.collectAsStateWithLifecycle()
    val analyzerResult by viewModel.analyzerResult.collectAsStateWithLifecycle()
    val analyzerError by viewModel.analyzerError.collectAsStateWithLifecycle()

    val suggestionGoal by viewModel.suggestionGoal.collectAsStateWithLifecycle()
    val suggestionDiet by viewModel.suggestionDiet.collectAsStateWithLifecycle()
    val suggestionNotes by viewModel.suggestionNotes.collectAsStateWithLifecycle()
    val suggestionLoading by viewModel.suggestionLoading.collectAsStateWithLifecycle()
    val suggestionResult by viewModel.suggestionResult.collectAsStateWithLifecycle()
    val suggestionError by viewModel.suggestionError.collectAsStateWithLifecycle()

    // Local Compose State
    var selectedTab by remember { mutableStateOf(0) }
    var showCustomMealDialog by remember { mutableStateOf(false) }
    var showEditGoalsDialog by remember { mutableStateOf(false) }

    // Control FAB & Bottom navigation visibility based on scroll direction
    var isUiControlsVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5f) {
                    isUiControlsVisible = false
                } else if (available.y > 5f) {
                    isUiControlsVisible = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(selectedTab) {
        isUiControlsVisible = true
    }

    val focusManager = LocalFocusManager.current

    // Calculations based on current logs
    val totalCaloriesConsumed = loggedMeals.sumOf { it.calories }
    val totalProteinConsumed = loggedMeals.sumOf { it.protein.toInt() }.toFloat()
    val totalCarbsConsumed = loggedMeals.sumOf { it.carbs.toInt() }.toFloat()
    val totalFatsConsumed = loggedMeals.sumOf { it.fat.toInt() }.toFloat()
    val remainingCalories = (calorieGoal - totalCaloriesConsumed).coerceAtLeast(0)

    // Palette with custom fresh diet theme
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Scaffold(
        topBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                ) {
                    // Modern Botanical Artwork Background
                    Image(
                        painter = painterResource(id = R.drawable.img_diet_header_1782029808355),
                        contentDescription = "Botanical background decoration",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.42f
                    )
                    
                    // Gentle color layer gradient for text legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            val headerTitle = when (selectedTab) {
                                0 -> "Wellness Hub 🪴"
                                1 -> "Food Journal 📖"
                                2 -> "Vision Scanner 📸"
                                else -> "AI Botanist Coach 🤖"
                            }
                            val headerSubtitle = when (selectedTab) {
                                0 -> "Nurture physical balance & mind"
                                1 -> "Balance your daily macros"
                                2 -> "Scan or type to record nutrition"
                                else -> "Organic botanical dietary wisdom"
                            }
                            Text(
                                text = headerTitle,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = headerSubtitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }

                        // Theme Toggle & Settings buttons with glassmorphism styling
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val nextTheme = if (themeSetting == "dark") "light" else "dark"
                                    viewModel.updateThemeSetting(nextTheme)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    .testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (themeSetting == "dark") Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showEditGoalsDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    .testTag("edit_goals_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isUiControlsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(76.dp)
                    ) {
                        val items = listOf("Dashboard", "Diary", "Analyzer", "Coach")
                        items.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.25f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "tab_icon_scale"
                            )
                            
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                label = {
                                    Text(
                                        text = item,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier.graphicsLayer(
                                            scaleX = iconScale,
                                            scaleY = iconScale
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (index) {
                                                0 -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                                                1 -> if (isSelected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
                                                2 -> if (isSelected) Icons.Filled.AutoAwesome else Icons.Default.AutoAwesome
                                                else -> if (isSelected) Icons.Filled.LocalFireDepartment else Icons.Outlined.LocalFireDepartment
                                            },
                                            contentDescription = "$item Tab"
                                        )
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = when (index) {
                                    1 -> Modifier.testTag("diary_tab")
                                    2 -> Modifier.testTag("ai_analyzer_tab")
                                    3 -> Modifier.testTag("ai_plans_tab")
                                    else -> Modifier
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                AnimatedVisibility(
                    visible = isUiControlsVisible,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        text = { Text("Log Food") },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add custom meal") },
                        onClick = { showCustomMealDialog = true },
                        modifier = Modifier.testTag("add_custom_meal_fab"),
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Render a compact quick-stats bar above Diary to remind user of current budget in food journal view
            if (selectedTab == 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "DAILY CALORIES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$totalCaloriesConsumed / $calorieGoal kcal consumed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$remainingCalories left",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // --- TAB CONTENT CANVAS ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                when (selectedTab) {
                    0 -> {
                        val carbsGoal = (calorieGoal * 0.50f / 4f)
                        val fatsGoal = (calorieGoal * 0.25f / 9f)

                        DashboardTabScreen(
                            totalCaloriesConsumed = totalCaloriesConsumed,
                            calorieGoal = calorieGoal,
                            remainingCalories = remainingCalories,
                            totalProteinConsumed = totalProteinConsumed,
                            proteinGoal = proteinGoal,
                            totalCarbsConsumed = totalCarbsConsumed,
                            carbsGoal = carbsGoal,
                            totalFatsConsumed = totalFatsConsumed,
                            fatsGoal = fatsGoal,
                            onNavigateToDiary = { selectedTab = 1 },
                            onNavigateToCoach = { selectedTab = 3 },
                            showEditGoalsDialog = { showEditGoalsDialog = true }
                        )
                    }
                    1 -> LogsTabScreen(
                        loggedMeals = loggedMeals,
                        onDeleteMeal = { id -> viewModel.deleteMeal(id) },
                        onClearAll = { viewModel.clearAllLogs() }
                    )
                    2 -> AIAnalyzerTabScreen(
                        analyzerInput = analyzerInput,
                        analyzerImage = analyzerImage,
                        isLoading = analyzerLoading,
                        result = analyzerResult,
                        error = analyzerError,
                        onInputChange = { text -> viewModel.updateAnalyzerInput(text) },
                        onImageChange = { bitmap -> viewModel.updateAnalyzerImage(bitmap) },
                        onAnalyzeClick = {
                            focusManager.clearFocus()
                            viewModel.analyzeFood()
                        },
                        onAddLogClick = { selectedMealPeriod ->
                            viewModel.addAnalyzedResultToLog(selectedMealPeriod)
                            selectedTab = 1 // jump to food journal (Diary) tab upon action success!
                        }
                    )
                    3 -> AISuggestionsTabScreen(
                        goal = suggestionGoal,
                        diet = suggestionDiet,
                        notes = suggestionNotes,
                        isLoading = suggestionLoading,
                        result = suggestionResult,
                        error = suggestionError,
                        onGoalChange = { viewModel.updateSuggestionGoal(it) },
                        onDietChange = { viewModel.updateSuggestionDiet(it) },
                        onNotesChange = { viewModel.updateSuggestionNotes(it) },
                        onGenerateClick = {
                            focusManager.clearFocus()
                            viewModel.fetchMealSuggestions()
                        }
                    )
                }
            }
        }
    }

    // --- MANAGE DIALOGS ---

    // 1. EDIT GOALS DIALOG
    if (showEditGoalsDialog) {
        var localCalorieText by remember { mutableStateOf(calorieGoal.toString()) }
        var localProteinText by remember { mutableStateOf(proteinGoal.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showEditGoalsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Settings & Daily Goals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Quick Theme Picker Sector
                    Text(
                        text = "App Theme Preference",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val themeModeOptions = listOf(
                            "system" to "🖥️ System",
                            "light" to "☀️ Light",
                            "dark" to "🌙 Dark"
                        )
                        themeModeOptions.forEach { (mode, label) ->
                            val isSelected = themeSetting == mode
                            val containerCol = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            val contentCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(containerCol)
                                    .clickable { viewModel.updateThemeSetting(mode) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = contentCol,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Daily Goals Sector
                    Text(
                        text = "Daily Nutrient Targets",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = localCalorieText,
                        onValueChange = { localCalorieText = it },
                        label = { Text("Daily Calorie Target (kcal)") },
                        leadingIcon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_calorie_goal_input")
                    )

                    OutlinedTextField(
                        value = localProteinText,
                        onValueChange = { localProteinText = it },
                        label = { Text("Daily Protein Target (g)") },
                        leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_protein_goal_input")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Quick Preset Buttons
                    Text(
                        text = "Calorie & Protein Goal Presets",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            Triple("Slim/Loss", 1800, 130),
                            Triple("Balance", 2300, 110),
                            Triple("Bulk/Gain", 2900, 150)
                        )
                        presets.forEach { (name, cal, pro) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        localCalorieText = cal.toString()
                                        localProteinText = pro.toString()
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "$cal kcal",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val calories = localCalorieText.toIntOrNull() ?: calorieGoal
                        val protein = localProteinText.toFloatOrNull() ?: proteinGoal
                        viewModel.updateCalorieGoal(calories)
                        viewModel.updateProteinGoal(protein)
                        showEditGoalsDialog = false
                    },
                    modifier = Modifier.testTag("save_goals_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. LOG MANUAL CUSTOM MEAL DIALOG
    if (showCustomMealDialog) {
        var mealName by remember { mutableStateOf("Lunch") }
        var foodDescription by remember { mutableStateOf("") }
        var calText by remember { mutableStateOf("") }
        var proText by remember { mutableStateOf("") }
        var carbText by remember { mutableStateOf("") }
        var fatText by remember { mutableStateOf("") }

        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCustomMealDialog = false },
            title = {
                Text(
                    text = "Log Custom food",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        if (validationError != null) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = mealName,
                            onValueChange = { mealName = it },
                            label = { Text("Meal Label (e.g. Lunch)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_meal_name_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = foodDescription,
                            onValueChange = { foodDescription = it },
                            label = { Text("What did you eat? (e.g. Eggs & toast)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("manual_food_desc_input")
                        )
                    }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = calText,
                                onValueChange = { calText = it },
                                label = { Text("Cal (kcal)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_calories_input")
                            )
                            OutlinedTextField(
                                value = proText,
                                onValueChange = { proText = it },
                                label = { Text("Pro (g)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_protein_input")
                            )
                        }
                    }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = carbText,
                                onValueChange = { carbText = it },
                                label = { Text("Carb (g)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_carbs_input")
                            )
                            OutlinedTextField(
                                value = fatText,
                                onValueChange = { fatText = it },
                                label = { Text("Fat (g)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_fat_input")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (foodDescription.trim().isEmpty()) {
                            validationError = "Description cannot be empty!"
                            return@Button
                        }
                        val finalCals = calText.toIntOrNull()
                        val finalPro = proText.toFloatOrNull()
                        val finalCarbs = carbText.toFloatOrNull()
                        val finalFat = fatText.toFloatOrNull()

                        if (finalCals == null || finalPro == null) {
                            validationError = "Please enter valid Numbers for Calories and Protein."
                            return@Button
                        }

                        viewModel.logMeal(
                            name = mealName.trim(),
                            foodItems = foodDescription.trim(),
                            calories = finalCals,
                            protein = finalPro,
                            carbs = finalCarbs ?: 0f,
                            fat = finalFat ?: 0f
                        )
                        showCustomMealDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_manual_meal_button")
                ) {
                    Text("Save to Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMealDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// --- DASHBOARD TAB SCREEN ---
// ==========================================
@Composable
fun DashboardTabScreen(
    totalCaloriesConsumed: Int,
    calorieGoal: Int,
    remainingCalories: Int,
    totalProteinConsumed: Float,
    proteinGoal: Float,
    totalCarbsConsumed: Float,
    carbsGoal: Float,
    totalFatsConsumed: Float,
    fatsGoal: Float,
    onNavigateToDiary: () -> Unit,
    onNavigateToCoach: () -> Unit,
    showEditGoalsDialog: () -> Unit
) {
    var waterCups by remember { mutableStateOf(3) }
    val scrollState = rememberScrollState()

    val caloriePct = if (calorieGoal > 0) (totalCaloriesConsumed.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1.2f) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // --- 1. METABOLIC STATUS BANNER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val progressInt = (caloriePct * 100).toInt()
                    val feedbackText = when {
                        progressInt == 0 -> "Begin your day: Fuel your power!"
                        progressInt in 1..49 -> "Great start! Keep fueling your goals."
                        progressInt in 50..89 -> "More than halfway! Beautiful consistency."
                        progressInt in 90..105 -> "Perfect target hit! Incredible job."
                        else -> "Energy target surpassed. Fueling high active state!"
                    }
                    Text(
                        text = "METABOLIC FOCUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = feedbackText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💡", fontSize = 15.sp)
                }
            }
        }

        // --- 2. LUXURY CALORIE RING CARD (LIFESUM STYLE) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY BALANCE TARGET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.clickable { showEditGoalsDialog() }
                    ) {
                        Text(
                            text = "Set Goals",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progressFraction = if (calorieGoal > 0) {
                        (totalCaloriesConsumed.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    val animatedCalorieProgress by animateFloatAsState(
                        targetValue = progressFraction,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "CalorieProgressAnimation"
                    )

                    // Radiant Activity Ring
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val strokeWidth = 12.dp
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.secondary
                        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = trackColor,
                                style = Stroke(width = strokeWidth.toPx())
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(primaryColor, secondaryColor, primaryColor)
                                ),
                                startAngle = -90f,
                                sweepAngle = animatedCalorieProgress * 360f,
                                useCenter = false,
                                style = Stroke(
                                    width = strokeWidth.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "of daily budget",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Precise dynamic text metrics
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "Consumed: ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format("%,d kcal", totalCaloriesConsumed),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                            )
                            Text(
                                text = "Goal Target: ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format("%,d kcal", calorieGoal),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format("%,d kcal left", remainingCalories),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. PREMIUM MACRO CAPSULES ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroColumnItem(
                label = "Protein",
                consumed = totalProteinConsumed,
                goal = proteinGoal,
                color = MaterialTheme.colorScheme.secondary,
                emoji = "🍗",
                modifier = Modifier.weight(1f)
            )
            MacroColumnItem(
                label = "Carbs",
                consumed = totalCarbsConsumed,
                goal = carbsGoal,
                color = MaterialTheme.colorScheme.tertiary,
                emoji = "🥖",
                modifier = Modifier.weight(1f)
            )
            MacroColumnItem(
                label = "Fats",
                consumed = totalFatsConsumed,
                goal = fatsGoal,
                color = MaterialTheme.colorScheme.primary,
                emoji = "🥑",
                modifier = Modifier.weight(1f)
            )
        }

        // --- 4. WEEKLY STREAK ACTIVITY RADAR ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Aesthetic Wellness Track".uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    letterSpacing = 1.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("M" to true, "T" to true, "W" to true, "T" to true, "F" to (totalCaloriesConsumed > 0), "S" to false, "S" to false)
                    days.forEachIndexed { i, (day, active) ->
                        val isToday = i == 4 // Today is Friday
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isToday && active -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                            active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f)
                                        }
                                    )
                                    .border(
                                        width = if (isToday) 2.dp else 1.dp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Logged",
                                        tint = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. INTERACTIVE WATER TRACKER CARD (BOUNCING RIPPLE) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF33B3FF).copy(alpha = 0.6f),
                            Color(0xFF0066FF).copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE0F2F1), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Water tracking icon",
                                tint = Color(0xFF00838F),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "HYDRATION METER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00838F),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$waterCups of 8 cups logged today",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Add cup button with nice ripple feedback
                    IconButton(
                        onClick = { if (waterCups < 12) waterCups++ },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00ACC1))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log Water Glass",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Interactive row of beautiful active water drops
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..8) {
                        val isLogged = i <= waterCups
                        val scale by animateFloatAsState(
                            targetValue = if (isLogged) 1.2f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "water_drop_spring_$i"
                        )
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    if (waterCups >= i) {
                                        waterCups = i - 1
                                    } else {
                                        waterCups = i
                                    }
                                }
                                .graphicsLayer(scaleX = scale, scaleY = scale),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLogged) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                                contentDescription = "Glass $i of water",
                                tint = if (isLogged) Color(0xFF0097A7) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }


    }
}

// ==========================================
// --- DAILY LOGS TAB SCREEN ---
// ==========================================
@Composable
fun LogsTabScreen(
    loggedMeals: List<MealLog>,
    onDeleteMeal: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    if (loggedMeals.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestaurantMenu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Your Food Diary is Empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use the plus button or AI analyzer to easily record meal photos & describe what you had.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        val totalCals = loggedMeals.sumOf { it.calories }
        val totalPro = loggedMeals.sumOf { it.protein.toInt() }
        val totalCarb = loggedMeals.sumOf { it.carbs.toInt() }
        val totalFat = loggedMeals.sumOf { it.fat.toInt() }

        Column(modifier = Modifier.fillMaxSize()) {
            // --- DIARY CALORIE SUMMARY HEADER (ALIGNED PERFECTLY) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIARY DAILY TOTALS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$totalCals kcal logged",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🥩 Protein", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${totalPro}g", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌾 Carbs", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${totalCarb}g", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🥑 Fats", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${totalFat}g", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline Entries (${loggedMeals.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_all_logs_button"),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Day", fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(loggedMeals, key = { it.id }) { meal ->
                    MealLogItemCard(meal = meal, onDelete = { onDeleteMeal(meal.id) })
                }
            }
        }
    }
}

@Composable
fun MealLogItemCard(
    meal: MealLog,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(meal.timestamp) { timeFormat.format(Date(meal.timestamp)) }

    val (periodBg, periodFg, periodEmoji, accentColor) = when (meal.name.lowercase().trim()) {
        "breakfast" -> Quadruple(Color(0xFFFFF3E0), Color(0xFFE65100), "🍳", Color(0xFFFF9800))
        "lunch" -> Quadruple(Color(0xFFE3F2FD), Color(0xFF0D47A1), "🥗", Color(0xFF2196F3))
        "dinner" -> Quadruple(Color(0xFFEDE7F6), Color(0xFF4A148C), "🍲", Color(0xFF9C27B0))
        "snack", "snacks" -> Quadruple(Color(0xFFE8F5E9), Color(0xFF1B5E20), "🍎", Color(0xFF4CAF50))
        else -> Quadruple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "🍽️", MaterialTheme.colorScheme.secondary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("meal_item_${meal.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Accent bar on the left side
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(periodEmoji, fontSize = 20.sp)
                        Box(
                            modifier = Modifier
                                .background(periodBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = meal.name.uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = periodFg,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = formattedTime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_meal_${meal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete this meal",
                            tint = Color.Red.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Food outline items
                Text(
                    text = meal.foodItems,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                // Macro breakdown items (ALIGNED BEAUTIFULLY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${meal.calories} kcal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MacroBadge(label = "Pro", value = "${meal.protein.toInt()}g", color = Color(0xFF4CAF50))
                        MacroBadge(label = "Carb", value = "${meal.carbs.toInt()}g", color = Color(0xFFFF9800))
                        MacroBadge(label = "Fat", value = "${meal.fat.toInt()}g", color = Color(0xFFE91E63))
                    }
                }
            }
        }
    }
}

// Data holder helper
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun MacroBadge(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

// ==========================================
// --- AI FOOD ANALYZER TAB SCREEN ---
// ==========================================
@Composable
fun AIAnalyzerTabScreen(
    analyzerInput: String,
    analyzerImage: Bitmap?,
    isLoading: Boolean,
    result: AnalyzedFoodResponse?,
    error: String?,
    onInputChange: (String) -> Unit,
    onImageChange: (Bitmap?) -> Unit,
    onAnalyzeClick: () -> Unit,
    onAddLogClick: (String) -> Unit
) {
    var mealSelection by remember { mutableStateOf("Lunch") }
    val mealPeriods = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Explanatory card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Plate Recognition Scanner",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Take a food snapshot or type description items. Gemini smart modeling instantly estimates calories & precise macros.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Photo upload / capture component
        item {
            val context = LocalContext.current
            val contentResolver = context.contentResolver
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(contentResolver, uri)
                            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val stream = contentResolver.openInputStream(uri)
                            BitmapFactory.decodeStream(stream)
                        }
                        onImageChange(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap: Bitmap? ->
                if (bitmap != null) {
                    onImageChange(bitmap)
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (analyzerImage == null) {
                        // High-tech target viewport reticle focus outline
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Reticle background markings draw
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 2.dp.toPx()
                                val len = 16.dp.toPx()
                                // Top-Left
                                drawLine(Color.Gray.copy(0.4f), start = Offset(16.dp.toPx(), 16.dp.toPx()), end = Offset(16.dp.toPx() + len, 16.dp.toPx()), strokeWidth = stroke)
                                drawLine(Color.Gray.copy(0.4f), start = Offset(16.dp.toPx(), 16.dp.toPx()), end = Offset(16.dp.toPx(), 16.dp.toPx() + len), strokeWidth = stroke)
                                // Top-Right
                                drawLine(Color.Gray.copy(0.4f), start = Offset(size.width - 16.dp.toPx(), 16.dp.toPx()), end = Offset(size.width - 16.dp.toPx() - len, 16.dp.toPx()), strokeWidth = stroke)
                                drawLine(Color.Gray.copy(0.4f), start = Offset(size.width - 16.dp.toPx(), 16.dp.toPx()), end = Offset(size.width - 16.dp.toPx(), 16.dp.toPx() + len), strokeWidth = stroke)
                                // Bottom-Left
                                drawLine(Color.Gray.copy(0.4f), start = Offset(16.dp.toPx(), size.height - 16.dp.toPx()), end = Offset(16.dp.toPx() + len, size.height - 16.dp.toPx()), strokeWidth = stroke)
                                drawLine(Color.Gray.copy(0.4f), start = Offset(16.dp.toPx(), size.height - 16.dp.toPx()), end = Offset(16.dp.toPx(), size.height - 16.dp.toPx() - len), strokeWidth = stroke)
                                // Bottom-Right
                                drawLine(Color.Gray.copy(0.4f), start = Offset(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()), end = Offset(size.width - 16.dp.toPx() - len, size.height - 16.dp.toPx()), strokeWidth = stroke)
                                drawLine(Color.Gray.copy(0.4f), start = Offset(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()), end = Offset(size.width - 16.dp.toPx(), size.height - 16.dp.toPx() - len), strokeWidth = stroke)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🍽️ Scan Center", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ready to analyze food plate...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    } else {
                        // Image Captured status viewport
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Image(
                                bitmap = analyzerImage.asImageBitmap(),
                                contentDescription = "Captured Food Item",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Captured Badge Label Overlay
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(Color.Green, CircleShape))
                                Text("READY SCAN PLATE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // Bottom Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(42.dp)
                                .testTag("camera_capture_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Snap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        ElevatedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(42.dp)
                                .testTag("gallery_picker_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (analyzerImage != null) {
                            IconButton(
                                onClick = { onImageChange(null) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Picture Choice",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search text box (optional if image is provided)
        item {
            OutlinedTextField(
                value = analyzerInput,
                onValueChange = onInputChange,
                placeholder = { Text(if (analyzerImage != null) "Optional: Add any description details..." else "What did you eat? Type logs here...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_food_input_field"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Run button
        item {
            Button(
                onClick = onAnalyzeClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("analyze_food_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI is analyzing food...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze with Gemini")
                }
            }
        }

        // Show parsing loading indicators or errors
        if (error != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Results Card Section
        if (result != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI ESTIMATION RESULT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Render each estimated food item
                        result.foodItems.forEach { item ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.calories} kcal",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Protein: ${item.protein}g  •  Carbs: ${item.carbs}g  •  Fat: ${item.fat}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Totals Card Block
                        Text(
                            text = "ESTIMATED TOTALS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${result.totalCalories} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MacroBadge(label = "Pro", value = "${result.totalProtein.toInt()}g", color = Color(0xFF4CAF50))
                                MacroBadge(label = "Carb", value = "${result.totalCarbs.toInt()}g", color = Color(0xFFFF9800))
                                MacroBadge(label = "Fat", value = "${result.totalFat.toInt()}g", color = Color(0xFFE91E63))
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Choose meal log target
                        Text(
                            text = "Save as:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            mealPeriods.forEach { period ->
                                val isSelected = mealSelection == period
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { mealSelection = period },
                                    label = { Text(period) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Add Log button
                        Button(
                            onClick = { onAddLogClick(mealSelection) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_analyzed_to_log_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm & Add to Tracker")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// --- AI MEAL SUGGESTIONS TAB SCREEN ---
// ==========================================
@Composable
fun AISuggestionsTabScreen(
    goal: String,
    diet: String,
    notes: String,
    isLoading: Boolean,
    result: MealSuggestionsResponse?,
    error: String?,
    onGoalChange: (String) -> Unit,
    onDietChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onGenerateClick: () -> Unit
) {
    val goalOptions = listOf("Lose weight", "Gain muscle", "Maintain weight")
    val dietOptions = listOf("Vegetarian", "Vegan", "Non-Vegetarian", "Keto", "High Protein")

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Banner card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Personalized Diet Planner",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Select your specific wellness goals and dietary preferences. Gemini will craft custom meal ideas tailored for your body.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: TARGET GOAL
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TARGET FITNESS GOAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    goalOptions.forEach { option ->
                        val isSelected = goal == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { onGoalChange(option) }
                                .padding(vertical = 10.dp)
                                .testTag("goal_chip_${option.replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section: DIETARY TYPE
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DIETARY TYPE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dietOptions.forEach { option ->
                        val isSelected = diet == option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { onDietChange(option) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("diet_chip_${option.replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section: CUSTOM NOTES
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ANY ALLERGIES OR EXTRA CUSTOM NOTES/PREFERENCES?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {
                        Text(
                            text = "e.g. strict low sodium, peanut allergies, include avocado and green tea requests...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diet_planner_notes_field")
                )
            }
        }

        // Submit action Button
        item {
            Button(
                onClick = onGenerateClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_ideas_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("AI is engineering your diet plan...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Design Plan Suggestions", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }

        // Display results or errors
        if (error != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Parsed suggest meals outputs
        if (result != null) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PERSONALIZED STRATEGY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    result.meals.forEach { meal ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = meal.type,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = meal.approxMacros,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = meal.suggestion,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper Composable to wrap layout elements, supporting inline wrapping FlowRow
@Composable
fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val spacingPx = with(density) { 8.dp.roundToPx() }
    
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        
        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > layoutWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + spacingPx
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        
        var totalHeight = 0
        val rowHeights = rows.map { row ->
            val height = row.maxOfOrNull { it.height } ?: 0
            totalHeight += height + spacingPx
            height
        }
        if (totalHeight > 0) totalHeight -= spacingPx
        
        layout(layoutWidth, totalHeight.coerceAtLeast(0)) {
            var currentY = 0
            rows.forEachIndexed { rowIndex, row ->
                var currentX = 0
                val rowHeight = rowHeights[rowIndex]
                row.forEach { placeable ->
                    placeable.placeRelative(currentX, currentY + (rowHeight - placeable.height) / 2)
                    currentX += placeable.width + spacingPx
                }
                currentY += rowHeight + spacingPx
            }
        }
    }
}

@Composable
fun MacroColumnItem(
    label: String,
    consumed: Float,
    goal: Float? = null,
    color: Color,
    emoji: String = "✨",
    modifier: Modifier = Modifier
) {
    val progressFraction = if (goal != null && goal > 0) {
        (consumed / goal).coerceIn(0f, 1.2f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "${label}Progress"
    )

    Column(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.08f),
                        color.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Emoji box representation
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 16.sp)
        }

        // Label
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.8.sp
        )

        // Progress Pill bar
        if (goal != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Consumed texts
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "${consumed.toInt()}g",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (goal != null) {
                Text(
                    text = "of ${goal.toInt()}g",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
