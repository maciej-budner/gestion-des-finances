package fr.ceri.gestionfinance

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

// Cette classe fait le lien entre tes 4 fichiers Fragment et le ViewPager
class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // On définit le nombre d'onglets (4 dans ton cas)
    override fun getItemCount(): Int = 4

    // Contrairement au tuto qui renvoie toujours le même fragment,
    // ici on renvoie un fragment spécifique selon la position (0 à 3)
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BlankFragmentRevenueContinue() // Ta page avec le graphique
            1 -> BlankFragmentRevenueNonContinue() // Ta page futures dépenses
            2 -> BlankFragmentDepenseContinue() // Ta page futurs gains
            3 -> BlankFragmentDepenseContinue() // Ta page prévisions
            else -> BlankFragmentRevenueContinue()
        }
    }
}