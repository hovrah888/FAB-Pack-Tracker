package com.example.fabpacktracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fabpacktracker.data.local.AppDatabase
import com.example.fabpacktracker.data.local.AssetPackDao
import com.example.fabpacktracker.data.repository.GroqRepository
import com.example.fabpacktracker.ui.screens.AddEditPackScreen
import com.example.fabpacktracker.ui.screens.PackListScreen
import com.example.fabpacktracker.viewmodel.AddEditPackViewModel
import com.example.fabpacktracker.viewmodel.AddEditPackViewModelFactory
import com.example.fabpacktracker.viewmodel.NEW_PACK_ID
import com.example.fabpacktracker.viewmodel.PackListViewModel
import com.example.fabpacktracker.viewmodel.PackListViewModelFactory


@Composable
fun AppNavigation (dao : AssetPackDao, groqRepository: GroqRepository, modifier: Modifier = Modifier){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "pack_list_screen", modifier = modifier){
        composable("pack_list_screen"){
            val viewModel: PackListViewModel = viewModel(factory = PackListViewModelFactory(dao))
            val packs by viewModel.packs.collectAsState()
            PackListScreen(
                packs = packs,
                onAddClick = {navController.navigate("create_edit_pack_screen/$NEW_PACK_ID")},
                onPackClick = { packId -> navController.navigate("create_edit_pack_screen/$packId")},
                onDeletePack = viewModel::deletePack
            )

        }
        composable("create_edit_pack_screen/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: NEW_PACK_ID
            val viewModel: AddEditPackViewModel = viewModel(factory = AddEditPackViewModelFactory(dao, id, groqRepository))
            AddEditPackScreen(viewModel = viewModel, onSaved = {navController.popBackStack()})

        }
    }
}