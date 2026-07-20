/*
 * Mushotoku — a privacy-focused, offline productivity app.
 * Copyright (C) 2026 Tom Frischmuth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mushotoku.app

import android.Manifest
import android.app.LocaleManager
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mushotoku.app.notification.ReminderScheduler
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale as JavaLocale
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import com.mushotoku.app.holidays.localizedFor
import com.mushotoku.app.ui.strings.AppStrings
import com.mushotoku.app.ui.screens.CalendarScreen
import com.mushotoku.app.ui.screens.NoteEditorBarState
import com.mushotoku.app.ui.screens.FinanceScreen
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.LocalAppCurrency
import com.mushotoku.app.ui.currencyByCode
import com.mushotoku.app.ui.screens.MeditationScreen
import com.mushotoku.app.ui.screens.NoteEditor
import com.mushotoku.app.ui.screens.NotesScreen
import com.mushotoku.app.ui.screens.collectTags
import com.mushotoku.app.ui.screens.isDarkSurface
import com.mushotoku.app.ui.screens.noteAccentColor
import com.mushotoku.app.ui.screens.noteHasTag
import com.mushotoku.app.ui.screens.withoutTag
import com.mushotoku.app.ui.screens.SettingsScreen
import com.mushotoku.app.ui.screens.SettingsSection
import com.mushotoku.app.ui.screens.TaskScreen
import com.mushotoku.app.ui.screens.TrashScreen
import com.mushotoku.app.ui.components.*
import com.mushotoku.app.ui.brand.MushotokuBrand
import com.mushotoku.app.ui.brand.MushotokuWordmark
import com.mushotoku.app.ui.theme.DarkAppColors
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.data.TaskStatus
import com.mushotoku.app.util.performCheckHaptic
import com.mushotoku.app.viewmodel.AppViewModel
import com.mushotoku.app.viewmodel.MeditationViewModel
import com.mushotoku.app.viewmodel.*
import java.time.YearMonth
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.mushotoku.app.ui.components.LocalHazeState
import com.mushotoku.app.ui.components.LocalGlassOverlayHost
import com.mushotoku.app.ui.dialogs.*
import com.mushotoku.app.ui.components.GlassOverlayHost
import com.mushotoku.app.ui.components.rememberGlassOverlayHostState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
internal fun BrandSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MushotokuBrand.Dark),
        contentAlignment = Alignment.Center
    ) {
        MushotokuWordmark(
            modifier    = Modifier.fillMaxWidth(0.7f),
            letterColor = DarkAppColors.onSurface
        )
    }
}

@Composable
fun MushotokuApp(
    vm: AppViewModel,
    meditationVm: MeditationViewModel,
    addTaskTrigger: Flow<Unit>   = emptyFlow(),
    openTodayTrigger: Flow<Unit> = emptyFlow()
) {
    val notesVm: NotesViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val tasksVm: TasksViewModel = viewModel()
    val financeVm: FinanceViewModel = viewModel()
    val habitsVm: HabitsViewModel = viewModel()
    val tasks                by tasksVm.tasks.collectAsStateWithLifecycle()
    val habits               by habitsVm.scheduledHabits.collectAsStateWithLifecycle()
    val habitCompletions     by habitsVm.habitCompletions.collectAsStateWithLifecycle()
    val habitStreaks          by habitsVm.habitStreaks.collectAsStateWithLifecycle()
    val allHabitLogs         by habitsVm.allHabitLogs.collectAsStateWithLifecycle()
    val notes                by notesVm.notes.collectAsStateWithLifecycle()
    val deletedNotes         by notesVm.deletedNotes.collectAsStateWithLifecycle()
    val expenses                     by financeVm.expenses.collectAsStateWithLifecycle()
    val historicalExpenses           by financeVm.historicalExpenses.collectAsStateWithLifecycle()
    val selectedDate                 by vm.selectedDate.collectAsStateWithLifecycle()
    val calendarMonth                by vm.calendarMonth.collectAsStateWithLifecycle()
    val appointmentsForMonth         by tasksVm.appointmentsForMonth.collectAsStateWithLifecycle()
    val allAppointments              by tasksVm.allAppointments.collectAsStateWithLifecycle()
    val categories                   by financeVm.categories.collectAsStateWithLifecycle()
    val recurringCostHistory         by financeVm.recurringCostHistory.collectAsStateWithLifecycle()
    val yearExpenses                 by financeVm.yearExpenses.collectAsStateWithLifecycle()
    val additionalIncomes            by financeVm.additionalIncomes.collectAsStateWithLifecycle()
    val historicalAdditionalIncomes  by financeVm.historicalAdditionalIncomes.collectAsStateWithLifecycle()
    val settings                     by settingsVm.settings.collectAsStateWithLifecycle()

    LaunchedEffect(selectedDate)  { tasksVm.setDate(selectedDate); financeVm.setDate(selectedDate); habitsVm.setDate(selectedDate) }
    LaunchedEffect(calendarMonth) { tasksVm.setMonth(calendarMonth) }

    val appContext = LocalContext.current
    val appliedLanguage by settingsVm.appliedLanguage.collectAsStateWithLifecycle()
    LaunchedEffect(appliedLanguage) {
        val lang = appliedLanguage ?: return@LaunchedEffect
        val lm = appContext.getSystemService(LocaleManager::class.java)
        val desired = localeListForSetting(lang)
        if (lm != null && lm.applicationLocales != desired) {
            lm.applicationLocales = desired
        }
    }
    val configuration = LocalConfiguration.current
    val appStrings = remember(configuration) { AppStrings(appContext) }

    val hazeState = rememberHazeState()
    val overlayHost = rememberGlassOverlayHostState()
    val appCurrency = remember(settings.currency) { currencyByCode(settings.currency) }

    val holidayBaseCtx = LocalContext.current
    val holidayProvider = remember { com.mushotoku.app.holidays.DefaultHolidayProvider() }
    val holidayRegion = remember(settings.holidayCountry, settings.holidayRegion) {
        com.mushotoku.app.holidays.HolidayDefaults.resolveRegion(
            holidayBaseCtx, settings.holidayCountry, settings.holidayRegion
        )
    }

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalAppCurrency provides appCurrency,
        LocalHazeState provides hazeState,
        LocalGlassOverlayHost provides overlayHost
    ) {
        val strings = LocalAppStrings.current

        val holidayLocalizedCtx = remember(strings.locale) { holidayBaseCtx.localizedFor(strings.locale) }
        val selectedDateHolidayNames = remember(selectedDate, holidayRegion, settings.showHolidays) {
            if (settings.showHolidays) {
                holidayProvider.holidays(holidayRegion, selectedDate.year..selectedDate.year)
                    .filter { it.date == selectedDate }
                    .map { com.mushotoku.app.holidays.HolidayNames.resolve(holidayLocalizedCtx, it.nameKey) }
            } else emptyList()
        }

        val notifContext = LocalContext.current
        var notificationsAllowed by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    notifContext, Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
            )
        }
        val notifPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> notificationsAllowed = granted }

        LaunchedEffect(settings.notificationsEnabled, notificationsAllowed) {
            if (settings.notificationsEnabled && !notificationsAllowed) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        LaunchedEffect(allAppointments, settings.notificationsEnabled, settings.notificationLeadMinutes) {
            // Skip until the appointments have actually loaded from the DB. Running
            // with the pre-load empty list would cancel every scheduled alarm.
            val appointments = allAppointments ?: return@LaunchedEffect
            ReminderScheduler.sync(
                context      = notifContext,
                appointments = appointments,
                enabled      = settings.notificationsEnabled,
                leadMinutes  = settings.notificationLeadMinutes,
            )
        }

        var currentTab          by remember { mutableStateOf(AppTab.TODAY) }
        var dateSliderScrollTrigger by remember { mutableIntStateOf(0) }
        var showAddTask         by remember { mutableStateOf(false) }
        // Which note is open is held here, not inside the notes screen: the
        // editor is drawn with the other full-screen overlays, above the bottom
        // bar. That way nothing has to be hidden and brought back for it.
        var editingNote         by remember { mutableStateOf<Note?>(null) }
        var creatingNote        by remember { mutableStateOf(false) }
        val noteEditorActive    = editingNote != null || creatingNote
        var noteEditorBarState  by remember { mutableStateOf<NoteEditorBarState?>(null) }
        LaunchedEffect(noteEditorActive) { if (!noteEditorActive) noteEditorBarState = null }
        var showFinanceOverview by remember { mutableStateOf(false) }
        var showCalendar        by remember { mutableStateOf(false) }
        var showSettings           by remember { mutableStateOf(false) }
        var settingsInitialSection by remember { mutableStateOf<SettingsSection?>(null) }
        var showBudgetOverview     by remember { mutableStateOf(false) }
        var showTrash              by remember { mutableStateOf(false) }
        var showMeditation         by remember { mutableStateOf(false) }
        var selectedNoteIds        by remember { mutableStateOf<Set<Long>>(emptySet()) }

        val focusManager = LocalFocusManager.current
        val scope        = rememberCoroutineScope()
        val taskSnackbar = remember { SnackbarHostState() }
        val anyOverlayOpen = showAddTask || noteEditorActive || showCalendar || showSettings ||
                showFinanceOverview || showBudgetOverview || showTrash || showMeditation
        LaunchedEffect(anyOverlayOpen) { if (anyOverlayOpen) focusManager.clearFocus() }

        LaunchedEffect(currentTab) { selectedNoteIds = emptySet() }

        LaunchedEffect(Unit) {
            addTaskTrigger.collect {
                currentTab  = AppTab.TODAY
                showAddTask = true
            }
        }
        LaunchedEffect(Unit) {
            openTodayTrigger.collect {
                currentTab          = AppTab.TODAY
                showSettings        = false
                showCalendar        = false
                showFinanceOverview = false
                showBudgetOverview  = false
                showTrash           = false
                showMeditation      = false
            }
        }

        val linkedNoteToTaskMap = remember(allAppointments) {
            allAppointments.orEmpty()
                .filter { it.linkedNoteId != null }
                .associateBy { it.linkedNoteId!! }
        }

        LaunchedEffect(settings.financeTabEnabled) {
            if (!settings.financeTabEnabled && currentTab == AppTab.FINANCE) {
                currentTab = AppTab.TODAY
            }
        }

        val colors   = LocalAppColors.current
        val density  = LocalDensity.current

        val isDark = (colors.background.red + colors.background.green + colors.background.blue) < 1.5f
        val glassStyle = HazeStyle(
            blurRadius   = 22.dp,
            tints        = listOf(HazeTint(
                if (isDark) Color(0xFF0E0E0E).copy(alpha = 0.55f)
                else        Color.White.copy(alpha = 0.62f)
            )),
            fallbackTint = HazeTint(
                if (isDark) Color(0xFF1C1C1C)
                else        Color(0xFFF0F0F5)
            )
        )
        val glassDividerColor = if (isDark) Color.White.copy(alpha = 0.07f)
                                else        Color.Black.copy(alpha = 0.05f)

        var topBarHeightPx    by remember { mutableIntStateOf(0) }
        var bottomBarHeightPx by remember { mutableIntStateOf(0) }

        val topPadding    = with(density) { topBarHeightPx.toDp() }
        val bottomPadding = with(density) { bottomBarHeightPx.toDp() }

        val contentPadding = PaddingValues(
            top    = topPadding,
            bottom = bottomPadding
        )

        // Switched off while the note editor is open: it brings its own handler,
        // and leaving both enabled let one back gesture close the note and jump
        // to the Today tab in the same motion.
        // Back closes what is open on top; on a bare Finance or Notes tab it
        // deliberately does nothing, so the gesture cannot throw the reader back
        // to Today.
        BackHandler(enabled = true) {
            when {
                showMeditation -> showMeditation = false
                showCalendar   -> showCalendar = false
                showTrash      -> showTrash = false
                showSettings   -> showSettings = false
            }
        }

        Box(Modifier.fillMaxSize()) {

            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                when (currentTab) {
                    AppTab.TODAY -> TaskScreen(
                        tasks            = tasks,
                        habits           = habits,
                        habitCompletions = habitCompletions,
                        habitStreaks      = habitStreaks,
                        allHabitLogs     = allHabitLogs,
                        selectedDate     = selectedDate,
                        holidayNames     = selectedDateHolidayNames,
                        contentPadding   = contentPadding,
                        onStatusClick    = {
                            val becomesDone = if (it.isAppointment) !it.isDone else it.status == TaskStatus.YELLOW
                            if (settings.hapticFeedbackEnabled && becomesDone) appContext.performCheckHaptic()
                            if (it.isAppointment) tasksVm.toggleDone(it) else tasksVm.cycleStatus(it)
                        },
                        onTitleSave      = { task, title -> tasksVm.updateTaskTitle(task, title) },
                        onMoveToTomorrow = { task ->
                            // The task leaves Today silently, so offer the way back.
                            val from = task.date
                            tasksVm.moveToTomorrow(task)
                            scope.launch {
                                val result = taskSnackbar.showSnackbar(
                                    message     = strings.taskMovedTomorrow,
                                    actionLabel = strings.undo,
                                    duration    = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    tasksVm.moveToDate(task, from)
                                }
                            }
                        },
                        onReschedule     = { task, date, time -> tasksVm.moveToDate(task, date, time) },
                        onDelete         = { tasksVm.deleteTask(it) },
                        onReorder        = { tasksVm.reorderTasks(it) },
                        onToggleHabit        = {
                            if (settings.hapticFeedbackEnabled && it.id !in habitCompletions) appContext.performCheckHaptic()
                            habitsVm.toggleHabit(it, selectedDate)
                        },
                        onUpdateHabit        = { habitsVm.updateHabit(it) },
                        onDeleteHabit        = { habitsVm.deleteHabit(it) },
                        confirmDeleteEnabled = settings.confirmDeleteEnabled,
                        onOpenLinkedNote     = { task ->
                            task.linkedNoteId?.let { noteId ->
                                notes.firstOrNull { it.id == noteId }?.let { editingNote = it }
                                currentTab = AppTab.NOTES
                            }
                        }
                    )
                    AppTab.FINANCE -> FinanceScreen(
                        selectedDate                = selectedDate,
                        expenses                    = expenses,
                        historicalExpenses          = historicalExpenses,
                        categories                  = categories,
                        recurringCostHistory        = recurringCostHistory,
                        yearExpenses                = yearExpenses,
                        contentPadding              = contentPadding,
                        salary                      = settings.salary,
                        salaryDay                   = settings.salaryDay,
                        additionalIncomes           = additionalIncomes,
                        historicalAdditionalIncomes = historicalAdditionalIncomes,
                        onAddIncome                 = { label, amount -> financeVm.addAdditionalIncome(label, amount) },
                        onDeleteIncome              = { financeVm.deleteAdditionalIncome(it) },
                        showOverview                = showFinanceOverview,
                        onDismissOverview           = { showFinanceOverview = false },
                        showBudgetOverview          = showBudgetOverview,
                        onDismissBudgetOverview     = { showBudgetOverview = false },
                        onAdd                       = { financeVm.addExpense(it.id, selectedDate) },
                        onRemove                    = { financeVm.removeExpense(it.id, selectedDate) },
                        onSetAmount                 = { cat, amt -> financeVm.setExpenseAmount(cat.id, amt, selectedDate) }
                    )
                    AppTab.NOTES -> NotesScreen(
                        notes                 = notes,
                        contentPadding        = contentPadding,
                        editorActive          = noteEditorActive,
                        onOpenNote            = { editingNote = it },
                        onDeleteTag           = { tag, withNotes ->
                            val carrying = notes.filter { noteHasTag(it, tag) }
                            if (withNotes) carrying.forEach { notesVm.deleteNote(it) }
                            else carrying.forEach { notesVm.updateNote(it.withoutTag(tag)) }
                        },
                        selectedNoteIds          = selectedNoteIds,
                        onSelectionChange        = { selectedNoteIds = it },
                        linkedNoteToTaskMap      = linkedNoteToTaskMap,
                    )
                }
            }


            if (currentTab == AppTab.TODAY) {
                FloatingActionButton(
                    onClick = soundClick { showAddTask = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomPadding + 16.dp),
                    containerColor = Color(0xFF3D5AFE)
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.add, tint = Color.White)
                }
            } else if (currentTab == AppTab.NOTES && !noteEditorActive) {
                FloatingActionButton(
                    onClick = soundClick { creatingNote = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomPadding + 16.dp),
                    containerColor = Color(0xFF3D5AFE)
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.add, tint = Color.White)
                }
            }

            // While a note is open the bar is plain, not frosted: what lies
            // behind it is the note itself, so there is nothing to show through.
            // It shares the grey of the markdown bar right below it, so the two
            // read as one strip of chrome above the text.
            val editorTint = if (noteEditorActive) colors.surface else null
            // The note's own hue at full strength; an uncoloured note keeps the
            // app's blue.
            val editorAccent = editingNote?.color
                ?.let { noteAccentColor(it, colors.background.isDarkSurface()) }
                ?: Color(0xFF3D5AFE)
            val topBarModifier = Modifier
                .align(Alignment.TopCenter)
                .let { if (editorTint != null) it.background(editorTint) else it.hazeEffect(hazeState, glassStyle) }
                .onSizeChanged { topBarHeightPx = it.height }

            SnackbarHost(
                hostState = taskSnackbar,
                modifier  = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding + 12.dp, start = 16.dp, end = 16.dp)
            ) { data ->
                // The action carries the app's accent, like every other one.
                Snackbar(snackbarData = data, actionContentColor = Color(0xFF3D5AFE))
            }

            // Always present now: the note editor is drawn over it instead of
            // making it disappear and come back.
            AppBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .hazeEffect(hazeState, glassStyle)
                    .onSizeChanged { bottomBarHeightPx = it.height },
                currentTab        = currentTab,
                selectedDate      = selectedDate,
                financeTabEnabled = settings.financeTabEnabled,
                glassDividerColor = glassDividerColor,
                onDateSelected    = { vm.selectDate(it) },
                onTodayLongPress  = { showMeditation = true },
                onTabChange       = {
                    if (it == AppTab.TODAY || it == AppTab.FINANCE) {
                        vm.selectDate(java.time.LocalDate.now())
                        dateSliderScrollTrigger++
                    }
                    currentTab = it
                },
                dateScrollTrigger = dateSliderScrollTrigger
            )

            // Above the bottom bar so it covers it, below the top bar which
            // carries the editor's own back and done buttons.
            if (noteEditorActive) {
                val linkedTask = editingNote?.id?.let { linkedNoteToTaskMap[it] }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent().changes.forEach { it.consume() }
                                }
                            }
                        }
                ) {
                    NoteEditor(
                        note        = editingNote,
                        defaultType = NoteType.NOTE,
                        onCreate    = { t, c, tp, cb -> notesVm.createNote(t, c, tp, cb) },
                        onUpdate    = { notesVm.updateNote(it) },
                        onUpdateQuiet = { notesVm.updateNoteQuietly(it) },
                        onDelete    = { notesVm.deleteNote(it) },
                        onClose     = { editingNote = null; creatingNote = false },
                        // Only the system's own inset: the editor covers the app's
                        // bottom bar, so reserving its height would leave a dead
                        // strip across the lower part of the note.
                        bottomPad   = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding(),
                        topPad      = contentPadding.calculateTopPadding(),
                        onBarState  = { noteEditorBarState = it },
                        linkedTask  = linkedTask,
                        onNavigateToTask = { task ->
                            editingNote = null
                            creatingNote = false
                            vm.selectDate(java.time.LocalDate.ofEpochDay(task.date))
                            currentTab = AppTab.TODAY
                        },
                        hapticEnabled = settings.hapticFeedbackEnabled,
                        startInTitle  = settings.newNoteStartsWithTitle,
                        knownTags     = remember(notes) { collectTags(notes) },
                        accent        = editorAccent
                    )
                }
            }

            // Drawn last of the three so the editor covers the bottom bar but
            // keeps the top bar, which carries its back and done buttons.
            when (currentTab) {
                AppTab.TODAY -> TodayTopBar(
                    modifier       = topBarModifier,
                    selectedDate   = selectedDate,
                    onOpenMindfulness = { showMeditation = true },
                    onOpenCalendar = {
                        vm.setCalendarMonth(YearMonth.from(selectedDate))
                        showCalendar = true
                    },
                    onOpenSettings = {
                        settingsInitialSection = null
                        showSettings = true
                    }
                )
                AppTab.NOTES -> NotesTopBar(
                    modifier         = topBarModifier,
                    noteEditorActive = noteEditorActive,
                    editorBar        = noteEditorBarState,
                    selectedNoteIds  = selectedNoteIds,
                    notes            = notes,
                    onClearSelection = { selectedNoteIds = emptySet() },
                    editorAccent = editorAccent,
                    onSetColor = { color ->
                        notesVm.setColor(notes.filter { it.id in selectedNoteIds }, color)
                    },
                    onPinSelected = {
                        val chosen = notes.filter { it.id in selectedNoteIds }
                        // All pinned already means the button unpins; pinNote toggles,
                        // so skip the notes that are already in the wanted state.
                        val pinning = !chosen.all { it.isPinned }
                        chosen.filter { it.isPinned != pinning }.forEach { notesVm.pinNote(it) }
                        selectedNoteIds = emptySet()
                    },
                    onDeleteSelected = {
                        notes.filter { it.id in selectedNoteIds }.forEach { notesVm.deleteNote(it) }
                        selectedNoteIds = emptySet()
                    },
                    onOpenTrash      = { showTrash = true },
                    onOpenSettings   = {
                        settingsInitialSection = null
                        showSettings = true
                    }
                )
                AppTab.FINANCE -> FinanceTopBar(
                    modifier              = topBarModifier,
                    selectedDate          = selectedDate,
                    onOpenMonthlyOverview = { showFinanceOverview = true },
                    onOpenBudget          = { showBudgetOverview = true },
                    onOpenSettings        = {
                        settingsInitialSection = SettingsSection.Finanzen
                        showSettings = true
                    }
                )
            }

            if (showMeditation) {
                MeditationScreen(
                    vm      = meditationVm,
                    strings = strings,
                    onClose = { showMeditation = false }
                )
            }

            if (showCalendar) {
                CalendarScreen(
                    currentMonth     = calendarMonth,
                    appointments     = appointmentsForMonth,
                    onMonthChange    = { vm.setCalendarMonth(it) },
                    onNavigateToDate  = { date -> vm.selectDate(date); showCalendar = false; currentTab = AppTab.TODAY },
                    onAddAppointment  = { title, time, date -> tasksVm.addAppointmentForDate(title, time, date) },
                    onClose           = { showCalendar = false },
                    holidayOn         = { date ->
                        if (settings.showHolidays) holidayProvider.holidayOn(holidayRegion, date) else null
                    },
                    holidayName       = { key -> com.mushotoku.app.holidays.HolidayNames.resolve(holidayLocalizedCtx, key) }
                )
            }
            if (showTrash) {
                TrashScreen(
                    notes            = deletedNotes,
                    onRestore        = { notesVm.restoreNote(it) },
                    onPermanentDelete = { notesVm.permanentlyDeleteNote(it) },
                    onDeleteAll      = { notesVm.permanentlyDeleteAllTrash() },
                    onClose          = { showTrash = false }
                )
            }
            if (showSettings) {
                SettingsScreen(
                    categories                 = categories,
                    settings                   = settings,
                    onClose                    = { showSettings = false },
                    onSetFinanceEnabled        = { settingsVm.setFinanceTabEnabled(it) },
                    onSetCategoryEnabled       = { cat, enabled -> financeVm.setCategoryEnabled(cat, enabled) },
                    onSetCategoryRecurringCost = { cat, cost -> financeVm.setCategoryRecurringCost(cat, cost) },
                    onAddCategory              = { name, group -> financeVm.addCustomCategory(name, group) },
                    onDeleteCategory           = { financeVm.deleteCategory(it) },
                    onResetCategories          = { financeVm.resetCategoriesToDefault() },
                    onDeleteFinanceData      = { financeVm.deleteAllExpenses() },
                    onDeleteAllTasks         = { tasksVm.deleteAllTasks() },
                    onDeleteAllAppointments  = { tasksVm.deleteAllAppointments() },
                    onDeleteAllHabits        = { habitsVm.deleteAllHabits() },
                    onDeleteAllNotes         = { notesVm.deleteAllNotes() },
                    onDeleteAllMindfulness   = { meditationVm.deleteAllMindfulnessData() },
                    onSetThemeMode      = { settingsVm.setThemeMode(it) },
                    onSetFontScale      = { settingsVm.setFontScale(it) },
                    onSetLanguage       = { settingsVm.setLanguage(it) },
                    onSetSalary         = { settingsVm.setSalary(it) },
                    onSetSalaryDay      = { settingsVm.setSalaryDay(it) },
                    onSetConfirmDelete  = { settingsVm.setConfirmDeleteEnabled(it) },
                    onSetHaptic         = { settingsVm.setHapticFeedbackEnabled(it) },
                    onSetNewNoteStartsWithTitle = { settingsVm.setNewNoteStartsWithTitle(it) },
                    onSetCurrency       = { settingsVm.setCurrency(it) },
                    onSetAppLockTimeout = { settingsVm.setAppLockTimeout(it) },
                    onSetBlockScreenshots = { settingsVm.setBlockScreenshots(it) },
                    onSetNotificationsEnabled = { settingsVm.setNotificationsEnabled(it) },
                    onSetNotificationLead     = { settingsVm.setNotificationLeadMinutes(it) },
                    onSetShowHolidays         = { settingsVm.setShowHolidays(it) },
                    onSetHolidayCountry       = { settingsVm.setHolidayCountry(it) },
                    onSetHolidayRegion        = { settingsVm.setHolidayRegion(it) },
                    onSetIncludeHolidaysInExport = { settingsVm.setIncludeHolidaysInExport(it) },
                    notificationsPermissionGranted = notificationsAllowed,
                    initialSection      = settingsInitialSection
                )
            }

            if (showAddTask) {
                AddTaskDialog(
                    notes         = notes,
                    linkedNoteIds = linkedNoteToTaskMap.keys,
                    onConfirm  = { title, isAppointment, time, linkedNoteId, newNoteTitle ->
                        if (isAppointment && newNoteTitle != null) {
                            notesVm.addNoteForLinking(newNoteTitle, NoteType.NOTE) { noteId ->
                                tasksVm.addTask(title, isAppointment = true, time = time, linkedNoteId = noteId)
                            }
                        } else {
                            tasksVm.addTask(title, isAppointment, time, linkedNoteId = if (isAppointment) linkedNoteId else null)
                        }
                    },
                    onAddHabit = { name, recurrence -> habitsVm.addHabit(name, recurrence) },
                    onDismiss  = { showAddTask = false }
                )
            }

            GlassOverlayHost(overlayHost)
        }
    }
}
