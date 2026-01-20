package fr.ceri.gestionfinance

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
//excel
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BlankFragmentRevenueNonContinue.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlankFragmentRevenueNonContinue : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var workingFile: java.io.File
    private lateinit var textViewDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_blank_revenue_non_continue, container, false)
        // 2. On récupère la référence du TextView
        textViewDisplay = view.findViewById(R.id.textViewExcelData)
        return view
    }
    fun readExecelFile() {
        if (!workingFile.exists()) return
        try {
            val fileStream = java.io.FileInputStream(workingFile)
            val workbook = XSSFWorkbook(fileStream)
            val sheet = workbook.getSheetAt(4) // On prend la 1ère feuille
            val sdf = SimpleDateFormat("M/yyyy", Locale.getDefault())
            val dateActuelle = Date()
            val resulta = StringBuilder() // Pour accumuler le texte

            // DataFormatter transforme n'importe quel type (Numeric, String, Formula)
            // en texte sans jamais crasher
            val formatter = org.apache.poi.ss.usermodel.DataFormatter()

            for (row in sheet) {
                if (row.rowNum == 0) continue
                val date_debut = row.getCell(0) ?: continue
                val date_fin = row.getCell(1)?: continue
                val cellM = row.getCell(2) // Assure-toi que c'est bien l'index 2 pour le montant

                // --- 1. LECTURE DE LA DATE (SÉCURISÉE) ---
                var dateBegin: Date? = null
                var dateEnd: Date? = null

                when (date_debut.cellType) {
                    CellType.NUMERIC -> {
                        if (DateUtil.isCellDateFormatted(date_debut)) {
                            dateBegin = date_debut.dateCellValue
                        }
                    }
                    CellType.STRING -> {
                        try {
                            // On tente de parser le texte si c'est une string
                            dateBegin = sdf.parse(date_debut.stringCellValue)
                        } catch (e: Exception) {
                            dateBegin = null
                        }
                    }
                    else -> { /* Gérer les autres types si besoin */ }
                }
                when (date_fin.cellType) {
                    CellType.NUMERIC -> {
                        if (DateUtil.isCellDateFormatted(date_fin)) {
                            dateEnd = date_fin.dateCellValue
                        }
                    }
                    CellType.STRING -> {
                        try {
                            // On tente de parser le texte si c'est une string
                            dateEnd = sdf.parse(date_fin.stringCellValue)
                        } catch (e: Exception) {
                            dateEnd = null
                        }
                    }
                    else -> { /* Gérer les autres types si besoin */ }
                }

                // --- 2. LECTURE DU MONTANT ---
                // Au lieu de formatter.formatCellValue, on va être plus direct
                val montantDouble = when (cellM?.cellType) {
                    CellType.NUMERIC -> cellM.numericCellValue
                    CellType.STRING -> cellM.stringCellValue.replace(",", ".").toDoubleOrNull() ?: 0.0
                    CellType.FORMULA -> {
                        // Si c'est une formule, on essaie de récupérer le résultat numérique
                        try { cellM.numericCellValue } catch (e: Exception) { 0.0 }
                    }
                    else -> 0.0
                }

                // 3. Lecture Nom et Description (Index 2 et 3)
                val nom = formatter.formatCellValue(row.getCell(3))
                val description = formatter.formatCellValue(row.getCell(4))


                if(dateBegin != null && dateEnd != null){
                    if(dateActuelle.after(dateEnd)){
                        // suprimmer donner
                        sheet.removeRow(row)
                    }
                    else if(dateActuelle.after(dateBegin) && dateActuelle.before(dateEnd)){
                        // Ici tu peux envoyer 'montant' vers ton graphique !
                        resulta.append("date fin:${sdf.format(dateEnd)} | $nom | $montantDouble €\n")
                    }
                    else{continue}
                }

            }

            // 3. ON AFFICHE DANS LE TEXTVIEW
            textViewDisplay.text = if (resulta.isEmpty()) "Aucune donnée trouvée" else resulta.toString()

            workbook.close()
            fileStream.close()
        } catch (e: Exception) {
            textViewDisplay.text = "Erreur : ${e.message}"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Préparer le fichier (Copie des assets vers le stockage privé)
        prepareWorkingFile()
        //supprime les donné dépaser
        supDonnerFini()
        // 2. Lire les données
        readExecelFile()
    }
    private fun supDonnerFini(){
        if (!workingFile.exists()) return
        try {
            val fileStream = java.io.FileInputStream(workingFile)
            val workbook = XSSFWorkbook(fileStream)
            val sheet = workbook.getSheetAt(4) // On prend la 5ère feuille
            val date = SimpleDateFormat("M/yyyy")
            val dateActuelle = Date()

            for (row in sheet) {
                if (row.rowNum == 0) continue
                val date_fin = row.getCell(1)?: continue

                // 2. Récupérer la date correctement peu importe le format Excel
                val dateEnd: Date? = if (DateUtil.isCellDateFormatted(date_fin)) {
                    date_fin.dateCellValue
                } else {
                    // Si c'est du texte "01/2026", on le transforme en Date
                    try { date.parse(date_fin.toString()) } catch (e: Exception) { null }
                }
                if(dateEnd != null){
                    if(dateActuelle.after(dateEnd)){
                        // suprimmer donner
                        sheet.removeRow(row)
                    }}

            }
            workbook.close()
            fileStream.close()
        } catch (e: Exception) {
            textViewDisplay.text = "Erreur : ${e.message}"
        }
    }
    private fun prepareWorkingFile() {
        workingFile = java.io.File(requireContext().filesDir, "finance_work.xlsx")

        if (!workingFile.exists()) {
            try {
                requireContext().assets.open("finance.xlsx").use { input ->
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BlankFragmentRevenueNonContinue.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BlankFragmentRevenueNonContinue().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}