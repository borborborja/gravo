package com.gravo.grabadora.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.gravo.grabadora.AppContainer
import com.gravo.grabadora.ui.detail.DetailScreen
import com.gravo.grabadora.ui.detail.DetailViewModel
import com.gravo.grabadora.ui.editor.EditorScreen
import com.gravo.grabadora.ui.editor.EditorViewModel
import com.gravo.grabadora.ui.home.HomeScreen
import com.gravo.grabadora.ui.home.HomeViewModel
import com.gravo.grabadora.ui.library.LibraryScreen
import com.gravo.grabadora.ui.library.LibraryViewModel
import com.gravo.grabadora.ui.settings.SettingsScreen
import com.gravo.grabadora.ui.settings.SettingsViewModel
import com.gravo.grabadora.ui.transcription.TranscriptionScreen
import com.gravo.grabadora.ui.transcription.TranscriptionViewModel

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    const val EDITOR = "editor/{id}"
    const val TRANSCRIPTION = "transcription/{id}"

    fun detail(id: Long) = "detail/$id"
    fun editor(id: Long) = "editor/$id"
    fun transcription(id: Long) = "transcription/$id"
}

@Composable
fun AppNavHost(navController: NavHostController, container: AppContainer) {
    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
            HomeScreen(
                viewModel = vm,
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onRecordingSaved = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.LIBRARY) {
            val vm: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))
            LibraryScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenRecording = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
            SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: DetailViewModel = viewModel(key = "detail_$id", factory = DetailViewModel.factory(container, id))
            DetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenEditor = { navController.navigate(Routes.editor(it)) },
                onOpenTranscription = { navController.navigate(Routes.transcription(it)) },
            )
        }
        composable(Routes.EDITOR, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: EditorViewModel = viewModel(key = "editor_$id", factory = EditorViewModel.factory(container, id))
            EditorScreen(viewModel = vm, onClose = { navController.popBackStack() })
        }
        composable(Routes.TRANSCRIPTION, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: TranscriptionViewModel = viewModel(key = "transcription_$id", factory = TranscriptionViewModel.factory(container, id))
            val context = LocalContext.current
            TranscriptionScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onShareText = { text ->
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            },
                            null,
                        ),
                    )
                },
            )
        }
    }
}
