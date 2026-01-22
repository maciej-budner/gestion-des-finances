// Initialisation du client
const _supabase = supabase.createClient(
    'https://foqtdzhwxxozxdgpbwdu.supabase.co', 
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZvcXRkemh3eHhvenhkZ3Bid2R1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkxMDc1MTQsImV4cCI6MjA4NDY4MzUxNH0.xmULuB0R7Hm8R417GdK1cLJSwwTFKk3PvHlj1DNQWTw'
);

let financeChart;

// --- NAVIGATION & UI ---
window.openAddModal = () => document.getElementById('modalAdd').classList.remove('hide');
window.closeModal = () => document.getElementById('modalAdd').classList.add('hide');
window.toggleDateFields = (show) => {
    document.getElementById('date-group').classList.toggle('hide', !show);
};

// --- INITIALISATION ---
async function initDashboard() {
    const { data: { user } } = await _supabase.auth.getUser();
    if (!user) {
        window.location.href = '../index.html';
        return;
    }
    await loadData();
}

// --- CHARGEMENT & CALCULS ---
// --- CHARGEMENT & CALCULS ---
async function loadData() {

    const { data: { user } } = await _supabase.auth.getUser();
    const today = new Date().toISOString().split('T')[0];
    // NETTOYAGE AUTOMATIQUE : Supprime les courants périmés
    await Promise.all([
        _supabase.from('gain_courant').delete().lt('date_fin', today).eq('id', user.id),
        _supabase.from('depence_courant').delete().lt('date_fin', today).eq('id', user.id)
    ]);
    // Récupération simultanée
    const [gCont, gCour, dCont, dCour, ep] = await Promise.all([
        _supabase.from('gain_continue').select('*').eq('id', user.id),
        _supabase.from('gain_courant').select('*').eq('id', user.id),
        _supabase.from('depence_continue').select('*').eq('id', user.id),
        _supabase.from('depence_courant').select('*').eq('id', user.id),
        _supabase.from('eparne').select('*').eq('id', user.id).maybeSingle() 
    ]);

    // 1. Récupérer le pourcentage d'épargne (On utilise une seule variable propre)
    const currentPourcent = ep.data ? ep.data.eparne : 0;

    // 2. Calcul Gains (Fixes + Courants si dans les dates)
    const activeGainsCourants = gCour.data?.filter(i => i.date_debut <= today && (!i.date_fin || i.date_fin >= today)) || [];
    const totalGains = (gCont.data?.reduce((acc, c) => acc + c.gain, 0) || 0) +
                       (activeGainsCourants.reduce((acc, c) => acc + c.gain, 0) || 0);

    // 3. Calcul Dépenses (Fixes + Courantes si dans les dates)
    const activeDepCourantes = dCour.data?.filter(i => i.date_debut <= today && (!i.date_fin || i.date_fin >= today)) || [];
    const totalDepenses = (dCont.data?.reduce((acc, c) => acc + c.depence, 0) || 0) +
                          (activeDepCourantes.reduce((acc, c) => acc + c.depence, 0) || 0);

    // 4. Calculs finaux
    const montantEparne = (totalGains * currentPourcent) / 100;
    const reste = totalGains - totalDepenses - montantEparne;

    // On filtre les listes pour ne passer à l'affichage QUE les éléments valides aujourd'hui
    const filteredGainsCourants = gCour.data?.filter(i => !i.date_fin || i.date_fin >= today) || [];
    const filteredDepCourantes = dCour.data?.filter(i => !i.date_fin || i.date_fin >= today) || [];
    // 5. Mise à jour UI Textuelle (Vérifie bien que ces IDs existent dans ton HTML)
    const elEparnePct = document.getElementById('val-eparne');
    const elReste = document.getElementById('val-reste');
    const elTotalDep = document.getElementById('val-total-dep');
    const elMontantEparne = document.getElementById('val-montant-eparne');

    if(elEparnePct) elEparnePct.textContent = currentPourcent + "%";
    if(elReste) elReste.textContent = reste.toFixed(2) + "€";
    if(elTotalDep) elTotalDep.textContent = totalDepenses.toFixed(2) + "€";
    if(elMontantEparne) elMontantEparne.textContent = montantEparne.toFixed(2) + "€";
    
    // 6. Mise à jour Graphique
    updateDonut(totalDepenses, montantEparne, reste);

    // 7. Mise à jour de la liste (CORRIGÉ ICI)
    // On envoie uniquement les éléments filtrés (valides) à l'affichage
    const finalGains = [...gCont.data || [], ...filteredGainsCourants];
    const finalDepenses = [...dCont.data || [], ...filteredDepCourantes];
    
    renderList(finalGains, finalDepenses);
}

// --- AFFICHAGE DE LA LISTE ---
// Cette fonction est appelée par loadData
function renderList(gains, depenses) {
    currentTabData = { gains, depenses }; // On sauvegarde les données reçues
    // On affiche par défaut le premier onglet
    renderTabContent('gain_continue');
}

function renderTabContent(category) {
    const container = document.getElementById('list-container-tabs');
    container.innerHTML = '';

    // Déterminer quelles données afficher
    let list = [];
    let isGain = category.includes('gain');
    
    if (isGain) {
        list = currentTabData.gains.filter(item => 
            category === 'gain_continue' ? !item.date_debut : item.date_debut
        );
    } else {
        list = currentTabData.depenses.filter(item => 
            category === 'depence_continue' ? !item.date_debut : item.date_debut
        );
    }

    list.forEach(item => {
        const id_db = item.id_gain_continue || item.id_gain_courant || item.id_depence_continue || item.id_depence_courant;
        const val = item.gain || item.depence;
        
        // Gestion de la date à afficher
        let dateInfo = "";
        if (category.includes('continue')) {
            // Pour les fixes : on affiche le mois/année de création (ex: 01/2026)
            const d = new Date(item.date);
            dateInfo = `${d.getMonth() + 1}/${d.getFullYear()}`;
        } else {
            // Pour les courants : on affiche la date de fin
            dateInfo = `Jusqu'au ${item.date_fin}`;
        }

        const div = document.createElement('div');
        div.className = 'card list-item';
        div.style = 'display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; padding:15px;';
        div.innerHTML = `
            <div>
                <div style="font-size: 0.9em; color: var(--gray);">${dateInfo} | ${item.nom}</div>
                <strong style="color: ${isGain ? '#2dd4bf' : '#ff453a'}; font-size: 1.2em;">
                    ${isGain ? '+' : '-'}${val}€
                </strong>
            </div>
            <button onclick="deleteEntry('${category}', ${id_db})" 
                    style="width:40px; height:40px; background:#ff453a; border-radius:50%; color:white; border:none; display:flex; align-items:center; justify-content:center;">
                🗑️
            </button>
        `;
        container.appendChild(div);
    });
}

// --- SAUVEGARDE NOUVELLE OPÉRATION ---
window.saveOperation = async () => {
    const type = document.querySelector('input[name="op-type"]:checked').value;
    const nom = document.getElementById('op-nom').value;
    const montant = parseFloat(document.getElementById('op-montant').value);
    const { data: { user } } = await _supabase.auth.getUser();

    if (!nom || isNaN(montant)) return alert("Veuillez remplir le nom et le montant.");

    let payload = { id: user.id, nom: nom, [type.includes('gain') ? 'gain' : 'depence']: montant };

    if (type.includes('courant')) {
        const dDebut = document.getElementById('op-debut').value;
        let dFin = document.getElementById('op-fin').value;
        if (!dDebut) return alert("Date de début requise");

        if (!dFin) { 
            // Au lieu de faire +1 mois jour pour jour, 
            // on met la date au dernier jour du mois en cours
            let date = new Date();
            let dernierJour = new Date(date.getFullYear(), date.getMonth() + 1, 0); 
            dFin = dernierJour.toISOString().split('T')[0];
        }
        payload.date_debut = dDebut;
        payload.date_fin = dFin;
    } else {
        payload.date = new Date().toISOString().split('T')[0];
    }

    const { error } = await _supabase.from(type).insert([payload]);
    if (error) alert(error.message);
    else { closeModal(); loadData(); }
};

// --- AUTRES FONCTIONS (Graph, Delete, Eparne) ---
function updateDonut(dep, epa, res) {
    const ctx = document.getElementById('financeChart').getContext('2d');
    if (financeChart) financeChart.destroy();
    financeChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Dépenses', 'Épargne', 'Reste'],
            datasets: [{
                data: [dep, epa, res < 0 ? 0 : res],
                backgroundColor: ['#ff453a', '#007aff', '#2dd4bf'],
                borderWidth: 0
            }]
        },
        options: { cutout: '75%', plugins: { legend: { display: false } } }
    });
}

async function deleteEntry(tableName, idValue) {
    if (!confirm("Supprimer cette opération ?")) return;
    const { error } = await _supabase.from(tableName).delete().eq(`id_${tableName}`, idValue);
    if (error) alert(error.message); else loadData();
}

window.addEparne = async () => {
    const { data: { user } } = await _supabase.auth.getUser();
    const val = prompt("Pourcentage d'épargne souhaité (0-100) :");
    
    if (val === null) return; // Annulation
    const num = parseFloat(val);

    if (isNaN(num) || num < 0 || num > 100) {
        return alert("Veuillez entrer un nombre valide entre 0 et 100.");
    }

    // .upsert va détecter l'ID unique et mettre à jour la ligne existante
    const { error } = await _supabase
        .from('eparne')
        .upsert({ 
            id: user.id, 
            eparne: num, 
            date: new Date().toISOString().split('T')[0],
            montant: 0 
        }, { onConflict: 'id' }); // On précise que le conflit se gère sur la colonne 'id'

    if (error) {
        console.error("Erreur Upsert:", error);
        alert("Erreur : " + error.message);
    } else {
        loadData(); // Rafraîchit l'affichage immédiatement
    }
};
let currentTabData = { gains: [], depenses: [] }; // Stockage temporaire

window.switchTab = (event, category) => {
    // Gérer l'apparence des boutons
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    event.currentTarget.classList.add('active');

    // Filtrer et afficher
    renderTabContent(category);
};

initDashboard();