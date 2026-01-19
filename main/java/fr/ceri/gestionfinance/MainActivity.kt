package fr.ceri.gestionfinance

import android.content.ContentValues.TAG
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

import java.util.Date;
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import fr.ceri.gestionfinance.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import org.apache.poi.ss.usermodel.DateUtil
import java.text.SimpleDateFormat
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import kotlin.math.log
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding
    private var depenceValue:Double = 0.0
    private var gainValue:Double = 0.0
    private var eparne:Double = 0.0
    private lateinit var workingFile : java.io.File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(ui.root)
        prepareWorkingFile()
        readExecelFile()
        setData()
        //tabLayoute
        val adapter = ViewPagerAdapter(this)
        ui.page.adapter = adapter

        // Pour lier les onglets et le ViewPager
        TabLayoutMediator(ui.tabLayout, ui.page) { tab, position ->
            tab.text = when (position) {
                0 -> "Revenue continue"
                1 -> "Revenue courante"
                2 -> "Dépense continue"
                3 -> "Dépense courante"
                else -> null
            }
        }.attach()

        val date = SimpleDateFormat("M/yyyy")
        val dateActuelle = date.format(Date())
        ui.date.text = dateActuelle.toString()


        // Masquer la barre de statut (status bar) et la barre de navigation
        val decorView = window.decorView
        val uiOptions =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions
    }

    private fun prepareWorkingFile() {
        workingFile = java.io.File(this.filesDir, "finance_work.xlsx")

        if (!workingFile.exists()) {
            try {
                this.assets.open("finance.xlsx").use { input ->
                    workingFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.d("EXCEL", "Fichier copié dans le stockage privé")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun readExecelFile() {
        if (!workingFile.exists()) return

        // Reset
        depenceValue = 0.0
        gainValue = 0.0
        var eparneValue = 0.0

        try {
            val fileStream = java.io.FileInputStream(workingFile)
            val workbook = XSSFWorkbook(fileStream)
            val evaluator = workbook.creationHelper.createFormulaEvaluator()

            // Recalcul de toutes les formules (SOMME.SI.ENS, etc.)
            evaluator.evaluateAll()

            // Fonction pour lire UNE cellule de résultat sans faire de boucle for
            fun getResult(sheetIndex: Int, colIndex: Int): Double {
                val sheet = workbook.getSheetAt(sheetIndex)
                val row = sheet.getRow(0) // On vise la ligne 1
                val cell = row?.getCell(colIndex)

                return if (cell != null) {
                    // IMPORTANT: on utilise l'évaluateur pour avoir le résultat de la formule
                    evaluator.evaluate(cell).numberValue
                } else 0.0
            }

            // Lecture des totaux calculés par Excel
            eparneValue = getResult(0, 3)   // Feuille 1, Col D
            depenceValue += getResult(1, 4) // Feuille 2, Col E
            depenceValue += getResult(2, 5) // Feuille 3, Col F
            gainValue += getResult(3, 4)    // Feuille 4, Col E
            gainValue += getResult(4, 5)    // Feuille 5, Col F
            Log.e("ExcelErreur", "Erreur : Eco ${getResult(0, 3)}, dep1 ${getResult(1, 4)}, dep2 ${getResult(2, 5)},gain1 ${getResult(3, 4) },gain2 ${getResult(4, 5)},")
            // Calculs Kotlin
            val restValue = gainValue - depenceValue
            var eco = 0.0
            if(restValue >=0){
                eco = restValue * eparneValue / 100.0
            }

            val finalRest = restValue - eco

            // Mise à jour de l'interface (UI)
            ui.pourcentEparne.text = "$eparneValue"
            ui.coter.text = String.format("%.2f", eco)
            ui.rest.text = String.format("%.2f", finalRest)
            Log.e("ExcelErreur", "Erreur : Eco ${eco}, rest ${finalRest}, rest2 ${restValue},gain ${gainValue},depence ${depenceValue},")
            workbook.close()
            fileStream.close()

        } catch (e: Exception) {
            Log.e("ExcelErreur", "Erreur : ${e.message}")
        }
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
