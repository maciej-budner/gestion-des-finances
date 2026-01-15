package fr.ceri.gestionfinance

import android.graphics.Color
import android.os.Bundle
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import fr.ceri.gestionfinance.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding
    private var depenceValue =1000
    private var gainValue = 1500
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(ui.root)
        setData()
        // Masquer la barre de statut (status bar) et la barre de navigation
        val decorView = window.decorView
        val uiOptions =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions
    }
    private fun setData() {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(depenceValue.toFloat(), "Dépense"))
        entries.add(PieEntry(gainValue.toFloat(), "Gain"))

        val dataSet = PieDataSet(entries, "Budget")

        // Couleurs : tu peux utiliser tes couleurs ou un template tout prêt
        dataSet.colors = listOf(
            Color.parseColor("#FFA726"),
            Color.parseColor("#66BB6A")
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        ui.piechart.data = data

        // Quelques réglages pour le look
        ui.piechart.description.isEnabled = false
        ui.piechart.isDrawHoleEnabled = true // Pour faire un donut (plus moderne)
        ui.piechart.setHoleColor(Color.TRANSPARENT)

        ui.piechart.animateY(1400) // Animation fluide
        ui.piechart.invalidate() // Rafraîchir
    }
}
