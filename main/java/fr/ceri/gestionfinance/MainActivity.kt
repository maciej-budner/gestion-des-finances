package fr.ceri.gestionfinance

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

import java.util.Date;
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import fr.ceri.gestionfinance.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import org.apache.poi.ss.usermodel.Sheet
import java.text.SimpleDateFormat
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Thread.sleep
import java.util.Calendar
import java.util.Locale

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

        //tabLayoute
        chargeGrapheTable()

        readExecelFile()
        setData()
        val date = SimpleDateFormat("M/yyyy")
        val dateActuelle = date.format(Date())
        ui.date.text = dateActuelle.toString()

        // Masquer la barre de statut (status bar) et la barre de navigation
        val decorView = window.decorView
        val uiOptions =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions

        //ecouteur
        ui.add.setOnClickListener(this::addColoneInExcel)
        ui.addEparne.setOnClickListener(this::addColoneinEparneExcel)
    }

    fun chargeGrapheTable(){
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

    }

    fun addColoneInExcel(view: View?) {
        // 1. Charger le layout XML du formulaire
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.layout_dialog_add, null)

        // 2. Créer la fenêtre surgissante (AlertDialog)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Empêche de fermer en cliquant à côté
            .create()

        // Récupérer les composants du formulaire
        val btnValider = dialogView.findViewById<Button>(R.id.btnValider)
        val editNom = dialogView.findViewById<EditText>(R.id.editNom)
        val editDescription = dialogView.findViewById<EditText>(R.id.editDescription)
        val editDateDebut = dialogView.findViewById<EditText>(R.id.editDateDebut)
        val editDateFin  = dialogView.findViewById<EditText>(R.id.editDateFin)
        val editMontant = dialogView.findViewById<EditText>(R.id.editMontant)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupType)

        // 3. Ajouter les boutons de la fenêtre
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Valider") { _, _ ->

        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Annuler") { d, _ ->
            d.dismiss() // Ferme la fenêtre
        }
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioDepCour || checkedId == R.id.radioGainCour) {
                editDateFin.visibility = View.VISIBLE
            } else {
                editDateFin.visibility = View.GONE
            }
        }
        dialog.show()
        val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        btnPositive.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val nom = editNom.text.toString().trim()
            val dateDeb = editDateDebut.text.toString().trim()
            val dateFin = editDateFin.text.toString().trim()
            val montantStr = editMontant.text.toString().trim()

            // Regex pour vérifier le format M/yyyy (ex: 1/2026 ou 12/2026)
            val dateRegex = Regex("""^(0?[1-9]|1[0-2])/\d{4}$""")

            var isValid = true

            // 1. Vérification de la feuille
            if (selectedId == -1) {
                Toast.makeText(this, "Choisis un type d'opération", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            // 2. Vérification du Nom
            if (nom.isEmpty()) {
                editNom.error = "Le nom est obligatoire"
                isValid = false
            }

            // 3. Vérification du Montant
            if (montantStr.isEmpty()) {
                editMontant.error = "Indique un montant"
                isValid = false
            }

            // 4. Vérification Date Début
            if (!dateDeb.matches(dateRegex)) {
                editDateDebut.error = "Format invalide (M/yyyy)"
                isValid = false
            }
            if ((montantStr.toDoubleOrNull() ?: 0.0) < 0) {
                editMontant.error = "Montant négatif, il doit être positif"
                isValid = false
            }

            // 5. Vérification Date Fin (uniquement pour Courant)
            val feuilleCible = when(selectedId) {
                R.id.radioGainCont -> 3
                R.id.radioDepCont -> 1
                R.id.radioDepCour -> 2
                R.id.radioGainCour -> 4
                else -> 0
            }

            if ((feuilleCible == 2 || feuilleCible == 4) && !dateFin.matches(dateRegex) && !dateFin.isEmpty()) {
                editDateFin.error = "Format invalide (M/yyyy)"
                isValid = false
            }

            // SI TOUT EST OK
            if (isValid) {
                val montantBrut = editMontant.text.toString().replace(",", ".") // Remplace virgule par point
                val montant = montantBrut.toDoubleOrNull() ?: 0.0
                saveToExcel(
                    feuilleCible,
                    nom,
                    montant,
                    editDescription.text.toString(),
                    dateDeb,
                    dateFin
                )
                dialog.dismiss() // On ferme seulement si c'est valide
            }
        }
    }

    fun getFirstEmptyRowIndex(sheet: Sheet): Int {
        // On commence à 0 (ou 1 si tu as des entêtes)
        var rowIndex = 1

        // On boucle tant que la ligne actuelle existe ET n'est pas vide
        while (sheet.getRow(rowIndex) != null &&
            sheet.getRow(rowIndex).getCell(0) != null && // On vérifie la colonne "Nom"
            sheet.getRow(rowIndex).getCell(0).toString().isNotEmpty()) {
            rowIndex++
        }

        return rowIndex
    }

    private fun saveToExcel(sheetIndex: Int, nom: String, montant: Double, description: String,dateDebut: String, dateFin: String) {
        try {
            val inputStream = FileInputStream(workingFile)
            val workbook = XSSFWorkbook(inputStream)
            inputStream.close()
            val sheet = workbook.getSheetAt(sheetIndex)

            // ON TROUVE LE PREMIER EMPLACEMENT VIDE
            val emptyRowIndex = getFirstEmptyRowIndex(sheet)

            // On crée la ligne (ou on la récupère si elle existait mais était vide)
            val row = sheet.getRow(emptyRowIndex) ?: sheet.createRow(emptyRowIndex)
            val nouvelleDateFin = if (dateFin.isEmpty()) {
                ajouterUnMois(dateDebut) // Cas : dateFin null/vide -> début + 1 mois
            } else {
                ajouterUnMois(dateFin)   // Cas : dateFin existe -> dateFin + 1 mois
            }
            // On remplit les données
            if(sheetIndex == 1 || sheetIndex == 3){
                //info sur date montant nom description
                row.createCell(0).setCellValue(dateDebut)
                val cellMontant = row.createCell(1)
                cellMontant.setCellValue(montant)
                row.createCell(2).setCellValue(nom)
                row.createCell(3).setCellValue(description)
            }
            else if(sheetIndex ==0){
                //info eparne
                row.createCell(0).setCellValue(dateDebut)
                val cellMontant = row.createCell(1)
                cellMontant.setCellValue(montant)
            }
            else{
                //indo sur date debut/fin montant nom description
                row.createCell(0).setCellValue(dateDebut)
                row.createCell(1).setCellValue(nouvelleDateFin)
                val cellMontant = row.createCell(2)
                cellMontant.setCellValue(montant)
                row.createCell(3).setCellValue(nom)
                row.createCell(4).setCellValue(description)
            }
            try {
                val evaluator = workbook.creationHelper.createFormulaEvaluator()
                // Au lieu de evaluateAll(), on ne fait rien ou on évalue juste la ligne
            } catch (e: Exception) {
                Log.e("ExcelSave", "Erreur evaluation ignoree")
            }
            // Sauvegarde
            val outputStream = FileOutputStream(workingFile)
            workbook.write(outputStream)
            outputStream.flush()
            outputStream.close()
            workbook.close()
            sleep(1000)
            readExecelFile()
            chargeGrapheTable()
            setData()
        } catch (e: Exception) {
            Log.e("ExcelSave", "Erreur : ${e.message}")
        }
    }
    private fun ajouterUnMois(dateStr: String): String {
        val sdf = SimpleDateFormat("M/yyyy", Locale.getDefault())
        return try {
            val date = sdf.parse(dateStr)
            val calendar = Calendar.getInstance()
            calendar.time = date!!
            calendar.add(Calendar.MONTH, 1) // Ajoute +1 au mois
            sdf.format(calendar.time)
        } catch (e: Exception) {
            dateStr // En cas d'erreur, on renvoie la date originale
        }
    }
    fun addColoneinEparneExcel(view: View?){
        // 1. Charger le layout XML du formulaire
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.layout_dialog_eparne, null)

        // 2. Créer la fenêtre surgissante (AlertDialog)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Empêche de fermer en cliquant à côté
            .create()

        // Récupérer les composants du formulaire
        val btnValider = dialogView.findViewById<Button>(R.id.btnValider)
        val date = SimpleDateFormat("M/yyyy")
        val editDateDebut = date.format(Date())
        val editMontant = dialogView.findViewById<EditText>(R.id.editMontant)

        // 3. Ajouter les boutons de la fenêtre
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Valider") { _, _ ->

        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Annuler") { d, _ ->
            d.dismiss() // Ferme la fenêtre
        }

        dialog.show()
        val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        btnPositive.setOnClickListener {
            val montantStr = editMontant.text.toString().trim()

            var isValid = true

            // 3. Vérification du Montant
            if (montantStr.isEmpty()) {
                editMontant.error = "Indique un Pourcentage"
                isValid = false
            }
            if ((montantStr.toDoubleOrNull() ?: 0.0) > 100 || (montantStr.toDoubleOrNull()
                    ?: 0.0) < 0
            ) {
                editMontant.error = "éparne entre 0 et 100"
                isValid = false
            }

            // SI TOUT EST OK
            if (isValid) {
                //pour eviter des erreur de sum sur excel
                val montantBrut = editMontant.text.toString().replace(",", ".") // Remplace virgule par point
                val montant = montantBrut.toDoubleOrNull() ?: 0.0
                saveToExcel(
                    0,
                    "",
                    montant,
                    "",
                    editDateDebut,
                    ""
                )
                dialog.dismiss() // On ferme seulement si c'est valide
            }
        }
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

    fun deleteExcelRow(sheetIndex: Int, rowNum: Int) {
        try {
            val inputStream = FileInputStream(workingFile)
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(sheetIndex)

            if (rowNum != null) {
                var row = sheet.getRow(rowNum)
                for (i in 0 until 6) {
                    // 6 correspond à la colonne G (A=0, B=1, C=2, D=3, E=4, F=5, G=6)
                    if (i == 6) {
                        continue // On ne touche pas à la colonne G, on passe à la suivante
                    }
                    val cell = row.getCell(i)
                    if (cell != null) {
                        row.removeCell(cell) // On supprime le contenu de la cellule
                    }
                }
                val outputStream = FileOutputStream(workingFile)
                workbook.write(outputStream)
                outputStream.close()
            }
            workbook.close()
            inputStream.close()
            sleep(1000)
            readExecelFile()
            chargeGrapheTable()
            setData()
        } catch (e: Exception) {
            Log.e("ExcelDelete", "Erreur : ${e.message}")
        }
    }
}
