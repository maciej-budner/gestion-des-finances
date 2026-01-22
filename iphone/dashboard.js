const supabaseUrl = "https://foqtdzhwxxozxdgpbwdu.supabase.co";
const supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZvcXRkemh3eHhvenhkZ3Bid2R1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkxMDc1MTQsImV4cCI6MjA4NDY4MzUxNH0.xmULuB0R7Hm8R417GdK1cLJSwwTFKk3PvHlj1DNQWTw";
const supabase = supabase.createClient(supabaseUrl, supabaseKey);

let userId;
let epargne = 0;
let totalGain = 0;
let totalDepense = 0;

init();

async function init() {
  const { data } = await supabase.auth.getUser();
  if (!data.user) location.href = "index.html";
  userId = data.user.id;

  await cleanCourant();
  await loadEpargne();
  await loadData();
  drawDonut();
  updateStats();
}

/* 🔥 Nettoyage automatique des courants expirés */
async function cleanCourant() {
  const today = new Date().toISOString().split("T")[0];

  await supabase
    .from("depence_courant")
    .delete()
    .lt("date_fin", today);

  await supabase
    .from("gain_courant")
    .delete()
    .lt("date_fin", today);
  
}

/* 💰 Épargne */
async function loadEpargne() {
  const { data } = await supabase
    .from("eparne")
    .select("*")
    .eq("id", userId)
    .order("date", { ascending: false })
    .limit(1);

  if (data.length) {
    epargne = data[0].eparne;
    epargneTxt.innerText = `Épargne : ${epargne} %`;
  }
}

/* 📊 Chargement gains / dépenses */
async function loadData() {
  totalGain = 0;
  totalDepense = 0;
  items.innerHTML = "";

  const tables = [
    ["gain_continue","gain"],
    ["gain_courant","gain"],
    ["depence_continue","depence"],
    ["depence_courant","depence"]
  ];

  for (const [table,type] of tables) {
    const { data } = await supabase
      .from(table)
      .select("*")
      .eq("id", userId);

    data.forEach(d => {
      const montant = d[type];
      type === "gain" ? totalGain += montant : totalDepense += montant;

      items.innerHTML += `
        <div class="item">
            <span>${d.nom}</span>
            <strong>${montant} €</strong>
            <button onclick="deleteItem('${table}', ${d.id_gain_continue || d.id_depence_continue || d.id_gain_courant || d.id_depence_courant})">🗑️</button>
        </div>
        `;

    });
  }
  
}

/* 🧮 Calculs */
function updateStats() {
  const net = totalGain - totalDepense;
  const epargneMont = net * epargne / 100;
  const reste = net - epargneMont;
  animateDonut(totalGain, totalDepense);
  document.getElementById("reste").innerText = reste.toFixed(2);
  document.getElementById("epargneMontant").innerText = epargneMont.toFixed(2);
}

/* 🍩 Donut pur JS */
function drawDonut() {
  const canvas = document.getElementById("donut");
  const ctx = canvas.getContext("2d");

  const total = totalGain + totalDepense;
  const gainAngle = (totalGain / total) * Math.PI * 2;

  ctx.clearRect(0,0,220,220);

  ctx.beginPath();
  ctx.arc(110,110,90,0,gainAngle);
  ctx.lineTo(110,110);
  ctx.fillStyle = "#5cb85c";
  ctx.fill();

  ctx.beginPath();
  ctx.arc(110,110,90,gainAngle,Math.PI*2);
  ctx.lineTo(110,110);
  ctx.fillStyle = "#f0ad4e";
  ctx.fill();

  ctx.beginPath();
  ctx.arc(110,110,50,0,Math.PI*2);
  ctx.fillStyle = "#1b1b1b";
  ctx.fill();
}

let currentAngle = 0;

function animateDonut(gain, depense) {
  const canvas = document.getElementById("donut");
  const ctx = canvas.getContext("2d");

  const total = gain + depense || 1;
  const targetAngle = (gain / total) * Math.PI * 2;

  function animate() {
    ctx.clearRect(0, 0, 220, 220);

    // Gain
    ctx.beginPath();
    ctx.arc(110, 110, 90, 0, currentAngle);
    ctx.lineTo(110, 110);
    ctx.fillStyle = "#00c9a7";
    ctx.fill();

    // Dépense
    ctx.beginPath();
    ctx.arc(110, 110, 90, currentAngle, Math.PI * 2);
    ctx.lineTo(110, 110);
    ctx.fillStyle = "#f4a261";
    ctx.fill();

    // Trou
    ctx.beginPath();
    ctx.arc(110, 110, 50, 0, Math.PI * 2);
    ctx.fillStyle = "#1b1b1b";
    ctx.fill();

    if (currentAngle < targetAngle) {
      currentAngle += 0.05;
      requestAnimationFrame(animate);
    }
  }

  animate();
}
async function deleteItem(table, idItem) {
  await supabase
    .from(table)
    .delete()
    .eq("id", userId)
    .eq("id_gain_continue", idItem);

  await loadData();
  animateDonut(totalGain, totalDepense);
  updateStats();
}
function openEpargne() {
  modalEpargne.classList.remove("hidden");
}

function openAdd() {
  modalAdd.classList.remove("hidden");
}

function closeModal() {
  modalEpargne.classList.add("hidden");
  modalAdd.classList.add("hidden");
}
document.getElementById("type").addEventListener("change", e => {
  const isCourant = e.target.value.includes("courant");
  document.getElementById("dateDebut").classList.toggle("hidden", !isCourant);
});
async function saveItem() {
  const type = document.getElementById("type").value;
  const nom = document.getElementById("nom").value;
  const montant = parseFloat(document.getElementById("montant").value);
  const dateDebut = document.getElementById("dateDebut").value;

  if (!nom || !montant) return alert("Nom et montant obligatoires");

  let data = { id: userId, nom };

  if (type.includes("gain")) data.gain = montant;
  else data.depence = montant;

  if (type.includes("courant")) {
    if (!dateDebut) return alert("Date début obligatoire");
    const d = new Date(dateDebut);
    const fin = new Date(d);
    fin.setMonth(fin.getMonth() + 1);

    data.date_debut = d.toISOString().split("T")[0];
    data.date_fin = fin.toISOString().split("T")[0];
  } else {
    data.date = new Date().toISOString().slice(0,7);
  }

  await supabase.from(type).insert(data);

  closeModal();
  await loadData();
  animateDonut(totalGain, totalDepense);
  updateStats();
}

async function deleteItem(table, idItem) {
  const idField = Object.keys(arguments.callee.caller.arguments[0] || {})[0];

  await supabase.from(table).delete().eq("id", userId).eq(idField, idItem);

  await loadData();
  animateDonut(totalGain, totalDepense);
  updateStats();
}
