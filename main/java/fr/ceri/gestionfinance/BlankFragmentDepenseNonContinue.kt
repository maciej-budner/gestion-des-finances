package fr.ceri.gestionfinance

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
//excel
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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

        return view
    }

    fun readExecelFile() {
        if (!workingFile.exists()) return
        try {
            val operations = mutableListOf<Operation>()
            val fileStream = java.io.FileInputStream(workingFile)
            val workbook = XSSFWorkbook(fileStream)
            val sheet = workbook.getSheetAt(2) // On prend la 1ère feuille
            val sdf = SimpleDateFormat("M/yyyy", Locale.getDefault())
            val dateActuelle = Date()

            // DataFormatter transforme n'importe quel type (Numeric, String, Formula)
            // en texte sans jamais crasher
            val formatter = org.apache.poi.ss.usermodel.DataFormatter()
            Log.e("ExcelSave", "lit non depence courant")
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


                Log.e("ExcelSave", "date fin:${sdf.format(dateEnd)} | $nom | $montantDouble €")
                if(dateBegin != null && dateEnd != null){
                    if(dateActuelle.after(dateEnd)){
                        // suprimmer donner
                        // On parcourt toutes les cellules de la ligne
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
                    }
                    else if(dateActuelle.after(dateBegin) && dateActuelle.before(dateEnd)){
                        // Ici tu peux envoyer 'montant' vers ton graphique !
                        operations.add(Operation("fin:${sdf.format(dateEnd)} | $nom | $montantDouble", row.rowNum))
                    }
                    else{continue}
                }

            }
            // Configurer la RecyclerView
            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewData)
            recyclerView?.layoutManager = LinearLayoutManager(context)
            recyclerView?.adapter = OperationAdapter(operations) { operation ->
                confirmDelete(operation, 2)
            }

            workbook.close()
            fileStream.close()
        } catch (e: Exception) {
            Log.e("Erreur","Erreur : ${e.message}")
        }
    }
    private fun confirmDelete(op: Operation, sheetIdx: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Supprimer")
            .setMessage("Voulez-vous supprimer ${op.txt} ?")
            .setPositiveButton("Oui") { _, _ ->
                (activity as MainActivity).deleteExcelRow(sheetIdx, op.index)
                readExecelFile() // Recharge la liste
            }
            .setNegativeButton("Non", null).show()
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