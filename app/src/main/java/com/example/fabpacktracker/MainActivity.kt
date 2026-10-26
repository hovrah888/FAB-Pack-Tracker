package com.example.fabpacktracker

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fabpacktracker.data.local.AppDatabase
import com.example.fabpacktracker.data.local.AssetPack
import com.example.fabpacktracker.data.remote.ChatRequest
import com.example.fabpacktracker.data.remote.DESCRIPTION
import com.example.fabpacktracker.data.remote.GROQ_PROMPT
import com.example.fabpacktracker.data.remote.GroqApi
import com.example.fabpacktracker.data.remote.GroqContent
import com.example.fabpacktracker.data.remote.GroqMessage
import com.example.fabpacktracker.data.remote.ResponseFormat
import com.example.fabpacktracker.data.remote.RetrofitInstance
import com.example.fabpacktracker.data.repository.GroqRepository
import com.example.fabpacktracker.navigation.AppNavigation
import com.example.fabpacktracker.ui.screens.AddEditPackScreen
import com.example.fabpacktracker.ui.screens.PackListScreen
import com.example.fabpacktracker.ui.screens.PackStatsScreen
import com.example.fabpacktracker.ui.theme.FABPackTrackerTheme
import com.example.fabpacktracker.viewmodel.PackListViewModel
import com.example.fabpacktracker.viewmodel.PackListViewModelFactory
import com.example.fabpacktracker.viewmodel.PackStatsViewModelFactory
import retrofit2.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val database = AppDatabase.getDatabase(context)
            val dao = database.assetPackDao()
            val groqRepository = GroqRepository(RetrofitInstance.api, RetrofitInstance.json)

            FABPackTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(dao = dao, groqRepository = groqRepository, modifier = Modifier.padding(innerPadding))
                    //PackStatsScreen(viewmodel = viewModel(factory = PackStatsViewModelFactory(dao)), modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
    Button(onClick = {}, enabled = false) { }
}


suspend fun urlTest(url: String) = withContext(Dispatchers.IO) {
    try {
        val doc =
            Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .timeout(5000)
                .execute()
        Log.d("OgTest", "${doc.statusCode()}")
        Log.d("OgTest", "${doc.headers()}")
        Log.d("OgTest", "${doc.body()}")
    } catch (e: Exception) {
        Log.e("OgTest", "failed: ${e::class.simpleName}: ${e.message}", e)
    }
}




@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FABPackTrackerTheme {
        Greeting("Android")
    }
}