// On utilise 'supabase' (l'objet global du CDN) pour créer le client
const _supabase = supabase.createClient(
    'https://foqtdzhwxxozxdgpbwdu.supabase.co', 
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZvcXRkemh3eHhvenhkZ3Bid2R1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkxMDc1MTQsImV4cCI6MjA4NDY4MzUxNH0.xmULuB0R7Hm8R417GdK1cLJSwwTFKk3PvHlj1DNQWTw'
);

// Navigation
document.getElementById('show-register').addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('login-card').classList.add('hide');
    document.getElementById('register-card').classList.remove('hide');
});

document.getElementById('show-login').addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('register-card').classList.add('hide');
    document.getElementById('login-card').classList.remove('hide');
});

// Connexion
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    
    const { data, error } = await _supabase.auth.signInWithPassword({ email, password });
    
    if (error) {
        showError(error.message);
    } else {
        window.location.href = 'iphone/dashboard.html';
    }
});

// Inscription
document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;
    const nom = document.getElementById('register-nom').value;
    const prenom = document.getElementById('register-prenom').value;

    // 1. Création du compte Auth
    const { data, error: authError } = await _supabase.auth.signUp({
        email,
        password
    });

    if (authError) {
        showError("Erreur Auth: " + authError.message);
        return;
    }

    // 2. Vérification si l'utilisateur est bien créé
    if (data?.user) {
        // L'utilisateur est créé, on insère ses infos dans la table 'users'
        const { error: dbError } = await _supabase
            .from('users')
            .insert([{
                id: data.user.id, // Important : lié à l'ID Auth
                mail: email,
                nom: nom,
                prenom: prenom
            }]);

        if (dbError) {
            console.error("Détails erreur BDD:", dbError);
            showError("Compte créé mais profil BDD échoué. Vérifiez SQL RLS.");
        } else {
            alert("Inscription réussie !");
            window.location.href = 'iphone/dashboard.html';
        }
    }
});

function showError(msg) {
    const err = document.getElementById('error-message');
    err.textContent = msg;
    err.classList.remove('hide');
    setTimeout(() => err.classList.add('hide'), 5000);
}