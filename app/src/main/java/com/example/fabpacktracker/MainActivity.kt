package com.example.fabpacktracker


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.fabpacktracker.data.local.AppDatabase
import com.example.fabpacktracker.data.remote.RetrofitInstance
import com.example.fabpacktracker.data.repository.GroqRepository
import com.example.fabpacktracker.navigation.AppNavigation
import com.example.fabpacktracker.ui.theme.FABPackTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // создавать объекты в onCreate, чтобы не пересоздавалось лишний раз при рекомпозиции
        val context = applicationContext
        val database = AppDatabase.getDatabase(context)
        val dao = database.assetPackDao()
        val groqRepository = GroqRepository(RetrofitInstance.api, RetrofitInstance.json)

        setContent {


            FABPackTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(dao = dao, groqRepository = groqRepository, modifier = Modifier.padding(innerPadding))
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




@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FABPackTrackerTheme {
        Greeting("Android")
    }
}