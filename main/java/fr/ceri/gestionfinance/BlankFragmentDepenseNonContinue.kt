package fr.ceri.gestionfinance

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.apache.poi.ss.usermodel.DateUtil
//excel
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BlankFragmentDepenseNonContinue.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlankFragmentDepenseNonContinue : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_blank_depense_non_continue, container, false)
        // 2. On récupère la référence du TextView
        textViewDisplay = view.findViewById(R.id.textViewExcelData)
        return view
    }

    fun readExecelFile() {
        if (!workingFile.exists()) return
        try {
            val fileStream = java.io.FileInputStream(workingFile)
            val workbook = XSSFWorkbook(fileStream)
            val sheet = workbook.getSheetAt(2) // On prend la 1ère feuille
            val date = SimpleDateFormat("M/yyyy")
            val dateActuelle = Date()
            val resulta = StringBuilder() // Pour accumuler le texte

            for (row in sheet) {
                if (row.rowNum == 0) continue
                val date_debut = row.getCell(0) ?: continue
                val date_fin = row.getCell(1)?: continue
                val cellMontant = row.getCell(2) ?: continue
                // 1. Gestion du Montant (Sécurisée)
                val montant = when (cellMontant.cellType) {
                    org.apache.poi.ss.usermodel.CellType.NUMERIC -> cellMontant.numericCellValue.toString()
                    org.apache.poi.ss.usermodel.CellType.STRING -> cellMontant.stringCellValue
                    else -> "0.0"
                }
                val description = row.getCell(3).toString()

                // 2. Récupérer la date correctement peu importe le format Excel
                val dateBegin: Date? = if (DateUtil.isCellDateFormatted(date_debut)) {
                    date_debut.dateCellValue
                } else {
                    // Si c'est du texte "01/2026", on le transforme en Date
                    try { date.parse(date_debut.toString()) } catch (e: Exception) { null }
                }
                val dateEnd: Date? = if (DateUtil.isCellDateFormatted(date_fin)) {
                    date_fin.dateCellValue
                } else {
                    // Si c'est du texte "01/2026", on le transforme en Date
                    try { date.parse(date_fin.toString()) } catch (e: Exception) { null }
                }
                if(dateBegin != null && dateEnd != null){
                    if(dateActuelle.after(dateBegin) && dateActuelle.before(dateEnd)){
                        // Ici tu peux envoyer 'montant' vers ton graphique !
                        resulta.append("date fin dépense: $date_fin -> $description -> $montant \n")
                    }}

            }
            // 3. ON AFFICHE DANS LE TEXTVIEW
            textViewDisplay.text = if (resulta.isEmpty()) "Fichier vide" else resulta.toString()
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

        // 2. Lire les données
        readExecelFile()
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
         * @return A new instance of fragment BlankFragmentDepenseNonContinue.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BlankFragmentDepenseNonContinue().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}