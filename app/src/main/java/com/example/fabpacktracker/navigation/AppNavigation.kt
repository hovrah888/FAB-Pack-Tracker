package com.example.fabpacktracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fabpacktracker.data.local.AssetPackDao
import com.example.fabpacktracker.data.repository.GroqRepository
import com.example.fabpacktracker.ui.screens.AddEditPackScreen
import com.example.fabpacktracker.ui.screens.PackListScreen
import com.example.fabpacktracker.viewmodel.AddEditPackViewModel
import com.example.fabpacktracker.viewmodel.AddEditPackViewModelFactory
import com.example.fabpacktracker.viewmodel.NEW_PACK_ID
import com.example.fabpacktracker.viewmodel.PackListViewModel
import com.example.fabpacktracker.viewmodel.PackListViewModelFactory

const val PACK_LIST_SCREEN = "pack_list_screen"
const val CREATE_EDIT_PACK_SCREEN = "create_edit_pack_screen"

@Composable
fun AppNavigation (dao : AssetPackDao, groqRepository: GroqRepository, modifier: Modifier = Modifier){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = PACK_LIST_SCREEN, modifier = modifier){
        composable(PACK_LIST_SCREEN){
            val viewModel: PackListViewModel = viewModel(factory = PackListViewModelFactory(dao))
            val packs by viewModel.packs.collectAsStateWithLifecycle()
            PackListScreen(
                packs = packs,
                onAddClick = {navController.navigate("$CREATE_EDIT_PACK_SCREEN/$NEW_PACK_ID")},
                onPackClick = { packId -> navController.navigate("$CREATE_EDIT_PACK_SCREEN/$packId")},
                onDeletePack = viewModel::deletePack
            )

        }
        composable("$CREATE_EDIT_PACK_SCREEN/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: NEW_PACK_ID
            val viewModel: AddEditPackViewModel = viewModel(factory = AddEditPackViewModelFactory(dao, id, groqRepository))
            AddEditPackScreen(viewModel = viewModel, onSaved = {navController.popBackStack()})

        }
    }
}