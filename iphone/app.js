const supabaseUrl = "https://foqtdzhwxxozxdgpbwdu.supabase.co";
const supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZvcXRkemh3eHhvenhkZ3Bid2R1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkxMDc1MTQsImV4cCI6MjA4NDY4MzUxNH0.xmULuB0R7Hm8R417GdK1cLJSwwTFKk3PvHlj1DNQWTw";
const supabase = supabase.createClient(supabaseUrl, supabaseKey);

async function login() {
  const email = email.value;
  const password = password.value;

  const { error } = await supabase.auth.signInWithPassword({
    email, password
  });

  if (error) {
    msg.innerText = error.message;
  } else {
    window.location.href = "dashboard.html";
  }
}

async function register() {
  const email = document.getElementById("email").value;
  const password = document.getElementById("password").value;
  const nom = document.getElementById("nom").value;
  const prenom = document.getElementById("prenom").value;

  const { data, error } = await supabase.auth.signUp({
    email, password
  });

  if (error) {
    msg.innerText = error.message;
    return;
  }

  await supabase.from("users").insert({
    id: data.user.id,
    mail: email,
    nom,
    prenom
  });

  window.location.href = "dashboard.html";
}
